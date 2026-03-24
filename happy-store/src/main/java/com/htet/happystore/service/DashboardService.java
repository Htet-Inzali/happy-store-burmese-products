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
        summary.setTodayProfit(revenue.subtract(totalCost));

        summary.setNewOrdersCount(orderRepository.findAll().stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        summary.setPendingPreordersCount(orderRepository.findAll().stream().filter(o -> o.getStatus() == Order.OrderStatus.PREORDER_PENDING).count());

        summary.setLowStockProductsCount(productRepository.findAllActiveWithBatches().stream()
                .filter(p -> p.getBatches().stream().mapToInt(StockBatch::getRemainingQuantity).sum() <= 5).count());

        LocalDate thirtyDays = LocalDate.now().plusDays(30);
        summary.setExpiringBatchesCount(stockBatchRepository.findAll().stream()
                .filter(b -> b.getRemainingQuantity() > 0 && b.getExpiryDate() != null && !b.getExpiryDate().isAfter(thirtyDays)).count());

        return summary;
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
        // လောလောဆယ် Build အောင်ရန်နှင့် Error မတက်ရန် Empty List ပြန်ပေးထားပါသည်။
        // (နောက်ပိုင်းမှ Order များကို တွက်ချက်သည့် Logic အစစ် ထည့်နိုင်ပါသည်)
        return java.util.Collections.emptyList();
    }
}