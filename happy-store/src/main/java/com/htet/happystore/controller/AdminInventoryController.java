package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.BatchRequest;
import com.htet.happystore.dto.LowStockDTO;
import com.htet.happystore.dto.ProductRequest;
import com.htet.happystore.entity.Product;
import com.htet.happystore.service.ExcelService;
import com.htet.happystore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final ExcelService excelService;
    private final ProductService productService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadExcel(@RequestParam("file") MultipartFile file) throws IOException {
        excelService.importProductsFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(null, "ပစ္စည်းစာရင်းများ အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ။"));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockDTO>>> getLowStock(@RequestParam(defaultValue = "10") Long threshold) {
        return ResponseEntity.ok(ApiResponse.success(productService.getLowStockAlerts(threshold), "လက်ကျန်နည်းသော ပစ္စည်းစာရင်းများ။"));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<String>> addProduct(@RequestBody ProductRequest request) {
        productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(null, "ပစ္စည်းအသစ်ကို စာရင်းသွင်းပြီးပါပြီ။"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Product>> editProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request), "ပစ္စည်းအချက်အလက် ပြင်ဆင်ပြီးပါပြီ။"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> removeProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "ပစ္စည်းကို အောင်မြင်စွာ ဖျက်ပြီးပါပြီ။"));
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<String>> addStockBatch(@RequestBody BatchRequest request) {
        productService.addNewBatch(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock အသစ် (Batch) ကို ထည့်သွင်းပြီးပါပြီ။"));
    }
}