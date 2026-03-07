package com.htet.happystore.repository;

import com.htet.happystore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNameIgnoreCase(String name);

    // ReportDTO.LowStock နှင့် တွဲဖက်အသုံးပြုရန် (ဖျက်ထားသော Product များ မပါစေရန် စစ်ထားသည်)
    @Query("SELECT p.name, COALESCE(SUM(b.remainingQuantity), 0) FROM Product p LEFT JOIN p.batches b " +
            "WHERE p.isActive = true " +
            "GROUP BY p.id, p.name " +
            "HAVING COALESCE(SUM(b.remainingQuantity), 0) <= :threshold")
    List<Object[]> findLowStockProducts(@Param("threshold") Long threshold);

    // User များအား ပြသမည့် Product List (ဖျက်ထားသော Product များ မပါစေရန် စစ်ထားသည်)
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.batches b " +
            "WHERE p.isActive = true AND (b IS NULL OR b.remainingQuantity > 0)")
    List<Product> findAllActiveWithBatches();
}