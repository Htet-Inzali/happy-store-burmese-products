package com.htet.happystore.repository;

import com.htet.happystore.entity.Order;
import com.htet.happystore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByUser(User user);

    List<Order> findAllByOrderByOrderDateDesc();

    @Query("SELECT o FROM Order o WHERE o.orderDate >= :startOfDay AND o.orderDate <= :endOfDay")
    List<Order> findAllOrdersForToday(
            @Param("startOfDay") LocalDateTime start,
            @Param("endOfDay") LocalDateTime end
    );

    // ReportDTO.TopProduct နှင့် တွဲဖက်အသုံးပြုရန်
    @Query("SELECT i.product.name, SUM(i.quantity) FROM OrderItem i " +
            "GROUP BY i.product.name " +
            "ORDER BY SUM(i.quantity) DESC")
    List<Object[]> findTopSellingProducts();
}