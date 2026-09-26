package com.example.ssms.common.exception;

import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.FieldErrorDetail;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiException extends RuntimeException {
	private final ErrorCode errorCode;
	private final HttpStatus httpStatus;
	private final List<FieldErrorDetail> details = new ArrayList<>();
	private final Map<String, Object> context = new HashMap<>();

	public ApiException(ErrorCode errorCode) {
		super(errorCode.getDefaultMessageTemplate());
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getHttpStatus();
	}

	public ApiException(ErrorCode errorCode, String customMessage) {
		super(customMessage);
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getHttpStatus();
	}

	public ApiException(ErrorCode errorCode, HttpStatus httpStatus, String customMessage) {
		super(customMessage);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	public ApiException withDetail(FieldErrorDetail detail) {
		this.details.add(detail);
		return this;
	}

	public ApiException withDetails(List<FieldErrorDetail> details) {
		if (details != null) {
			this.details.addAll(details);
		}
		return this;
	}

	public ApiException withContext(String key, Object value) {
		this.context.put(key, value);
		return this;
	}

	public ApiException withContextMap(Map<String, Object> context) {
		if (context != null) {
			this.context.putAll(context);
		}
		return this;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public List<FieldErrorDetail> getDetails() {
		return details;
	}

	public Map<String, Object> getContext() {
		return context;
	}
}
