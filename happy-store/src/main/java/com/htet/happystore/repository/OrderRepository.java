package com.htet.happystore.repository;

import com.htet.happystore.entity.Order;
import com.htet.happystore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Sales report အတွက် date range filter
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    // User ကိုယ်ပိုင် order history အတွက်
    List<Order> findByUser(User user);

    // Order တွေကို ရက်စွဲအလိုက် အသစ်ဆုံးကနေ အရင်ပြရန်
    List<Order> findAllByOrderByOrderDateDesc();

    @Query("SELECT o FROM Order o WHERE o.orderDate >= :startOfDay AND o.orderDate <= :endOfDay")
    List<Order> findAllOrdersForToday(
            @Param("startOfDay") LocalDateTime start,
            @Param("endOfDay") LocalDateTime end
    );

    @Query("SELECT i.product.name, SUM(i.quantity) FROM OrderItem i " +
            "GROUP BY i.product.name " +
            "ORDER BY SUM(i.quantity) DESC")
    List<Object[]> findTopSellingProducts();

//    List<Order> findByUserOrderByOrderDateDesc(User user);
}