package com.example.ssms.system.entity;

import com.example.ssms.common.entity.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_settings_key", columnList = "setting_key", unique = true)
})
public class SystemSetting extends AuditableEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 1000)
    private String settingValue;

    @Column(name = "is_secret", nullable = false)
    private boolean isSecret = false;

    @Column(name = "description", length = 500)
    private String description;

    public String getKey() {
        return settingKey;
    }

    public void setKey(String key) {
        this.settingKey = key;
    }

    public String getValue() {
        return settingValue;
    }

    public void setValue(String value) {
        this.settingValue = value;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
