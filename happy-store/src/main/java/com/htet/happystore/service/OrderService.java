package com.htet.happystore.service;

import com.htet.happystore.dto.OrderAdminResponse;
import com.htet.happystore.dto.OrderRequest;
import com.htet.happystore.dto.OrderUserResponse;
import com.htet.happystore.dto.TopProductDTO;
import com.htet.happystore.entity.*;
import com.htet.happystore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockBatchRepository batchRepository;

    // =========================================================================
    // 🛒 User များ Order တင်ရန် (Checkout)
    // =========================================================================
    @Transactional
    public Order placeOrder(User user, OrderRequest request) {

        Order order = new Order();
        order.setUser(user);

        // Entity မှာ PENDING လို့ default ပေးထားပြီးဖြစ်၍ setStatus ထပ်ရေးရန်မလိုပါ။

        order.setDeliveryType(Order.DeliveryType.valueOf(request.getDeliveryType()));
        order.setShippingAddress(request.getShippingAddress());

        // 📞 ဖုန်းနံပါတ် အလိုအလျောက် ဖြည့်ပေးသည့်အပိုင်း
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            order.setContactPhone(request.getContactPhone());
        } else {
            order.setContactPhone(user.getPhone());
        }

        order.setItems(new ArrayList<>());
        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        for (OrderRequest.CartItemDTO itemDTO : request.getItems()) {

            int remainingToDeduct = itemDTO.getQuantity();

            // Pessimistic write lock
            List<StockBatch> availableBatches =
                    batchRepository.findAvailableBatchesForUpdate(itemDTO.getProductId());

            for (StockBatch batch : availableBatches) {

                if (remainingToDeduct <= 0) break;

                int batchQty = batch.getRemainingQuantity();
                if (batchQty <= 0) continue;

                int takeAmount = Math.min(batchQty, remainingToDeduct);

                // Stock ထဲမှ နုတ်ခြင်း
                batch.setRemainingQuantity(batchQty - takeAmount);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(batch.getProduct());
                orderItem.setBatch(batch);
                orderItem.setQuantity(takeAmount);
                orderItem.setPriceAtPurchaseVND(batch.getSalePriceVND());

                order.getItems().add(orderItem);

                BigDecimal lineTotal = batch.getSalePriceVND()
                        .multiply(BigDecimal.valueOf(takeAmount));

                totalOrderAmount = totalOrderAmount.add(lineTotal);
                remainingToDeduct -= takeAmount;
            }

            if (remainingToDeduct > 0) {
                throw new IllegalStateException(
                        "Product ID " + itemDTO.getProductId() + " အတွက် Stock မလောက်တော့ပါ။"
                );
            }
        }

        order.setTotalAmountVND(totalOrderAmount);
        return orderRepository.save(order);
    }

    // =========================================================================
    // 👑 Admin များ Order Status ပြောင်းရန်
    // =========================================================================
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order ID ရှာမတွေ့ပါ"));

        // 🌟 ပိုမိုစိတ်ချရသော Enum ရှာဖွေနည်း
        Order.OrderStatus targetStatus = null;
        String input = (newStatusStr != null) ? newStatusStr.trim().toUpperCase() : "";

        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            if (status.name().equals(input)) {
                targetStatus = status;
                break;
            }
        }

        if (targetStatus == null) {
            throw new IllegalArgumentException("မှားယွင်းသော Status ဖြစ်ပါသည်။ (ပို့လိုက်သောစာသား: '" + newStatusStr + "')");
        }

        order.setStatus(targetStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatusWithEnum(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order ID ရှာမတွေ့ပါ"));

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    // Admin အတွက် DTO ပြောင်းပေးခြင်း
    public List<OrderAdminResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream().map(order -> {
            OrderAdminResponse dto = new OrderAdminResponse();
            dto.setId(order.getId());
            dto.setCustomerName(order.getUser().getFullName());
            dto.setCustomerPhone(order.getContactPhone());
            dto.setOrderDate(order.getOrderDate());
            dto.setTotalAmountVND(order.getTotalAmountVND());
            dto.setStatus(order.getStatus().name());

            dto.setItems(order.getItems().stream().map(item -> {
                OrderAdminResponse.ItemAdminDTO itemDto = new OrderAdminResponse.ItemAdminDTO();
                itemDto.setProductName(item.getProduct().getName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setPrice(item.getPriceAtPurchaseVND());
                itemDto.setBatchId(item.getBatch().getId()); // Batch Info ပါသည်
                itemDto.setOriginalCost(item.getBatch().getOriginalPriceMMK());
                return itemDto;
            }).toList());

            return dto;
        }).toList();
    }

    // User အတွက် DTO ပြောင်းပေးခြင်း (ဥပမာ- My Orders ကြည့်ရန်)
    public List<OrderUserResponse> getMyOrders(User user) {
        return orderRepository.findByUser(user).stream().map(order -> {
            OrderUserResponse dto = new OrderUserResponse();
            dto.setId(order.getId());
            dto.setOrderDate(order.getOrderDate());
            dto.setTotalAmountVND(order.getTotalAmountVND());
            dto.setStatus(order.getStatus().name());
            dto.setDeliveryType(order.getDeliveryType().name());

            dto.setItems(order.getItems().stream().map(item -> {
                OrderUserResponse.ItemUserDTO itemDto = new OrderUserResponse.ItemUserDTO();
                itemDto.setProductName(item.getProduct().getName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setPrice(item.getPriceAtPurchaseVND());
                return itemDto; // 🛡 Batch Info လုံးဝ မပါပါ
            }).toList());

            return dto;
        }).toList();
    }

    // OrderService.java ထဲမှာ ပေါင်းထည့်ပါ
    public Map<String, Object> getDailySalesSummary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        List<Order> todayOrders = orderRepository.findAllOrdersForToday(start, end);

        BigDecimal totalRevenue = todayOrders.stream()
                .map(Order::getTotalAmountVND)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("date", LocalDate.now());
        summary.put("totalOrders", todayOrders.size());
        summary.put("totalRevenueVND", totalRevenue);

        return summary;
    }

    public List<TopProductDTO> getTopSellingProducts() {
        return orderRepository.findTopSellingProducts().stream()
                .map(result -> new TopProductDTO((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
    }

    public OrderAdminResponse getOrderDetailsForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ID " + orderId + " ကို ရှာမတွေ့ပါ"));

        OrderAdminResponse dto = new OrderAdminResponse();
        dto.setId(order.getId());
        dto.setCustomerName(order.getUser().getFullName());
        dto.setCustomerPhone(order.getContactPhone());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmountVND(order.getTotalAmountVND());
        dto.setStatus(order.getStatus().name());

        dto.setItems(order.getItems().stream().map(item -> {
            OrderAdminResponse.ItemAdminDTO itemDto = new OrderAdminResponse.ItemAdminDTO();
            itemDto.setProductName(item.getProduct().getName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setPrice(item.getPriceAtPurchaseVND());
            itemDto.setBatchId(item.getBatch().getId()); // 🌟 Admin အတွက် Batch ID ပါမယ်
            itemDto.setOriginalCost(item.getBatch().getOriginalPriceMMK()); // 🌟 ရင်းနှီးခုတ် ပါမယ်
            return itemDto;
        }).toList());

        return dto;
    }
}