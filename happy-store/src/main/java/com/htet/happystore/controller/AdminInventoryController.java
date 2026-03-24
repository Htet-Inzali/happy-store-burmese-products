package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.ProductDTO;
import com.htet.happystore.service.ExcelService;
import com.htet.happystore.service.FileUploadService;
import com.htet.happystore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final ExcelService excelService;
    private final ProductService productService;
    private final FileUploadService fileUploadService;

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = fileUploadService.saveImage(file);
        return ResponseEntity.ok(ApiResponse.success(imageUrl, "ပုံတင်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadExcel(@RequestParam("file") MultipartFile file) throws IOException {
        excelService.importProductsFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success(null, "ပစ္စည်းစာရင်းများ အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ။"));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO.Response>> createProduct(@RequestBody ProductDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success(productService.createProduct(request), "ပစ္စည်းအသစ် ထည့်သွင်းပြီးပါပြီ။"));
    }

    @PostMapping("/products/{id}/batches")
    public ResponseEntity<ApiResponse<ProductDTO.Response>> addStockBatch(
            @PathVariable Long id,
            @RequestBody ProductDTO.BatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.addStockBatch(id, request), "Stock အသစ်ဖြည့်တင်းပြီး ရောင်းဈေးအား Update ပြုလုပ်ပြီးပါပြီ။"));
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

    // 🌟 ဤနေရာတွင် Batch များပြင်ဆင်ရန် လိုအပ်နေသော API အသစ်ကို ထည့်သွင်းပေးထားပါသည်
    @PutMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<ProductDTO.BatchResponse>> updateBatch(
            @PathVariable Long batchId,
            @RequestBody ProductDTO.BatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateBatch(batchId, request), "Batch အချက်အလက် ပြင်ဆင်ပြီးပါပြီ။"));
    }
}