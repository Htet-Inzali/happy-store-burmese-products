package com.htet.happystore.service;

import com.htet.happystore.dto.ReportDTO;
import com.htet.happystore.entity.Order;
import com.htet.happystore.entity.OrderItem;
import com.htet.happystore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesReportService {

    private final OrderRepository orderRepository;
    private final SettingService settingService;

    @Transactional(readOnly = true)
    public List<ReportDTO.Sales> getSalesReport(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = (from != null && to != null)
                ? orderRepository.findByOrderDateBetween(from, to)
                : orderRepository.findAll();
        return buildReport(orders);
    }

    @Transactional(readOnly = true)
    public List<ReportDTO.Sales> getAllSalesReport() {
        return buildReport(orderRepository.findAll());
    }

    private List<ReportDTO.Sales> buildReport(List<Order> orders) {
        BigDecimal exchangeRate = settingService.getExchangeRate();
        List<ReportDTO.Sales> reports = new ArrayList<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {

                ReportDTO.Sales dto = new ReportDTO.Sales();
                dto.setOrderDate(order.getOrderDate());
                dto.setProductName(item.getProduct().getName());
                dto.setQuantity(item.getQuantity());
                dto.setSalePriceVND(item.getPriceAtPurchaseVND());

                BigDecimal original = item.getBatch().getOriginalPriceMMK() != null
                        ? item.getBatch().getOriginalPriceMMK() : BigDecimal.ZERO;
                BigDecimal kiloCost = item.getBatch().getCalculatedKiloCost();
                BigDecimal costPerItemMMK = original.add(kiloCost);
                dto.setCostPerItemMMK(costPerItemMMK);

                BigDecimal totalSaleVND = item.getPriceAtPurchaseVND()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                dto.setTotalSaleVND(totalSaleVND);

                BigDecimal costPerItemVND = costPerItemMMK
                        .multiply(exchangeRate)
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal profitPerItemVND = item.getPriceAtPurchaseVND().subtract(costPerItemVND);
                dto.setProfitPerItemVND(profitPerItemVND);

                BigDecimal totalProfitVND = profitPerItemVND
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setTotalProfitVND(totalProfitVND);

                reports.add(dto);
            }
        }
        return reports;
    }
}