package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LinksDto(String self, String next, String prev, String first, String last) {
	public static LinksDto of(String basePath, int page, int limit, int totalPages) {
		String self = String.format("%s?page=%d&limit=%d", basePath, page, limit);
		String next = page < totalPages ? String.format("%s?page=%d&limit=%d", basePath, page + 1, limit) : null;
		String prev = page > 1 ? String.format("%s?page=%d&limit=%d", basePath, page - 1, limit) : null;
		String first = String.format("%s?page=1&limit=%d", basePath, limit);
		String last = totalPages > 0 ? String.format("%s?page=%d&limit=%d", basePath, totalPages, limit) : first;
		return new LinksDto(self, next, prev, first, last);
	}
}
