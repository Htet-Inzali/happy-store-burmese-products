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
import java.util.List;
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
    private static final int COL_ORIGINAL_PRICE = 2;  // C: ဝယ်ဈေး (MMK) — အရင်း ၁
    private static final int COL_KILO_RATE = 3;       // D: Kilo Rate (MMK) — အရင်း ၂
    private static final int COL_SALE_PRICE = 4;      // E: 🌟 ရောင်းဈေး (VND) — Admin ကိုယ်တိုင် ထည့်
    private static final int COL_QUANTITY = 5;        // F: အရေအတွက်
    private static final int COL_ARRIVAL_DATE = 6;    // G: ရောက်ရှိရက် (optional)
    private static final int COL_EXPIRY_DATE = 7;     // H: သက်တမ်းကုန်ရက် (optional)
    // I (col 8): ပုံ — embed ပုံကို anchor/rich-data ဖြင့် တွဲသည် (cell value မဟုတ်)

    // 🌟 အဆင့် ၁ — Excel ကို ဖတ်၍ preview row များ ပြန်ပေးသည် (DB မသိမ်းသေး၊ ပုံတော့ Cloudinary တင်ပြီး)
    public List<com.htet.happystore.dto.ProductDTO.BulkRow> parseForPreview(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        List<com.htet.happystore.dto.ProductDTO.BulkRow> rows = new java.util.ArrayList<>();

        byte[] fileBytes = file.getBytes();

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Floating picture (Insert → Picture) နှင့် "Place in cell" (rich-data) ပုံ နှစ်မျိုးလုံးကို ဖတ်သည်
            Map<Integer, byte[]> rowImages = extractEmbeddedImages(sheet);
            rowImages.putAll(extractRichDataImages(fileBytes));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getStringCell(row.getCell(COL_NAME));
                if (name.isBlank()) continue;

                BigDecimal weight = getNumericCell(row.getCell(COL_WEIGHT));
                BigDecimal originalPrice = getNumericCell(row.getCell(COL_ORIGINAL_PRICE));
                BigDecimal kiloRate = getNumericCell(row.getCell(COL_KILO_RATE));
                BigDecimal salePrice = getNumericCell(row.getCell(COL_SALE_PRICE));
                int qty = getNumericCell(row.getCell(COL_QUANTITY)).intValue();

                LocalDate arrivalDate = getDateCell(row.getCell(COL_ARRIVAL_DATE));
                if (arrivalDate == null) arrivalDate = LocalDate.now();
                LocalDate expiryDate = getDateCell(row.getCell(COL_EXPIRY_DATE));

                com.htet.happystore.dto.ProductDTO.BulkRow dto = new com.htet.happystore.dto.ProductDTO.BulkRow();
                dto.setName(name);
                dto.setWeightGram(weight.doubleValue());
                dto.setOriginalPriceMMK(originalPrice);
                dto.setKiloRateMMK(kiloRate);
                dto.setInitialQuantity(qty);
                dto.setArrivalDate(arrivalDate);
                dto.setExpiryDate(expiryDate);

                // 🌟 ရောင်းဈေးကို Admin ကိုယ်တိုင် Excel တွင် ထည့်သည် (auto-calc မဟုတ်တော့)
                dto.setSalePriceVND(salePrice);
                dto.setCurrentPriceVND(salePrice); // frontend preview compat

                // 🌟 အရင်း (cost) breakdown — preview တွင် ရှင်းရှင်း ပြရန်
                StockBatch tmpBatch = buildTransientBatch(weight, originalPrice, kiloRate);
                BigDecimal totalCostMMK = tmpBatch.getTotalCostMMK(); // ဝယ်ဈေး + သယ်ယူခ
                dto.setTotalCostMMK(totalCostMMK);
                dto.setTotalCostVND(totalCostMMK.multiply(settingService.getExchangeRate()));

                // 🌟 embed ပုံ ရှိပါက resize (max 1200px) ပြီး base64 အဖြစ်သာ သိမ်းသည်။
                // Cloudinary တင်ခြင်းကို confirm (saveBulkProducts) မှသာ လုပ်၍ cancel လျှင် orphan မဖြစ်စေပါ။
                byte[] imageData = rowImages.get(i);
                if (imageData != null && imageData.length > 0) {
                    byte[] optimized = resizeImage(imageData, 1200);
                    dto.setImageBase64(java.util.Base64.getEncoder().encodeToString(optimized));
                }

                rows.add(dto);
            }
        }
        return rows;
    }

    // 🌟 အဆင့် ၂ — Admin အတည်ပြုပြီးနောက် preview row များကို DB သို့ သိမ်းသည်
    @Transactional
    public void saveBulkProducts(List<com.htet.happystore.dto.ProductDTO.BulkRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("သိမ်းရန် ပစ္စည်း မရှိပါ။");
        }

        for (com.htet.happystore.dto.ProductDTO.BulkRow r : rows) {
            if (r.getName() == null || r.getName().isBlank()) continue;

            double weight = r.getWeightGram() != null ? r.getWeightGram() : 0;
            BigDecimal originalPrice = r.getOriginalPriceMMK() != null ? r.getOriginalPriceMMK() : BigDecimal.ZERO;
            BigDecimal kiloRate = r.getKiloRateMMK() != null ? r.getKiloRateMMK() : BigDecimal.ZERO;
            int qty = r.getInitialQuantity() != null ? r.getInitialQuantity() : 0;
            LocalDate arrivalDate = r.getArrivalDate() != null ? r.getArrivalDate() : LocalDate.now();

            // 🌟 ရောင်းဈေးကို အရင်တွက်သည် — Admin ထည့်ထားသော ဈေး၊ မထည့်ထားလျှင်သာ auto-calc fallback
            final BigDecimal finalPriceVND = (r.getSalePriceVND() != null && r.getSalePriceVND().compareTo(BigDecimal.ZERO) > 0)
                    ? r.getSalePriceVND()
                    : settingService.calculateSalePriceVND(buildTransientBatch(BigDecimal.valueOf(weight), originalPrice, kiloRate));

            Product product = productRepository.findByNameIgnoreCase(r.getName())
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setName(r.getName());
                        p.setWeightGram(weight);
                        p.setActive(true);
                        p.setSku("SKU-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000));
                        p.setCurrentPriceVND(finalPriceVND); // 🌟 NOT NULL constraint အတွက် save မလုပ်ခင် သတ်မှတ်
                        return productRepository.save(p);
                    });

            // 🌟 soft-delete (isActive=false) ထားသော ပစ္စည်းကို ပြန်တင်လျှင် reactivate လုပ်သည်
            product.setActive(true);

            // 🌟 ပုံ — preview base64 ရှိလျှင် (confirm) Cloudinary သို့ တင်၍ URL ရယူသည်
            if (r.getImageBase64() != null && !r.getImageBase64().isBlank()) {
                try {
                    byte[] imgBytes = java.util.Base64.getDecoder().decode(r.getImageBase64());
                    product.setImageUrl(fileUploadService.saveImageBytes(imgBytes));
                } catch (Exception e) {
                    log.warn("{} ၏ ပုံ Cloudinary တင်ခြင်း မအောင်မြင်ပါ: {}", r.getName(), e.getMessage());
                }
            } else if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
                product.setImageUrl(r.getImageUrl()); // ရှိပြီးသား URL (fallback)
            }

            StockBatch batch = new StockBatch();
            batch.setProduct(product);
            batch.setOriginalPriceMMK(originalPrice);
            batch.setKiloRateMMK(kiloRate);
            batch.setInitialQuantity(qty);
            batch.setRemainingQuantity(qty);
            batch.setArrivalDate(arrivalDate);
            batch.setExpiryDate(r.getExpiryDate());
            batch.setSalePriceVND(finalPriceVND);

            product.setCurrentPriceVND(finalPriceVND);
            productRepository.save(product);
            batchRepository.save(batch);
        }
    }

    // ဈေးတွက်ရန် ယာယီ batch (DB မသိမ်း)
    private StockBatch buildTransientBatch(BigDecimal weight, BigDecimal originalPrice, BigDecimal kiloRate) {
        Product tmpProduct = new Product();
        tmpProduct.setWeightGram(weight != null ? weight.doubleValue() : 0);
        StockBatch batch = new StockBatch();
        batch.setProduct(tmpProduct);
        batch.setOriginalPriceMMK(originalPrice);
        batch.setKiloRateMMK(kiloRate);
        return batch;
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

    /**
     * Excel ၏ "Place in cell" (IMAGE / rich-data) ပုံများကို ထုတ်ယူသည်။
     * ဤပုံများသည် drawing shape မဟုတ်ဘဲ xl/richData ထဲ သိမ်းထားသဖြင့် POI drawing API ဖြင့် မရပါ။
     * cell(vm) → valueMetadata → richValueRel → media file ဟု chain ဖြင့် map ၍ row index နှင့် တွဲသည်။
     */
    private Map<Integer, byte[]> extractRichDataImages(byte[] fileBytes) {
        Map<Integer, byte[]> result = new HashMap<>();
        try {
            // 1) zip part အားလုံးကို memory ထဲ ဖတ်ယူသည်
            Map<String, byte[]> parts = new HashMap<>();
            try (java.util.zip.ZipInputStream zis =
                         new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
                java.util.zip.ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    parts.put(e.getName(), zis.readAllBytes());
                }
            }

            byte[] sheetXml = parts.get("xl/worksheets/sheet1.xml");
            byte[] metaXml = parts.get("xl/metadata.xml");
            byte[] richValueRelXml = parts.get("xl/richData/richValueRel.xml");
            byte[] relsXml = parts.get("xl/richData/_rels/richValueRel.xml.rels");
            if (sheetXml == null || metaXml == null || richValueRelXml == null || relsXml == null) {
                return result; // "Place in cell" ပုံ မရှိပါ
            }

            java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;

            // 2) rId → media path (ဥပမာ rId1 → ../media/image1.jpeg)
            Map<String, String> ridToTarget = new HashMap<>();
            java.util.regex.Matcher rm = java.util.regex.Pattern
                    .compile("Id=\"([^\"]+)\"[^>]*Target=\"([^\"]+)\"")
                    .matcher(new String(relsXml, utf8));
            while (rm.find()) ridToTarget.put(rm.group(1), rm.group(2));

            // 3) richValueRel: rel အစဉ်လိုက် rId စာရင်း (index → rId)
            List<String> relOrder = new java.util.ArrayList<>();
            java.util.regex.Matcher relM = java.util.regex.Pattern
                    .compile("r:id=\"([^\"]+)\"")
                    .matcher(new String(richValueRelXml, utf8));
            while (relM.find()) relOrder.add(relM.group(1));

            // 4) valueMetadata: vm (1-based) → richValueRel index (rc v)
            List<Integer> vmToRelIdx = new java.util.ArrayList<>();
            String meta = new String(metaXml, utf8);
            int vmStart = meta.indexOf("<valueMetadata");
            if (vmStart >= 0) {
                java.util.regex.Matcher rcM = java.util.regex.Pattern
                        .compile("<rc [^>]*v=\"([0-9]+)\"")
                        .matcher(meta.substring(vmStart));
                while (rcM.find()) vmToRelIdx.add(Integer.parseInt(rcM.group(1)));
            }

            // 5) cell (r="H2" vm="1") → row index နှင့် ပုံ bytes တွဲသည်
            java.util.regex.Matcher cm = java.util.regex.Pattern
                    .compile("<c r=\"[A-Z]+([0-9]+)\"[^>]*vm=\"([0-9]+)\"")
                    .matcher(new String(sheetXml, utf8));
            while (cm.find()) {
                int excelRow = Integer.parseInt(cm.group(1)); // 1-based (H2 → 2)
                int vm = Integer.parseInt(cm.group(2));        // 1-based

                int relIdx = (vm - 1 < vmToRelIdx.size()) ? vmToRelIdx.get(vm - 1) : (vm - 1);
                if (relIdx < 0 || relIdx >= relOrder.size()) continue;

                String rId = relOrder.get(relIdx);
                String target = ridToTarget.get(rId);
                if (target == null) continue;

                String mediaPath = "xl/" + target.replace("../", "");
                byte[] img = parts.get(mediaPath);
                if (img != null) {
                    result.put(excelRow - 1, img); // POI row index (0-based) နှင့် ကိုက်စေရန်
                }
            }
            log.info("Excel ထဲမှ 'Place in cell' ပုံ {} ပုံ တွေ့ရှိသည်", result.size());
        } catch (Exception e) {
            log.warn("Rich-data ပုံ ထုတ်ယူ၍ မရပါ: {}", e.getMessage());
        }
        return result;
    }

    /**
     * ပုံကို အများဆုံး maxDim (px) အထိ ချုံ့ပြီး JPEG bytes ပြန်ပေးသည် (base64 payload သေးစေရန်)။
     * ဖတ်၍/ချုံ့၍ မရပါက မူရင်း bytes ကို ပြန်ပေးသည်။
     */
    private byte[] resizeImage(byte[] data, int maxDim) {
        try {
            java.awt.image.BufferedImage img =
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return data; // format မဖတ်နိုင်ပါ

            int w = img.getWidth(), h = img.getHeight();
            if (w <= maxDim && h <= maxDim) return data; // သေးပြီးသား

            double scale = (double) maxDim / Math.max(w, h);
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));

            java.awt.image.BufferedImage resized =
                    new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = resized.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, java.awt.Color.WHITE, null);
            g.dispose();

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(resized, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("ပုံ ချုံ့ခြင်း မအောင်မြင်ပါ — မူရင်းအတိုင်း သုံးမည်: {}", e.getMessage());
            return data;
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
