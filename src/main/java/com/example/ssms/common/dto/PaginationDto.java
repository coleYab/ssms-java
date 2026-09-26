package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaginationDto(int page, int limit, long totalItems, int totalPages, boolean hasNext, boolean hasPrev,
		Boolean totalIsEstimate) {
	public static PaginationDto of(int page, int limit, long totalItems) {
		int totalPages = limit > 0 ? (int) Math.ceil((double) totalItems / limit) : 0;
		return new PaginationDto(page, limit, totalItems, totalPages, page < totalPages, page > 1,
				totalItems > 100000 ? Boolean.TRUE : null);
	}
}
