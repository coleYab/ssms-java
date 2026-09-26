package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDetail(
    String field,
    String code,
    String message,
    String location,
    Object rejectedValue
) {
    public static FieldErrorDetail body(String field, String code, String message) {
        return new FieldErrorDetail(field, code, message, "body", null);
    }

    public static FieldErrorDetail body(String field, String code, String message, Object rejectedValue) {
        return new FieldErrorDetail(field, code, message, "body", rejectedValue);
    }
}
