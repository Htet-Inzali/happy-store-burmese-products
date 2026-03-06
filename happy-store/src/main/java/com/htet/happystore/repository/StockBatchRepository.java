package com.htet.happystore.repository;

import com.htet.happystore.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // FIFO အတွက် — expiryDate null ဖြစ်ရင် arrivalDate နဲ့ fallback sort လုပ်မယ်
    // @Lock သည် write operation မှာသာ သုံးသင့်တယ်၊ read query မှာ OPTIMISTIC lock မသင့်တော်ဘူး
    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.product.id = :productId
              AND b.remainingQuantity > :minQty
            ORDER BY
                CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END ASC,
                b.expiryDate ASC,
                b.arrivalDate ASC
            """)
    List<StockBatch> findAvailableBatchesByProduct(
            @Param("productId") Long productId,
            @Param("minQty") Integer minQty
    );

    // Stock deduct တဲ့အချိန် pessimistic lock သုံးမယ်
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.product.id = :productId
              AND b.remainingQuantity > 0
            ORDER BY
                CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END ASC,
                b.expiryDate ASC,
                b.arrivalDate ASC
            """)
    List<StockBatch> findAvailableBatchesForUpdate(@Param("productId") Long productId);
}