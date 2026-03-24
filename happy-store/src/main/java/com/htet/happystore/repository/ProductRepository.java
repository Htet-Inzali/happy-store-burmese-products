package com.htet.happystore.repository;

import com.htet.happystore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNameIgnoreCase(String name);

    // 🌟 ပြင်ဆင်ချက်: ORDER BY p.id ASC ထည့်သွင်းထားသဖြင့် အမြဲတမ်း ID အစဉ်လိုက်သာ ပေါ်ပါမည်
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.batches b " +
            "WHERE p.isActive = true ORDER BY p.id ASC")
    List<Product> findAllActiveWithBatches();
}