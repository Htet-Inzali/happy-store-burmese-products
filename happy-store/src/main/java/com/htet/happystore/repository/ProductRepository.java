package com.htet.happystore.repository;

import com.htet.happystore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNameIgnoreCase(String name);

    // ReportDTO.LowStock နှင့် တွဲဖက်အသုံးပြုရန်
    @Query("SELECT p.name, COALESCE(SUM(b.remainingQuantity), 0) FROM Product p LEFT JOIN p.batches b " +
            "WHERE p.isActive = true " +
            "GROUP BY p.id, p.name " +
            "HAVING COALESCE(SUM(b.remainingQuantity), 0) <= :threshold")
    List<Object[]> findLowStockProducts(@Param("threshold") Long threshold);

    // 🌟 ပြင်ဆင်ချက်: ORDER BY p.id ASC ထည့်သွင်းထားသဖြင့် အမြဲတမ်း ID အစဉ်လိုက်သာ ပေါ်ပါမည်
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.batches b " +
            "WHERE p.isActive = true ORDER BY p.id ASC")
    List<Product> findAllActiveWithBatches();
}