package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.ProductDTO;
import com.htet.happystore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO.Response>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllActiveProducts(), "ပစ္စည်းစာရင်းများ ရယူပြီးပါပြီ။"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO.Response>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id), "ပစ္စည်းအချက်အလက် ရယူပြီးပါပြီ။"));
    }
}