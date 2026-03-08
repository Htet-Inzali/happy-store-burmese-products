package com.htet.happystore.service;

import com.htet.happystore.entity.GlobalSetting;
import com.htet.happystore.entity.StockBatch;
import com.htet.happystore.repository.GlobalSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final GlobalSettingRepository settingRepository;

    public void updateSetting(String key, BigDecimal value) {
        GlobalSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(GlobalSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }

    public BigDecimal getSettingValue(String key, BigDecimal defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(GlobalSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public BigDecimal getExchangeRate() {
        return getSettingValue("EXCHANGE_RATE", BigDecimal.ONE);
    }

    public BigDecimal calculateSalePriceVND(StockBatch batch) {
        BigDecimal exchangeRate  = getSettingValue("EXCHANGE_RATE",  BigDecimal.ONE);
        BigDecimal profitPercent = getSettingValue("PROFIT_PERCENT", BigDecimal.valueOf(20));

        BigDecimal original = batch.getOriginalPriceMMK() != null
                ? batch.getOriginalPriceMMK() : BigDecimal.ZERO;

        BigDecimal kiloCost = batch.getCalculatedKiloCost();
        BigDecimal totalCostMMK = original.add(kiloCost);

        BigDecimal multiplier = BigDecimal.ONE.add(
                profitPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );

        return totalCostMMK
                .multiply(multiplier)
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
    }
}