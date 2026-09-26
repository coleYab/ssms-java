package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetaDto(String requestId, Instant timestamp, PaginationDto pagination) {
	public static MetaDto now(String requestId) {
		return new MetaDto(requestId, Instant.now(), null);
	}

	public static MetaDto now(String requestId, PaginationDto pagination) {
		return new MetaDto(requestId, Instant.now(), pagination);
	}
}
