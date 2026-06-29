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
    private final UserRepository userRepository;

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("30000");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("500000");
    private static final String WALK_IN_PHONE = "WALK-IN-POS"; // Walk-in system user ၏ phone (login မဝင်နိုင်)

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

        // 🌟 ပို့ဆောင်ခကို order total ထဲ မပေါင်းတော့ပါ — COD ၏ ပို့ခကို ပို့ချိန်တွင် သီးသန့်ကောက်ခံသည် (Add-on)

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
    // 1.5 Admin: Walk-in Sale (ဆိုင်ရှေ့ ရောင်းအား — Stock နုတ်ပြီး ရောင်းအား report ထဲ ထည့်)
    // ==========================================
    @Transactional
    public OrderDTO.AdminResponse createWalkInSale(OrderDTO.WalkInRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("ရောင်းမည့် ပစ္စည်း မရှိပါ။");
        }

        User walkInUser = getOrCreateWalkInUser();

        Order order = new Order();
        order.setUser(walkInUser);
        order.setDeliveryType(Order.DeliveryType.PICKUP);
        order.setStatus(Order.OrderStatus.DELIVERED); // ဆိုင်ရှေ့တွင် ရောင်းပြီးသား ဖြစ်၍ ပြီးစီး status
        order.setShippingAddress("");
        order.setContactPhone(
                (request.getCustomerName() != null && !request.getCustomerName().isBlank())
                        ? request.getCustomerName() : "Walk-in");
        order.setItems(new ArrayList<>());

        BigDecimal totalAmountVND = BigDecimal.ZERO;

        for (OrderDTO.WalkInRequest.Item reqItem : request.getItems()) {
            Product product = productRepository.findById(reqItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

            int neededQty = reqItem.getQuantity();

            // ဈေး — admin override မရှိရင် product ၏ လက်ရှိ ရောင်းဈေး
            BigDecimal priceVND = reqItem.getPriceVND() != null
                    ? reqItem.getPriceVND()
                    : (product.getCurrentPriceVND() != null ? product.getCurrentPriceVND() : BigDecimal.ZERO);

            List<StockBatch> availableBatches = batchRepository.findAvailableBatchesForUpdate(product.getId());
            for (StockBatch batch : availableBatches) {
                if (neededQty <= 0) break;
                int availableInBatch = batch.getRemainingQuantity();
                int qtyToDeduct = Math.min(availableInBatch, neededQty);
                if (qtyToDeduct <= 0) continue;

                batch.setRemainingQuantity(availableInBatch - qtyToDeduct);
                batchRepository.save(batch);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(qtyToDeduct);
                orderItem.setPriceAtPurchaseVND(priceVND);
                orderItem.setBatch(batch);
                order.getItems().add(orderItem);

                totalAmountVND = totalAmountVND.add(priceVND.multiply(BigDecimal.valueOf(qtyToDeduct)));
                neededQty -= qtyToDeduct;
            }

            if (neededQty > 0) {
                throw new IllegalStateException(product.getName() + " အတွက် Stock လက်ကျန် မလုံလောက်ပါ။");
            }
        }

        order.setTotalAmountVND(totalAmountVND);

        // ID ရရှိရန် အရင် Save → ထို့နောက် Order Number (POS prefix) ပြောင်း
        Order savedOrder = orderRepository.save(order);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        savedOrder.setOrderNumber(String.format("POS-%s-%04d", datePart, savedOrder.getId()));

        return mapToAdminResponse(orderRepository.save(savedOrder));
    }

    // Walk-in order များအတွက် system user တစ်ခုကို တစ်ကြိမ်တည်း ဖန်တီး၍ ပြန်သုံးသည် (login မဝင်နိုင်)
    private User getOrCreateWalkInUser() {
        return userRepository.findByPhone(WALK_IN_PHONE).orElseGet(() -> {
            User u = new User();
            u.setFullName("Walk-in Customer");
            u.setPhone(WALK_IN_PHONE);
            // Login မဝင်နိုင်စေရန် encode မလုပ်ထားသော random password (BCrypt hash မဟုတ်၍ login fail မည်)
            u.setPassword("!no-login!" + java.util.UUID.randomUUID());
            u.setCountry(User.Country.MYANMAR);
            u.setRole(Role.USER);
            u.setActive(true);
            return userRepository.save(u);
        });
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

        // 🌟 Order ၏ စုစုပေါင်းတန်ဖိုး — ပစ္စည်းဖိုးသာ (ပို့ဆောင်ခ မပါ၊ ပို့ချိန်တွင် သီးသန့်ကောက်ခံ)
        order.setTotalAmountVND(newItemsTotal);

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

    // 🌟 Admin: ငွေပေးချေမှု status ပြောင်း (UNPAID ⇄ PAID)
    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order ID ရှာမတွေ့ပါ"));
        try {
            order.setPaymentStatus(Order.PaymentStatus.valueOf(paymentStatusStr.toUpperCase()));
            orderRepository.save(order);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("မှားယွင်းသော Payment Status ဖြစ်ပါသည်။");
        }
    }

    // 🌟 Customer: မိမိ၏ PENDING order ကို ပယ်ဖျက်ခြင်း (stock ပြန်ဖြည့်ပေးသည်)
    @Transactional
    public void cancelMyOrder(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order မတွေ့ပါ"));

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("ဤ order ကို ပယ်ဖျက်ခွင့် မရှိပါ။");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.PREORDER_PENDING) {
            throw new IllegalStateException("အတည်ပြုပြီး/ပို့ဆောင်ပြီး order ကို ပယ်ဖျက်၍ မရတော့ပါ။ ဆိုင်သို့ ဆက်သွယ်ပါ။");
        }

        // PENDING (stock နှုတ်ပြီးသား) order ကို ပယ်ဖျက်လျှင် batch သို့ stock ပြန်ဖြည့်ပေးသည်
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                StockBatch batch = item.getBatch();
                if (batch != null) {
                    batch.setRemainingQuantity(batch.getRemainingQuantity() + item.getQuantity());
                    batchRepository.save(batch);
                }
            }
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
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
        res.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "UNPAID");

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