package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.ProductDTO;
import com.htet.happystore.service.ExcelService;
import com.htet.happystore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO.Response>> createProduct(@RequestBody ProductDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success(productService.createProduct(request), "ပစ္စည်းအသစ် ထည့်သွင်းပြီးပါပြီ။"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO.Response>> updateProduct(@PathVariable Long id, @RequestBody ProductDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request), "ပစ္စည်းအချက်အလက် ပြင်ဆင်ပြီးပါပြီ။"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> removeProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "ပစ္စည်းကို အောင်မြင်စွာ ဖျက်ပြီးပါပြီ။"));
    }
}