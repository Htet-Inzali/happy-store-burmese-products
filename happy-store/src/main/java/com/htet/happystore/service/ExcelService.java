package com.htet.happystore.service;

import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelService.class);

    private final ProductRepository productRepository;
    private final StockBatchRepository batchRepository;
    private final SettingService settingService;
    private final FileUploadService fileUploadService;

    // ===== Excel Column အစီအစဉ် (template နှင့် တစ်ပြေးညီ) =====
    private static final int COL_NAME = 0;            // A: ပစ္စည်းအမည်
    private static final int COL_WEIGHT = 1;          // B: အလေးချိန် (g)
    private static final int COL_ORIGINAL_PRICE = 2;  // C: ဝယ်ဈေး (MMK)
    private static final int COL_KILO_RATE = 3;       // D: Kilo Rate (MMK)
    private static final int COL_QUANTITY = 4;        // E: အရေအတွက်
    private static final int COL_ARRIVAL_DATE = 5;    // F: ရောက်ရှိရက် (optional)
    private static final int COL_EXPIRY_DATE = 6;     // G: သက်တမ်းကုန်ရက် (optional)
    // H (col 7): ပုံ — embed ပုံကို anchor row ဖြင့် တွဲသည် (cell value မဟုတ်)

    @Transactional
    public void importProductsFromExcel(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 🌟 Excel ထဲ embed ထားသော ပုံများကို row index ဖြင့် ထုတ်ယူသည်
            Map<Integer, byte[]> rowImages = extractEmbeddedImages(sheet);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getStringCell(row.getCell(COL_NAME));
                if (name.isBlank()) continue;

                BigDecimal weight = getNumericCell(row.getCell(COL_WEIGHT));
                BigDecimal originalPrice = getNumericCell(row.getCell(COL_ORIGINAL_PRICE));
                BigDecimal kiloRate = getNumericCell(row.getCell(COL_KILO_RATE));
                int qty = getNumericCell(row.getCell(COL_QUANTITY)).intValue();

                LocalDate arrivalDate = getDateCell(row.getCell(COL_ARRIVAL_DATE));
                if (arrivalDate == null) arrivalDate = LocalDate.now();
                LocalDate expiryDate = getDateCell(row.getCell(COL_EXPIRY_DATE));

                Product product = productRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Product newProduct = new Product();
                            newProduct.setName(name);
                            newProduct.setWeightGram(weight.doubleValue());
                            newProduct.setActive(true);
                            newProduct.setSku("SKU-" + System.currentTimeMillis()); // 🌟 SKU Auto-generate
                            return productRepository.save(newProduct);
                        });

                // 🌟 ဤ row တွင် ပုံ embed ထားပါက Cloudinary သို့ upload လုပ်၍ product နှင့် တွဲသည်
                byte[] imageData = rowImages.get(i);
                if (imageData != null && imageData.length > 0) {
                    try {
                        String imageUrl = fileUploadService.saveImageBytes(imageData);
                        product.setImageUrl(imageUrl);
                    } catch (Exception e) {
                        // ပုံ upload မအောင်မြင်လျှင်လည်း ပစ္စည်းသွင်းခြင်းကို ဆက်လုပ်သည်
                        log.warn("Row {} ({}) ၏ ပုံ upload မအောင်မြင်ပါ: {}", i, name, e.getMessage());
                    }
                }

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

    /**
     * Excel sheet ထဲ embed ထားသော ပုံများကို သူတို့ရှိရာ row index နှင့် တွဲ၍ ထုတ်ယူသည်။
     * (Excel တွင် "Insert → Pictures" ဖြင့် cell ပေါ်တွင် တင်ထားသော ပုံများကို POI က ဖတ်နိုင်သည်)
     */
    private Map<Integer, byte[]> extractEmbeddedImages(Sheet sheet) {
        Map<Integer, byte[]> rowImages = new HashMap<>();
        if (!(sheet instanceof XSSFSheet xssfSheet)) return rowImages;

        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) return rowImages;

        for (XSSFShape shape : drawing.getShapes()) {
            if (shape instanceof XSSFPicture picture) {
                try {
                    XSSFClientAnchor anchor = picture.getClientAnchor();
                    if (anchor == null) continue;
                    int rowIdx = anchor.getRow1();
                    byte[] data = picture.getPictureData().getData();
                    // တစ် row တွင် ပုံ ၁ ပုံသာ — ပထမဆုံး ပုံကို ယူသည်
                    rowImages.putIfAbsent(rowIdx, data);
                } catch (Exception e) {
                    log.warn("Embed ပုံတစ်ခု ထုတ်ယူ၍ မရပါ: {}", e.getMessage());
                }
            }
        }
        log.info("Excel ထဲမှ embed ပုံ {} ပုံ တွေ့ရှိသည်", rowImages.size());
        return rowImages;
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
