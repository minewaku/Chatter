package com.minewaku.chatter.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;

@Service
public final class CerbosAttributeValueUtil {

    private CerbosAttributeValueUtil() {}

    /**
     * Chuyển Object (Java) → AttributeValue (Cerbos) - Java 17 compatible
     */
    public static AttributeValue toAttributeValue(Object obj) {
        if (obj == null) {
            return AttributeValue.stringValue("null");
        }

        if (obj instanceof String) {
            return AttributeValue.stringValue((String) obj);
        }
        if (obj instanceof Boolean) {
            return AttributeValue.boolValue((Boolean) obj);
        }
        if (obj instanceof Integer) {
            return AttributeValue.doubleValue(((Integer) obj).doubleValue());
        }
        if (obj instanceof Long) {
            return AttributeValue.doubleValue(((Long) obj).doubleValue());
        }
        if (obj instanceof Double) {
            return AttributeValue.doubleValue((Double) obj);
        }
        if (obj instanceof Float) {
            return AttributeValue.doubleValue(((Float) obj).doubleValue());
        }
        if (obj instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) obj;
            List<AttributeValue> values = list.stream()
                    .map(CerbosAttributeValueUtil::toAttributeValue)
                    .collect(Collectors.toUnmodifiableList());
            return AttributeValue.listValue(values);
        }
        if (obj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, AttributeValue> attrMap = map.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> toAttributeValue(e.getValue())
                    ));
            return AttributeValue.mapValue(attrMap);
        }

        // Fallback: toString()
        return AttributeValue.stringValue(obj.toString());
    }

    /**
     * Áp dụng toàn bộ map vào Resource.Builder
     */
    public Resource applyAttributes(Resource resource, Map<String, Object> attrs) {
        if (attrs == null || attrs.isEmpty())

        attrs.forEach((key, value) -> {
            AttributeValue attrVal = toAttributeValue(value);
            resource.withAttribute(key, attrVal);
        });

        return resource;
    }

    public Principal applyAttributes(Principal principal, Map<String, Object> attrs) {
        if (attrs == null || attrs.isEmpty())

        attrs.forEach((key, value) -> {
            AttributeValue attrVal = toAttributeValue(value);
            principal.withAttribute(key, attrVal);
        });

        return principal;
    }
}