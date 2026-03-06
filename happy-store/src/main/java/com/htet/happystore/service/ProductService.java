package com.htet.happystore.service;

import com.htet.happystore.dto.BatchRequest;
import com.htet.happystore.dto.LowStockDTO;
import com.htet.happystore.dto.ProductRequest;
import com.htet.happystore.dto.ProductResponse;
import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBatchRepository batchRepository;
    private final SettingService settingService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsForUser() {

        return productRepository.findAllWithBatches()
                .stream()
                .map(product -> {

                    ProductResponse dto = new ProductResponse();
                    dto.setId(product.getId());
                    dto.setName(product.getName());
                    dto.setWeightGram(product.getWeightGram());
                    dto.setCategory(product.getCategory()); // category ထည့်ပြီ
                    dto.setImageUrl(product.getImageUrl());

                    // 🌟 Query ထပ်ခေါ်စရာမလိုတော့ဘဲ product.getBatches() ကနေ တိုက်ရိုက်ယူခြင်း
                    List<StockBatch> activeBatches = product.getBatches().stream()
                            .filter(b -> b.getRemainingQuantity() > 0)
                            .sorted(Comparator.comparing(StockBatch::getArrivalDate)) // FIFO
                            .toList();

                    if (!activeBatches.isEmpty()) {
                        dto.setSalePriceVND(activeBatches.getFirst().getSalePriceVND());
                        dto.setTotalStock(activeBatches.stream().mapToInt(StockBatch::getRemainingQuantity).sum());
                    } else {
                        dto.setSalePriceVND(BigDecimal.ZERO);
                        dto.setTotalStock(0);
                    }
                    return dto;
                }).toList();
    }

    public List<LowStockDTO> getLowStockAlerts(Long threshold) {
        return productRepository.findLowStockProducts(threshold).stream()
                .map(result -> new LowStockDTO((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setWeightGram(request.getWeightGram());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product ID " + id + " ကို ရှာမတွေ့ပါ"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setWeightGram(request.getWeightGram());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("ဖျက်ချင်သော Product ID " + id + " မရှိပါ");
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public void addNewBatch(BatchRequest request) {
        // 1. Product ကို အရင်ရှာမယ်
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product ID " + request.getProductId() + " ကို ရှာမတွေ့ပါ"));

        // 2. Batch အသစ်ကို တည်ဆောက်ပြီး Data များ ထည့်သွင်းခြင်း
        StockBatch batch = new StockBatch();
        batch.setProduct(product);
        batch.setOriginalPriceMMK(request.getOriginalPriceMMK());
        batch.setKiloRateMMK(request.getKiloRateMMK());
        batch.setInitialQuantity(request.getInitialQuantity());
        batch.setRemainingQuantity(request.getInitialQuantity());
        batch.setArrivalDate(request.getArrivalDate());
        batch.setExpiryDate(request.getExpiryDate());

        // 🌟 ဈေးနှုန်းသတ်မှတ်ခြင်း Logic
        BigDecimal finalSalePrice;
        if (request.getManualSalePriceVND() != null && request.getManualSalePriceVND().compareTo(BigDecimal.ZERO) > 0) {
            // (၁) Admin က ကိုယ်တိုင်ဈေးနှုန်း ထည့်ပေးထားလျှင်
            finalSalePrice = request.getManualSalePriceVND();
        } else {
            // (၂) Admin က ဈေးနှုန်း မထည့်ထားလျှင် စနစ်ကို အလိုအလျောက် တွက်ခိုင်းမည်
            finalSalePrice = settingService.calculateSalePriceVND(batch);
        }

        batch.setSalePriceVND(finalSalePrice);
        batchRepository.save(batch);
    }
}