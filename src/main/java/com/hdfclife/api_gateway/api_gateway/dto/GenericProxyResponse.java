package com.hdfclife.api_gateway.api_gateway.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A generic DTO to capture any JSON response from a downstream service.
 * The @JsonAnySetter annotation collects all unknown properties into a map.
 */
public class GenericProxyResponse {

    private Map<String, Object> properties = new LinkedHashMap<>();

    @JsonAnySetter
    public void set(String fieldName, Object value) {
        this.properties.put(fieldName, value);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}