package com.htet.happystore.service;

import com.htet.happystore.dto.ProductDTO;
import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    public List<ProductDTO.Response> getAllActiveProducts() {
        return productRepository.findAllActiveWithBatches().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductDTO.Response getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));
        if (!product.isActive()) throw new IllegalStateException("ဤ Product ကို ဖျက်ထားပြီးဖြစ်ပါသည်။");
        return mapToResponse(product);
    }

    @Transactional
    public ProductDTO.Response createProduct(ProductDTO.Request request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setWeightGram(request.getWeightGram());
        product.setCurrentPriceVND(request.getCurrentPriceVND());
        product.setSku(request.getSku());
        product.setActive(true);
        Product savedProduct = productRepository.save(product);

        if (request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            StockBatch batch = new StockBatch();
            batch.setProduct(savedProduct);
            batch.setOriginalPriceMMK(request.getOriginalPriceMMK());
            batch.setKiloRateMMK(request.getKiloRateMMK());
            batch.setSalePriceVND(request.getCurrentPriceVND());
            batch.setInitialQuantity(request.getInitialQuantity());
            batch.setRemainingQuantity(request.getInitialQuantity());
            batch.setArrivalDate(LocalDate.now());
            batch.setExpiryDate(request.getExpiryDate());
            stockBatchRepository.save(batch);
        }

        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductDTO.Response addStockBatch(Long productId, ProductDTO.BatchRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

        StockBatch batch = new StockBatch();
        batch.setProduct(product);
        batch.setOriginalPriceMMK(request.getOriginalPriceMMK());
        batch.setKiloRateMMK(request.getKiloRateMMK());

        BigDecimal priceToUse = request.getNewSalePriceVND() != null ? request.getNewSalePriceVND() : product.getCurrentPriceVND();
        batch.setSalePriceVND(priceToUse);

        batch.setInitialQuantity(request.getInitialQuantity());
        batch.setRemainingQuantity(request.getInitialQuantity());
        batch.setArrivalDate(request.getArrivalDate() != null ? request.getArrivalDate() : LocalDate.now());
        batch.setExpiryDate(request.getExpiryDate());

        product.getBatches().add(batch);
        stockBatchRepository.save(batch);

        // 🌟 ဈေးအသစ်ပါလာခဲ့လျှင် Product ရော၊ လက်ကျန် Batch တွေကိုပါ တစ်ပြေးညီ Sync လုပ်မည်
        if (request.getNewSalePriceVND() != null) {
            syncPriceAcrossProductAndActiveBatches(product, request.getNewSalePriceVND());
        }

        return mapToResponse(product);
    }

    @Transactional
    public ProductDTO.Response updateProduct(Long id, ProductDTO.Request request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setWeightGram(request.getWeightGram());
        if(request.getSku() != null) product.setSku(request.getSku());

        // 🌟 Product အပြင်ဈေးပြောင်းလျှင် လက်ကျန် Batch တွေကိုပါ အလိုအလျောက် ဈေးလိုက်ပြောင်းမည်
        if(request.getCurrentPriceVND() != null) {
            syncPriceAcrossProductAndActiveBatches(product, request.getCurrentPriceVND());
        } else {
            productRepository.save(product);
        }

        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ဖျက်ချင်သော Product မရှိပါ"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductDTO.BatchResponse updateBatch(Long batchId, ProductDTO.BatchRequest request) {
        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch မတွေ့ပါ"));

        if (request.getOriginalPriceMMK() != null) batch.setOriginalPriceMMK(request.getOriginalPriceMMK());
        if (request.getKiloRateMMK() != null) batch.setKiloRateMMK(request.getKiloRateMMK());
        if (request.getExpiryDate() != null) batch.setExpiryDate(request.getExpiryDate());

        if (request.getInitialQuantity() != null) {
            batch.setInitialQuantity(request.getInitialQuantity());
            batch.setRemainingQuantity(request.getInitialQuantity());
        }

        // 🌟 Batch ဈေးပြောင်းလျှင် အပြင်က Product ဈေးရော တခြား Batch တွေကိုပါ ဈေးလိုက်ပြောင်းမည်
        if (request.getNewSalePriceVND() != null) {
            batch.setSalePriceVND(request.getNewSalePriceVND());
            stockBatchRepository.save(batch); // လက်ရှိ batch ကိုအရင်သိမ်းမည်

            Product product = batch.getProduct();
            syncPriceAcrossProductAndActiveBatches(product, request.getNewSalePriceVND());
        } else {
            stockBatchRepository.save(batch);
        }

        return mapToBatchResponse(batch);
    }

    // ==============================================
    // 🌟 Helper Method (The Universal Price Synchronizer)
    // ==============================================
    private void syncPriceAcrossProductAndActiveBatches(Product product, BigDecimal newPrice) {
        product.setCurrentPriceVND(newPrice);
        if (product.getBatches() != null) {
            for (StockBatch b : product.getBatches()) {
                if (b.getRemainingQuantity() != null && b.getRemainingQuantity() > 0) {
                    b.setSalePriceVND(newPrice);
                    stockBatchRepository.save(b);
                }
            }
        }
        productRepository.save(product);
    }

    // ==============================================
    // Helper Mappers
    // ==============================================

    private ProductDTO.Response mapToResponse(Product product) {
        ProductDTO.Response res = new ProductDTO.Response();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setImageUrl(product.getImageUrl());
        res.setWeightGram(product.getWeightGram());
        res.setCurrentPriceVND(product.getCurrentPriceVND());
        res.setSku(product.getSku());

        int totalStock = (product.getBatches() != null) ?
                product.getBatches().stream().mapToInt(StockBatch::getRemainingQuantity).sum() : 0;
        res.setTotalStock(totalStock);

        if (product.getBatches() != null) {
            List<ProductDTO.BatchResponse> batchResponses = product.getBatches().stream()
                    .map(this::mapToBatchResponse)
                    .collect(Collectors.toList());
            res.setBatches(batchResponses);
        }

        return res;
    }

    private ProductDTO.BatchResponse mapToBatchResponse(StockBatch b) {
        ProductDTO.BatchResponse br = new ProductDTO.BatchResponse();
        br.setId(b.getId());
        br.setRemainingQuantity(b.getRemainingQuantity());
        br.setOriginalPriceMMK(b.getOriginalPriceMMK());
        br.setKiloRateMMK(b.getKiloRateMMK());
        br.setSalePriceVND(b.getSalePriceVND());
        br.setArrivalDate(b.getArrivalDate());
        br.setExpiryDate(b.getExpiryDate());
        return br;
    }
}