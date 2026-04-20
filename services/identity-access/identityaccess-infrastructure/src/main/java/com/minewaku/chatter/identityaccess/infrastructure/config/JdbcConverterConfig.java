package com.minewaku.chatter.identityaccess.infrastructure.config;

import java.sql.SQLException;
import java.util.Arrays;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto.JdbcDeviceInfo.DeviceInfoToJsonConverter;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.dto.JdbcDeviceInfo.JsonToDeviceInfoConverter;

@Configuration
public class JdbcConverterConfig extends AbstractJdbcConfiguration {
   
    private final ObjectMapper objectMapper;

    public JdbcConverterConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
                new DeviceInfoToJsonConverter(objectMapper),
                new JsonToDeviceInfoConverter(objectMapper),
                new JsonNodeToPGobjectConverter(objectMapper),
                new PGobjectToJsonNodeConverter(objectMapper)
        ));
    }

    @WritingConverter
    public class JsonNodeToPGobjectConverter implements Converter<JsonNode, PGobject> {
        private final ObjectMapper mapper;
        public JsonNodeToPGobjectConverter(ObjectMapper mapper) { this.mapper = mapper; }

        @Override
        public PGobject convert(JsonNode source) {
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            try {
                pgObject.setValue(mapper.writeValueAsString(source));
            } catch (SQLException | JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
            return pgObject;
        }
    }

    @ReadingConverter
    public class PGobjectToJsonNodeConverter implements Converter<PGobject, JsonNode> {
        private final ObjectMapper mapper;
        public PGobjectToJsonNodeConverter(ObjectMapper mapper) { this.mapper = mapper; }

        @Override
        public JsonNode convert(PGobject source) {
            try {
                return mapper.readTree(source.getValue());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
