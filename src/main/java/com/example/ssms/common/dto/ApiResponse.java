package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, MetaDto meta, LinksDto links) {
	public static <T> ApiResponse<T> ok(T data, String requestId) {
		return new ApiResponse<>(true, data, MetaDto.now(requestId), null);
	}

	public static <T> ApiResponse<T> ok(T data, String requestId, PaginationDto pagination, LinksDto links) {
		return new ApiResponse<>(true, data, MetaDto.now(requestId, pagination), links);
	}
}
