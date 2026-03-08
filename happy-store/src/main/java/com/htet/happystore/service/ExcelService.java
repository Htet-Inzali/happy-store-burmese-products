package com.htet.happystore.service;

import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final ProductRepository productRepository;
    private final StockBatchRepository batchRepository;
    private final SettingService settingService;

    @Transactional
    public void importProductsFromExcel(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getStringCell(row.getCell(0));
                if (name.isBlank()) continue;

                BigDecimal weight = getNumericCell(row.getCell(1));
                BigDecimal originalPrice = getNumericCell(row.getCell(2));
                BigDecimal kiloRate = getNumericCell(row.getCell(3));
                int qty = getNumericCell(row.getCell(5)).intValue();

                LocalDate arrivalDate = getDateCell(row.getCell(6));
                if (arrivalDate == null) arrivalDate = LocalDate.now();
                LocalDate expiryDate = getDateCell(row.getCell(7));

                Product product = productRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Product newProduct = new Product();
                            newProduct.setName(name);
                            newProduct.setWeightGram(weight.doubleValue());
                            newProduct.setActive(true);
                            newProduct.setSku("SKU-" + System.currentTimeMillis()); // 🌟 SKU Auto-generate
                            return productRepository.save(newProduct);
                        });

                StockBatch batch = new StockBatch();
                batch.setProduct(product);
                batch.setOriginalPriceMMK(originalPrice);
                batch.setKiloRateMMK(kiloRate);
                batch.setInitialQuantity(qty);
                batch.setRemainingQuantity(qty);
                batch.setArrivalDate(arrivalDate);
                batch.setExpiryDate(expiryDate);

                BigDecimal finalPriceVND = settingService.calculateSalePriceVND(batch);
                batch.setSalePriceVND(finalPriceVND);

                // 🌟 Excel အသစ်သွင်းလိုက်သည်နှင့် Product ၏ Live Price ကိုပါ တစ်ခါတည်း Update လုပ်ပေးခြင်း
                product.setCurrentPriceVND(finalPriceVND);
                productRepository.save(product);

                batchRepository.save(batch);
            }
        }
    }

    private String getStringCell(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private BigDecimal getNumericCell(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try { yield new BigDecimal(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield BigDecimal.ZERO; }
            }
            default -> BigDecimal.ZERO;
        };
    }

    private LocalDate getDateCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}