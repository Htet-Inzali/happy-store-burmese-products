package com.htet.happystore.service;

import com.htet.happystore.dto.OrderDTO;
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
    private final ProductRepository productRepository;
    private final StockBatchRepository batchRepository;

    // ==========================================
    // 🛒 1. Checkout (FIFO Logic)
    // ==========================================
    @Transactional
    public OrderDTO.UserResponse createOrder(User user, OrderDTO.Request request) {
        Order order = new Order();
        order.setUser(user);
        order.setDeliveryType(Order.DeliveryType.valueOf(request.getDeliveryType().toUpperCase()));
        order.setShippingAddress(request.getShippingAddress());
        order.setContactPhone(request.getContactPhone() != null ? request.getContactPhone() : user.getPhone());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        BigDecimal totalAmountVND = BigDecimal.ZERO;

        for (OrderDTO.Request.CartItem cartItem : request.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

            if (!product.isActive()) {
                throw new IllegalStateException(product.getName() + " သည် လက်ရှိတွင် ရောင်းချခြင်းမရှိပါ။");
            }

            int neededQty = cartItem.getQuantity();

            // PESSIMISTIC Lock ဖြင့် Batch များကို ဆွဲထုတ်ခြင်း
            List<StockBatch> availableBatches = batchRepository.findAvailableBatchesForUpdate(product.getId());

            for (StockBatch batch : availableBatches) {
                if (neededQty <= 0) break;

                int availableInBatch = batch.getRemainingQuantity();
                int qtyToDeduct = Math.min(availableInBatch, neededQty);

                batch.setRemainingQuantity(availableInBatch - qtyToDeduct);
                batchRepository.save(batch);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setBatch(batch);
                orderItem.setQuantity(qtyToDeduct);
                orderItem.setPriceAtPurchaseVND(product.getCurrentPriceVND());

                order.getItems().add(orderItem);

                BigDecimal itemTotal = product.getCurrentPriceVND().multiply(BigDecimal.valueOf(qtyToDeduct));
                totalAmountVND = totalAmountVND.add(itemTotal);

                neededQty -= qtyToDeduct;
            }

            if (neededQty > 0) {
                throw new IllegalStateException(product.getName() + " အတွက် Stock အလုံအလောက် မရှိပါ။");
            }
        }

        order.setTotalAmountVND(totalAmountVND);
        Order savedOrder = orderRepository.save(order);
        return mapToUserResponse(savedOrder);
    }

    // ==========================================
    // 👤 2. User Order History
    // ==========================================
    public List<OrderDTO.UserResponse> getMyOrders(User user) {
        return orderRepository.findByUser(user).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 👑 3. Admin Order Management
    // ==========================================
    public List<OrderDTO.AdminResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String newStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order ID ရှာမတွေ့ပါ"));
        try {
            order.setStatus(Order.OrderStatus.valueOf(newStatusStr.toUpperCase()));
            orderRepository.save(order);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("မှားယွင်းသော Status ဖြစ်ပါသည်။");
        }
    }

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

    // ==========================================
    // 🔄 Mappers
    // ==========================================
    private OrderDTO.UserResponse mapToUserResponse(Order order) {
        OrderDTO.UserResponse res = new OrderDTO.UserResponse();
        res.setId(order.getId());
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmountVND(order.getTotalAmountVND());
        res.setStatus(order.getStatus().name());
        res.setDeliveryType(order.getDeliveryType().name());

        List<OrderDTO.UserResponse.Item> items = order.getItems().stream().map(i -> {
            OrderDTO.UserResponse.Item itemDTO = new OrderDTO.UserResponse.Item();
            itemDTO.setProductName(i.getProduct().getName());
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setPrice(i.getPriceAtPurchaseVND());
            return itemDTO;
        }).collect(Collectors.toList());

        res.setItems(items);
        return res;
    }

    private OrderDTO.AdminResponse mapToAdminResponse(Order order) {
        OrderDTO.AdminResponse res = new OrderDTO.AdminResponse();
        res.setId(order.getId());
        res.setCustomerName(order.getUser().getFullName());
        res.setCustomerPhone(order.getContactPhone() != null ? order.getContactPhone() : order.getUser().getPhone());
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmountVND(order.getTotalAmountVND());
        res.setStatus(order.getStatus().name());

        List<OrderDTO.AdminResponse.Item> items = order.getItems().stream().map(i -> {
            OrderDTO.AdminResponse.Item itemDTO = new OrderDTO.AdminResponse.Item();
            itemDTO.setProductName(i.getProduct().getName());
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setPrice(i.getPriceAtPurchaseVND());
            itemDTO.setBatchId(i.getBatch().getId()); // 🌟 အမြတ်တွက်ရန် Batch ID

            BigDecimal original = i.getBatch().getOriginalPriceMMK() != null ? i.getBatch().getOriginalPriceMMK() : BigDecimal.ZERO;
            itemDTO.setOriginalCost(original.add(i.getBatch().getCalculatedKiloCost()));
            return itemDTO;
        }).collect(Collectors.toList());

        res.setItems(items);
        return res;
    }
}