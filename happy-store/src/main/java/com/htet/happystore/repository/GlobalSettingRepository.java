package com.htet.happystore.repository;

import com.htet.happystore.entity.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, Long> {
    Optional<GlobalSetting> findBySettingKey(String settingKey);
}