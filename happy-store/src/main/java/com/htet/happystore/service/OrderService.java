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
import java.time.format.DateTimeFormatter;
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

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("30000");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("500000");

    // ==========================================
    // 1. User: Create Order (အော်ဒါတင်ခြင်း)
    // ==========================================
    @Transactional
    public List<OrderDTO.UserResponse> createOrder(User user, OrderDTO.Request request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("ခြင်းတောင်းထဲတွင် ပစ္စည်းမရှိပါ။");
        }

        List<OrderDTO.Request.CartItem> inStockItems = new ArrayList<>();
        List<OrderDTO.Request.CartItem> preorderItems = new ArrayList<>();

        for (OrderDTO.Request.CartItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

            if (!product.isActive()) {
                throw new IllegalStateException(product.getName() + " သည် လက်ရှိတွင် ရောင်းချခြင်းမရှိပါ။");
            }

            int totalStock = product.getBatches() == null ? 0 : product.getBatches().stream()
                    .mapToInt(StockBatch::getRemainingQuantity).sum();

            if (totalStock >= item.getQuantity()) {
                inStockItems.add(item);
            } else {
                preorderItems.add(item);
            }
        }

        List<Order> savedOrders = new ArrayList<>();
        if (!inStockItems.isEmpty()) {
            savedOrders.add(processOrderSplit(user, request, inStockItems, false));
        }
        if (!preorderItems.isEmpty()) {
            savedOrders.add(processOrderSplit(user, request, preorderItems, true));
        }

        return savedOrders.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    private Order processOrderSplit(User user, OrderDTO.Request request, List<OrderDTO.Request.CartItem> cartItems, boolean isPreorder) {
        Order order = new Order();
        order.setUser(user);

        if (request.getDeliveryType() != null && !request.getDeliveryType().trim().isEmpty()) {
            try {
                order.setDeliveryType(Order.DeliveryType.valueOf(request.getDeliveryType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                order.setDeliveryType(Order.DeliveryType.COD);
            }
        } else {
            order.setDeliveryType(Order.DeliveryType.COD);
        }

        order.setShippingAddress(request.getShippingAddress() != null ? request.getShippingAddress() : "");
        order.setContactPhone(request.getContactPhone() != null ? request.getContactPhone() : user.getPhone());
        order.setStatus(isPreorder ? Order.OrderStatus.PREORDER_PENDING : Order.OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        BigDecimal totalAmountVND = BigDecimal.ZERO;

        for (OrderDTO.Request.CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

            int neededQty = cartItem.getQuantity();
            BigDecimal priceVND = product.getCurrentPriceVND() != null ? product.getCurrentPriceVND() : BigDecimal.ZERO;

            if (!isPreorder) {
                List<StockBatch> availableBatches = batchRepository.findAvailableBatchesForUpdate(product.getId());
                for (StockBatch batch : availableBatches) {
                    if (neededQty <= 0) break;
                    int availableInBatch = batch.getRemainingQuantity();
                    int qtyToDeduct = Math.min(availableInBatch, neededQty);

                    batch.setRemainingQuantity(availableInBatch - qtyToDeduct);
                    batchRepository.save(batch);

                    // Batch တစ်ခုချင်းစီအတွက် OrderItem သီးသန့်ဆောက်ပေးသည်
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(qtyToDeduct);
                    orderItem.setPriceAtPurchaseVND(priceVND);
                    orderItem.setBatch(batch);

                    order.getItems().add(orderItem);
                    neededQty -= qtyToDeduct;
                }
                if (neededQty > 0) {
                    throw new IllegalStateException(product.getName() + " အတွက် Stock မလုံလောက်ပါ။");
                }
            } else {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(neededQty);
                orderItem.setPriceAtPurchaseVND(priceVND);
                order.getItems().add(orderItem);
            }

            totalAmountVND = totalAmountVND.add(priceVND.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        if (!isPreorder && order.getDeliveryType() == Order.DeliveryType.COD && totalAmountVND.compareTo(FREE_DELIVERY_THRESHOLD) < 0) {
            totalAmountVND = totalAmountVND.add(DELIVERY_FEE);
        }

        order.setTotalAmountVND(totalAmountVND);

        // 🌟 အဆင့် ၁: ID ရရှိရန် အရင် Save ပါ
        Order savedOrder = orderRepository.save(order);

        // 🌟 အဆင့် ၂: ID ရပြီဆိုလျှင် Order Number ပြောင်းပြီး ပြန် Save ပါ
        String prefix = isPreorder ? "PRE" : "ORD";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        savedOrder.setOrderNumber(String.format("%s-%s-%04d", prefix, datePart, savedOrder.getId()));

        return orderRepository.save(savedOrder);
    }

    // ==========================================
    // 2. Admin: Fulfill Preorder (Preorder ကို Stock နှုတ်ခြင်း နှင့် ဈေးနှုန်းအသစ် ချိန်ညှိခြင်း)
    // ==========================================
    @Transactional
    public void fulfillPreorder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order မတွေ့ပါ"));

        if (order.getStatus() != Order.OrderStatus.PREORDER_PENDING) {
            throw new IllegalStateException("၎င်းသည် Preorder မဟုတ်ပါ။");
        }

        List<OrderItem> additionalItems = new ArrayList<>();
        BigDecimal newItemsTotal = BigDecimal.ZERO; // 🌟 ဈေးနှုန်းအသစ်ဖြင့် ပေါင်းမည့် ကိန်းရှင်

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int neededQty = item.getQuantity();

            // 🌟 ဈေးနှုန်းအသစ်ကို ယူပါမည် (Market Price)
            BigDecimal currentPriceVND = product.getCurrentPriceVND() != null ? product.getCurrentPriceVND() : BigDecimal.ZERO;

            List<StockBatch> availableBatches = batchRepository.findAvailableBatchesForUpdate(product.getId());
            boolean isFirstBatch = true;

            for (StockBatch batch : availableBatches) {
                if (neededQty <= 0) break;
                int qtyToDeduct = Math.min(batch.getRemainingQuantity(), neededQty);

                batch.setRemainingQuantity(batch.getRemainingQuantity() - qtyToDeduct);
                batchRepository.save(batch);

                if (isFirstBatch) {
                    item.setBatch(batch);
                    item.setQuantity(qtyToDeduct);
                    item.setPriceAtPurchaseVND(currentPriceVND); // 🌟 ဈေးနှုန်းအသစ် သတ်မှတ်သည်
                    isFirstBatch = false;
                } else {
                    // Preorder ကို ဖြည့်တင်းရာတွင် Batch (၂) ခုကွဲသွားပါက အမြတ်တွက်ရန် OrderItem အသစ်ခွဲထုတ်ပေးသည်
                    OrderItem splitItem = new OrderItem();
                    splitItem.setOrder(order);
                    splitItem.setProduct(product);
                    splitItem.setQuantity(qtyToDeduct);
                    splitItem.setPriceAtPurchaseVND(currentPriceVND); // 🌟 ဈေးနှုန်းအသစ် သတ်မှတ်သည်
                    splitItem.setBatch(batch);
                    additionalItems.add(splitItem);
                }

                // 🌟 Subtotal အသစ်အတွက် ပေါင်းထည့်မည်
                newItemsTotal = newItemsTotal.add(currentPriceVND.multiply(BigDecimal.valueOf(qtyToDeduct)));
                neededQty -= qtyToDeduct;
            }
            if (neededQty > 0) {
                throw new IllegalStateException(product.getName() + " အတွက် Stock မလုံလောက်ပါ။");
            }
        }

        order.getItems().addAll(additionalItems);
        order.setStatus(Order.OrderStatus.PENDING);

        // 🌟 Order ၏ စုစုပေါင်းတန်ဖိုး (Total Amount) ကို အသစ်ပြန်လည် တွက်ချက်ခြင်း
        BigDecimal finalTotal = newItemsTotal;
        // COD ဖြစ်ပြီး Free Delivery Limit မကျော်ရင် ပို့ဆောင်ခ ပြန်ပေါင်းထည့်မည်
        if (order.getDeliveryType() == Order.DeliveryType.COD && newItemsTotal.compareTo(FREE_DELIVERY_THRESHOLD) < 0) {
            finalTotal = finalTotal.add(DELIVERY_FEE);
        }
        order.setTotalAmountVND(finalTotal);

        orderRepository.save(order);
    }

    // ==========================================
    // 3. Controller များနှင့် ချိတ်ဆက်မည့် API Methods
    // ==========================================

    // User ရဲ့ Order History ဆွဲရန် (OrderController အတွက်)
    public List<OrderDTO.UserResponse> getMyOrders(User user) {
        return orderRepository.findByUser(user).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // Admin ရဲ့ Dashboard မှာ အော်ဒါအားလုံးပြရန် (AdminOrderController အတွက်)
    public List<OrderDTO.AdminResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    // Admin က Order Status ပြောင်းရန် (AdminOrderController အတွက်)
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

    // Admin Dashboard တွင် ယနေ့ အရောင်းစာရင်းပြရန် (AdminOrderController အတွက်)
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
    // 4. Helpers: Mappers
    // ==========================================
    private OrderDTO.UserResponse mapToUserResponse(Order order) {
        OrderDTO.UserResponse res = new OrderDTO.UserResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmountVND(order.getTotalAmountVND());
        res.setStatus(order.getStatus() != null ? order.getStatus().name() : "");
        res.setDeliveryType(order.getDeliveryType() != null ? order.getDeliveryType().name() : "");

        List<OrderDTO.UserResponse.Item> items = order.getItems().stream().map(i -> {
            OrderDTO.UserResponse.Item itemDTO = new OrderDTO.UserResponse.Item();
            itemDTO.setProductName(i.getProduct() != null ? i.getProduct().getName() : "");
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
        res.setOrderNumber(order.getOrderNumber());
        res.setCustomerName(order.getUser() != null ? order.getUser().getFullName() : "");
        res.setCustomerPhone(order.getContactPhone() != null ? order.getContactPhone() : (order.getUser() != null ? order.getUser().getPhone() : ""));
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmountVND(order.getTotalAmountVND());
        res.setStatus(order.getStatus() != null ? order.getStatus().name() : "");

        List<OrderDTO.AdminResponse.Item> items = order.getItems().stream().map(i -> {
            OrderDTO.AdminResponse.Item itemDTO = new OrderDTO.AdminResponse.Item();
            itemDTO.setProductName(i.getProduct() != null ? i.getProduct().getName() : "");
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setPrice(i.getPriceAtPurchaseVND());
            itemDTO.setBatchId(i.getBatch() != null ? i.getBatch().getId() : null);

            BigDecimal original = (i.getBatch() != null && i.getBatch().getOriginalPriceMMK() != null)
                    ? i.getBatch().getOriginalPriceMMK() : BigDecimal.ZERO;
            BigDecimal kiloCost = (i.getBatch() != null && i.getBatch().getCalculatedKiloCost() != null)
                    ? i.getBatch().getCalculatedKiloCost() : BigDecimal.ZERO;

            itemDTO.setOriginalCost(original.add(kiloCost));
            return itemDTO;
        }).collect(Collectors.toList());

        res.setItems(items);
        return res;
    }
}