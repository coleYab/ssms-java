package com.example.ssms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(boolean success, ApiErrorDetail error) {
	public static ApiErrorResponse of(ApiErrorDetail error) {
		return new ApiErrorResponse(false, error);
	}
}
