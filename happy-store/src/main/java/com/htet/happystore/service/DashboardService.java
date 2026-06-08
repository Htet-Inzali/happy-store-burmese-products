package com.htet.happystore.service;

import com.htet.happystore.dto.DashboardDTO;
import com.htet.happystore.entity.Order;
import com.htet.happystore.entity.OrderItem;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.OrderRepository;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SettingService settingService;

    // 🌟 အချိန်ကာလကို တွက်ချက်ပေးမည့် Helper
    private LocalDateTime[] getDateRange(String filter) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        if ("WEEK".equalsIgnoreCase(filter)) {
            startOfDay = today.minusDays(7).atStartOfDay();
        } else if ("MONTH".equalsIgnoreCase(filter)) {
            startOfDay = today.withDayOfMonth(1).atStartOfDay();
        }
        return new LocalDateTime[]{startOfDay, endOfDay};
    }

    public DashboardDTO.Summary getDashboardSummary(String filter) {
        DashboardDTO.Summary summary = new DashboardDTO.Summary();
        LocalDateTime[] range = getDateRange(filter);

        // 🌟 Filter အပေါ်မူတည်၍ အော်ဒါများကို ဆွဲထုတ်မည်
        List<Order> orders = orderRepository.findByOrderDateBetween(range[0], range[1]);

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal currentExchangeRate = settingService.getExchangeRate(); // 🌟 Dynamic Rate ယူရန်

        for (Order order : orders) {
            if (order.getStatus() != Order.OrderStatus.CANCELLED) {
                revenue = revenue.add(order.getTotalAmountVND() != null ? order.getTotalAmountVND() : BigDecimal.ZERO);
                for (OrderItem item : order.getItems()) {
                    if (item.getBatch() != null) {
                        BigDecimal originalCost = item.getBatch().getOriginalPriceMMK() != null ? item.getBatch().getOriginalPriceMMK() : BigDecimal.ZERO;
                        BigDecimal kiloCost = item.getBatch().getCalculatedKiloCost() != null ? item.getBatch().getCalculatedKiloCost() : BigDecimal.ZERO;

                        // 🌟 Hardcode 6.6 အစား Dynamic Rate ကို ပြောင်းသုံးထားသည်
                        BigDecimal costVND = originalCost.add(kiloCost).multiply(currentExchangeRate);
                        totalCost = totalCost.add(costVND.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
        }

        summary.setTodayRevenue(revenue);
        BigDecimal profit = revenue.subtract(totalCost);
        summary.setTodayProfit(profit);

        // 🌟 ထပ်တိုး metrics — ကာလအတွင်း orders (cancelled မပါ)
        List<Order> validOrders = orders.stream()
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        long totalOrders = validOrders.size();
        summary.setTotalOrdersCount(totalOrders);

        long itemsSold = validOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .mapToLong(OrderItem::getQuantity).sum();
        summary.setTotalItemsSold(itemsSold);

        summary.setAverageOrderValueVND(totalOrders > 0
                ? revenue.divide(BigDecimal.valueOf(totalOrders), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Online vs Walk-in (POS) ခွဲခြားခြင်း
        BigDecimal walkInRev = validOrders.stream()
                .filter(o -> o.getOrderNumber() != null && o.getOrderNumber().startsWith("POS-"))
                .map(o -> o.getTotalAmountVND() != null ? o.getTotalAmountVND() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setWalkInRevenue(walkInRev);
        summary.setOnlineRevenue(revenue.subtract(walkInRev));

        summary.setProfitMarginPercent(revenue.compareTo(BigDecimal.ZERO) > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // လက်ကျန် stock ၏ ကုန်ကျစရိတ် တန်ဖိုး (inventory value)
        BigDecimal inventoryValue = stockBatchRepository.findAll().stream()
                .filter(b -> b.getRemainingQuantity() != null && b.getRemainingQuantity() > 0)
                .map(b -> {
                    BigDecimal orig = b.getOriginalPriceMMK() != null ? b.getOriginalPriceMMK() : BigDecimal.ZERO;
                    BigDecimal kilo = b.getCalculatedKiloCost() != null ? b.getCalculatedKiloCost() : BigDecimal.ZERO;
                    return orig.add(kilo).multiply(currentExchangeRate).multiply(BigDecimal.valueOf(b.getRemainingQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setInventoryValueVND(inventoryValue);

        summary.setTotalActiveProducts(productRepository.findAllActiveWithBatches().size());

        summary.setNewOrdersCount(orderRepository.findAll().stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        summary.setPendingPreordersCount(orderRepository.findAll().stream().filter(o -> o.getStatus() == Order.OrderStatus.PREORDER_PENDING).count());

        summary.setLowStockProductsCount(productRepository.findAllActiveWithBatches().stream()
                .filter(p -> p.getBatches().stream().mapToInt(StockBatch::getRemainingQuantity).sum() <= 5).count());

        LocalDate thirtyDays = LocalDate.now().plusDays(30);
        summary.setExpiringBatchesCount(stockBatchRepository.findAll().stream()
                .filter(b -> b.getRemainingQuantity() > 0 && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(thirtyDays)).count());

        return summary;
    }

    // 🌟 Sales Trend — နေ့စဉ် ဝင်ငွေ/အမြတ် (chart အတွက်)။ filter: TODAY(7d)/WEEK(7d)/MONTH(30d)
    public List<DashboardDTO.SalesTrendPoint> getSalesTrend(String filter) {
        int days = "MONTH".equalsIgnoreCase(filter) ? 30 : 7;
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = today.atTime(LocalTime.MAX);
        List<Order> orders = orderRepository.findByOrderDateBetween(rangeStart, rangeEnd).stream()
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        BigDecimal exchangeRate = settingService.getExchangeRate();

        // နေ့တိုင်းအတွက် point တစ်ခုစီ (data မရှိရင်တောင် 0 ဖြင့်) တည်ဆောက်သည်
        List<DashboardDTO.SalesTrendPoint> points = new java.util.ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            List<Order> dayOrders = orders.stream()
                    .filter(o -> o.getOrderDate() != null && o.getOrderDate().toLocalDate().equals(day))
                    .collect(Collectors.toList());

            BigDecimal dayRevenue = BigDecimal.ZERO;
            BigDecimal dayCost = BigDecimal.ZERO;
            for (Order order : dayOrders) {
                dayRevenue = dayRevenue.add(order.getTotalAmountVND() != null ? order.getTotalAmountVND() : BigDecimal.ZERO);
                for (OrderItem item : order.getItems()) {
                    if (item.getBatch() != null) {
                        BigDecimal orig = item.getBatch().getOriginalPriceMMK() != null ? item.getBatch().getOriginalPriceMMK() : BigDecimal.ZERO;
                        BigDecimal kilo = item.getBatch().getCalculatedKiloCost() != null ? item.getBatch().getCalculatedKiloCost() : BigDecimal.ZERO;
                        BigDecimal costVND = orig.add(kilo).multiply(exchangeRate);
                        dayCost = dayCost.add(costVND.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }

            DashboardDTO.SalesTrendPoint p = new DashboardDTO.SalesTrendPoint();
            p.setDate(day);
            p.setRevenue(dayRevenue);
            p.setProfit(dayRevenue.subtract(dayCost));
            p.setOrders(dayOrders.size());
            points.add(p);
        }
        return points;
    }

    public List<DashboardDTO.ExpiringBatch> getExpiringBatchesAlert() {
        LocalDate thirtyDays = LocalDate.now().plusDays(30);
        return stockBatchRepository.findAll().stream()
                .filter(b -> b.getRemainingQuantity() > 0 && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(thirtyDays))
                .map(b -> {
                    DashboardDTO.ExpiringBatch dto = new DashboardDTO.ExpiringBatch();
                    dto.setProductName(b.getProduct().getName());
                    dto.setSku(b.getProduct().getSku());
                    dto.setRemainingQuantity(b.getRemainingQuantity());
                    dto.setExpiryDate(b.getExpiryDate());
                    return dto;
                }).collect(Collectors.toList());
    }

    // 🌟 Excel ဖိုင်အစစ် ထုတ်ပေးမည့် Logic (Apache POI)
    public byte[] generateExcelReport(String filter) {
        LocalDateTime[] range = getDateRange(filter);
        List<Order> orders = orderRepository.findByOrderDateBetween(range[0], range[1]);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Order ID");
            header.createCell(1).setCellValue("Date");
            header.createCell(2).setCellValue("Customer");
            header.createCell(3).setCellValue("Total Amount (VND)");
            header.createCell(4).setCellValue("Status");

            int rowIdx = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(o.getOrderNumber());
                row.createCell(1).setCellValue(o.getOrderDate().toString());
                row.createCell(2).setCellValue(o.getUser() != null ? o.getUser().getFullName() : "Guest");
                row.createCell(3).setCellValue(o.getTotalAmountVND() != null ? o.getTotalAmountVND().doubleValue() : 0);
                row.createCell(4).setCellValue(o.getStatus().name());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel report ထုတ်ရာတွင် အမှားရှိပါသည်။");
        }
    }

    // 🌟 အရောင်းရဆုံး ပစ္စည်းများကို ဆွဲထုတ်မည့် API (Build Error ရှင်းရန်)
    public List<DashboardDTO.TopProduct> getTopProducts() {
        return orderRepository.findTopSellingProducts().stream()
                .limit(10)
                .map(row -> {
                    DashboardDTO.TopProduct tp = new DashboardDTO.TopProduct();
                    tp.setName((String) row[0]);
                    tp.setTotalSold(((Number) row[1]).longValue());
                    return tp;
                })
                .collect(Collectors.toList());
    }
}