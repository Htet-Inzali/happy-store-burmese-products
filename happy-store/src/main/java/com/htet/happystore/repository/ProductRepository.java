package com.htet.happystore.repository;

import com.htet.happystore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Excel upload တင်ရင် နာမည်တူရှိလား စစ်ဖို့
    Optional<Product> findByNameIgnoreCase(String name);

    @Query("SELECT p.name, SUM(b.remainingQuantity) FROM Product p JOIN p.batches b " +
            "GROUP BY p.id, p.name " +
            "HAVING SUM(b.remainingQuantity) <= :threshold")
    List<Object[]> findLowStockProducts(@Param("threshold") Long threshold);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.batches b WHERE b IS NULL OR b.remainingQuantity > 0")
    List<Product> findAllWithBatches();
}