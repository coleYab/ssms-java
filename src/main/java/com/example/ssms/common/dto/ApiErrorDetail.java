package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDetail(
    String code,
    String message,
    int status,
    List<FieldErrorDetail> details,
    Map<String, Object> context,
    String requestId,
    Instant timestamp,
    String path,
    String method,
    String documentationUrl
) {
}
