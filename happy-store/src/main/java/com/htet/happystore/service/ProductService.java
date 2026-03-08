package com.htet.happystore.service;

import com.htet.happystore.dto.ProductDTO;
import com.htet.happystore.entity.Product;
import com.htet.happystore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDTO.Response updateProduct(Long id, ProductDTO.Request request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product မတွေ့ပါ"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setWeightGram(request.getWeightGram());
        product.setCurrentPriceVND(request.getCurrentPriceVND());
        if(request.getSku() != null) product.setSku(request.getSku());

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ဖျက်ချင်သော Product မရှိပါ"));
        product.setActive(false); // Soft Delete
        productRepository.save(product);
    }

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
                product.getBatches().stream().mapToInt(b -> b.getRemainingQuantity()).sum() : 0;
        res.setTotalStock(totalStock);

        return res;
    }
}