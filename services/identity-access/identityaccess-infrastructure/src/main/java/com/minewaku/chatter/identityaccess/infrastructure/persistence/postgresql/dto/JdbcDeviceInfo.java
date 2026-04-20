package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

public record JdbcDeviceInfo(
    String ipAddress,
    String country,

    String rawUserAgent,

    String deviceType,         // Desktop | Phone | Tablet | Robot
    String deviceBrand,        // Apple | Samsung | ...
    String osName,             // Windows | macOS | Android | iOS
    String osVersion,          // 14.0 | 11 | ...
    String browserName,        // Chrome | Firefox | Safari | ...
    String browserVersion      // 120.0.0 | ...
) {

    @WritingConverter
    public static class DeviceInfoToJsonConverter implements Converter<JdbcDeviceInfo, PGobject> {
        
        private final ObjectMapper objectMapper;

        public DeviceInfoToJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public PGobject convert(JdbcDeviceInfo source) {
            try {
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(objectMapper.writeValueAsString(source));
                return pgObject;
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize DeviceInfo to JSON", e);
            }
        }
    }

    @ReadingConverter
    public static class JsonToDeviceInfoConverter implements Converter<PGobject, JdbcDeviceInfo> {
        
        private final ObjectMapper objectMapper;

        public JsonToDeviceInfoConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JdbcDeviceInfo convert(PGobject source) {
            try {
                if (source.getValue() == null) {
                    return null;
                }
                return objectMapper.readValue(source.getValue(), JdbcDeviceInfo.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize JSON to DeviceInfo", e);
            }
        }
    }
}
