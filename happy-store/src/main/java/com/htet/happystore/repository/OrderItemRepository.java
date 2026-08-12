package com.htet.happystore.repository;

import com.htet.happystore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ဤ batch မှ ရောင်း/မှာယူထားမှု (order item) အရေအတွက် — batch ဖျက်ခွင့် စစ်ရန်
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.batch.id = :batchId")
    long countByBatch(@Param("batchId") Long batchId);
}
