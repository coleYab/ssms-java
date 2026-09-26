package com.example.ssms.common.constant;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	// REQUEST_
	REQUEST_MALFORMED_JSON(HttpStatus.BAD_REQUEST, "Request body is not valid JSON."), REQUEST_INVALID_ID(
			HttpStatus.BAD_REQUEST, "The identifier in the URL is not a valid UUID."), REQUEST_BODY_REQUIRED(
					HttpStatus.BAD_REQUEST, "Request body is required."), REQUEST_INVALID_QUERY(HttpStatus.BAD_REQUEST,
							"One or more query parameters are malformed."), REQUEST_METHOD_NOT_ALLOWED(
									HttpStatus.METHOD_NOT_ALLOWED,
									"Method is not allowed for this route."), REQUEST_NOT_ACCEPTABLE(
											HttpStatus.NOT_ACCEPTABLE,
											"The requested response format is not supported."), REQUEST_PAYLOAD_TOO_LARGE(
													HttpStatus.PAYLOAD_TOO_LARGE,
													"Request body exceeds the maximum allowed size."), REQUEST_UNSUPPORTED_MEDIA_TYPE(
															HttpStatus.UNSUPPORTED_MEDIA_TYPE,
															"Content-Type must be application/json."), REQUEST_PRECONDITION_REQUIRED(
																	HttpStatus.PRECONDITION_REQUIRED,
																	"The If-Match header is required for this operation."),

	// VALIDATION_
	VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "One or more fields are invalid."), VALIDATION_UNKNOWN_FIELD(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"Unknown field {field}."), VALIDATION_INVALID_SORT(HttpStatus.UNPROCESSABLE_ENTITY,
					"Sorting by {field} is not supported."), VALIDATION_INVALID_FILTER(HttpStatus.UNPROCESSABLE_ENTITY,
							"Filtering by {field} is not supported."), VALIDATION_BULK_LIMIT_EXCEEDED(
									HttpStatus.UNPROCESSABLE_ENTITY,
									"A bulk request may contain at most {max} items."), VALIDATION_FILE_INVALID(
											HttpStatus.UNPROCESSABLE_ENTITY,
											"The uploaded file is invalid or in an unsupported format."),

	// AUTH_
	AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Authentication is required."), AUTH_TOKEN_INVALID(
			HttpStatus.UNAUTHORIZED, "The access token is invalid."), AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,
					"The access token has expired."), AUTH_TOKEN_STALE(HttpStatus.UNAUTHORIZED,
							"Your permissions have changed. Please refresh your session."), AUTH_INVALID_CREDENTIALS(
									HttpStatus.UNAUTHORIZED,
									"Email or password is incorrect."), AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN,
											"Account is temporarily locked. Try again after {lockedUntil}."), AUTH_ACCOUNT_DISABLED(
													HttpStatus.FORBIDDEN,
													"This account has been disabled. Contact an administrator."), AUTH_REFRESH_INVALID(
															HttpStatus.UNAUTHORIZED,
															"The refresh token is invalid or expired."), AUTH_REFRESH_REUSED(
																	HttpStatus.UNAUTHORIZED,
																	"Refresh token reuse detected. All sessions have been signed out."), AUTH_SESSION_REVOKED(
																			HttpStatus.UNAUTHORIZED,
																			"This session has been signed out."), AUTH_REAUTH_REQUIRED(
																					HttpStatus.UNAUTHORIZED,
																					"Please sign in again to confirm this action."), AUTH_PASSWORD_CHANGE_REQUIRED(
																							HttpStatus.FORBIDDEN,
																							"You must change your password before continuing."), AUTH_CURRENT_PASSWORD_INCORRECT(
																									HttpStatus.UNPROCESSABLE_ENTITY,
																									"Current password is incorrect."), AUTH_PASSWORD_REUSED(
																											HttpStatus.UNPROCESSABLE_ENTITY,
																											"New password must differ from your last 5 passwords."), AUTH_RESET_TOKEN_INVALID(
																													HttpStatus.GONE,
																													"This password reset link is invalid or has expired."),

	// AUTHZ_
	AUTHZ_FORBIDDEN_ROLE(HttpStatus.FORBIDDEN,
			"Your role does not have permission to perform this action."), AUTHZ_FORBIDDEN_SCOPE(HttpStatus.FORBIDDEN,
					"You do not have access to this {resource}."), AUTHZ_SELF_MODIFICATION_FORBIDDEN(
							HttpStatus.FORBIDDEN,
							"You cannot perform this action on your own account."), AUTHZ_PRIVILEGED_TARGET(
									HttpStatus.FORBIDDEN,
									"Only a super administrator can modify administrator accounts."), AUTHZ_EDIT_WINDOW_CLOSED(
											HttpStatus.FORBIDDEN,
											"The edit window for this attendance record has closed. Contact an administrator."),

	// RESOURCE_
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "{Resource} not found."), RESOURCE_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND,
			"The requested endpoint does not exist."), RESOURCE_GONE(HttpStatus.GONE,
					"This {resource} has been permanently removed."),

	// CONFLICT_
	CONFLICT_DUPLICATE(HttpStatus.CONFLICT, "A {resource} with this {field} already exists."), CONFLICT_IN_USE(
			HttpStatus.CONFLICT,
			"This {resource} cannot be deleted because it is referenced by {count} {dependents}."), CONFLICT_INVALID_STATE(
					HttpStatus.CONFLICT,
					"This {resource} is in state {state} and cannot be {action}."), CONFLICT_VERSION_MISMATCH(
							HttpStatus.PRECONDITION_FAILED,
							"This {resource} was modified by someone else. Reload and try again."), IDEMPOTENCY_KEY_REUSED(
									HttpStatus.CONFLICT,
									"This idempotency key was already used with a different request."), CONFLICT_REQUEST_IN_PROGRESS(
											HttpStatus.CONFLICT,
											"A request with this idempotency key is still being processed."),

	// BUSINESS_
	BUSINESS_LAST_SUPER_ADMIN(HttpStatus.CONFLICT,
			"At least one active super administrator must remain."), BUSINESS_ACADEMIC_YEAR_OVERLAP(
					HttpStatus.UNPROCESSABLE_ENTITY,
					"Academic year dates overlap with {name}."), BUSINESS_ACADEMIC_YEAR_CLOSED(HttpStatus.CONFLICT,
							"The academic year is closed and cannot be modified."), BUSINESS_NO_ACTIVE_ACADEMIC_YEAR(
									HttpStatus.CONFLICT,
									"There is no active academic year."), BUSINESS_TERM_OUT_OF_RANGE(
											HttpStatus.UNPROCESSABLE_ENTITY,
											"Term dates must fall within the academic year."), BUSINESS_TERM_OVERLAP(
													HttpStatus.UNPROCESSABLE_ENTITY,
													"Term dates overlap with another term."), BUSINESS_CLASS_FULL(
															HttpStatus.CONFLICT,
															"This class has reached its capacity of {capacity}."), BUSINESS_CAPACITY_BELOW_ENROLLMENT(
																	HttpStatus.UNPROCESSABLE_ENTITY,
																	"Capacity cannot be lower than the current enrollment ({count})."), BUSINESS_STUDENT_ALREADY_ENROLLED(
																			HttpStatus.CONFLICT,
																			"Student is already enrolled in a class for this academic year."), BUSINESS_STUDENT_NOT_ENROLLED(
																					HttpStatus.CONFLICT,
																					"Student is not enrolled in this class."), BUSINESS_STUDENT_NOT_ACTIVE(
																							HttpStatus.CONFLICT,
																							"Student status is {status}; this action requires an active student."), BUSINESS_GRADE_MISMATCH(
																									HttpStatus.UNPROCESSABLE_ENTITY,
																									"The class grade level does not match the course grade level."), BUSINESS_TEACHER_NOT_ACTIVE(
																											HttpStatus.CONFLICT,
																											"Teacher status is {status}; this action requires an active teacher."), BUSINESS_TEACHER_NOT_QUALIFIED(
																													HttpStatus.UNPROCESSABLE_ENTITY,
																													"Teacher is not qualified to teach this course."), BUSINESS_TEACHER_OVERLOAD(
																															HttpStatus.UNPROCESSABLE_ENTITY,
																															"This assignment exceeds the teacher's weekly limit of {max} periods."), BUSINESS_SCHEDULE_CONFLICT_TEACHER(
																																	HttpStatus.CONFLICT,
																																	"The teacher already has a session at this time."), BUSINESS_SCHEDULE_CONFLICT_CLASS(
																																			HttpStatus.CONFLICT,
																																			"The class already has a session at this time."), BUSINESS_OFFERING_DUPLICATE(
																																					HttpStatus.CONFLICT,
																																					"This course is already offered to this class in this term."), BUSINESS_HOMEROOM_CONFLICT(
																																							HttpStatus.CONFLICT,
																																							"Teacher is already homeroom teacher of another class this year."), BUSINESS_ATTENDANCE_DATE_INVALID(
																																									HttpStatus.UNPROCESSABLE_ENTITY,
																																									"Attendance cannot be recorded for a future date or a non-school day."), BUSINESS_ATTENDANCE_NOT_SCHEDULED(
																																											HttpStatus.UNPROCESSABLE_ENTITY,
																																											"The offering has no scheduled period on this date."), BUSINESS_ATTENDANCE_SESSION_EXISTS(
																																													HttpStatus.CONFLICT,
																																													"An attendance session already exists for this offering, date and period."), BUSINESS_ATTENDANCE_SESSION_SUBMITTED(
																																															HttpStatus.CONFLICT,
																																															"This attendance session has been submitted and is locked."), BUSINESS_ATTENDANCE_INCOMPLETE(
																																																	HttpStatus.UNPROCESSABLE_ENTITY,
																																																	"Attendance must be marked for all {count} students before submitting."), BUSINESS_ATTENDANCE_STUDENT_NOT_IN_SESSION(
																																																			HttpStatus.UNPROCESSABLE_ENTITY,
																																																			"Student is not part of this attendance session."), BUSINESS_EXCUSE_WINDOW_CLOSED(
																																																					HttpStatus.UNPROCESSABLE_ENTITY,
																																																					"The deadline to submit an excuse for this absence has passed."), BUSINESS_EXCUSE_ALREADY_REVIEWED(
																																																							HttpStatus.CONFLICT,
																																																							"This excuse has already been reviewed."), BUSINESS_INVALID_STATUS_TRANSITION(
																																																									HttpStatus.CONFLICT,
																																																									"Cannot change status from {from} to {to}."), BUSINESS_GUARDIAN_PRIMARY_REQUIRED(
																																																											HttpStatus.UNPROCESSABLE_ENTITY,
																																																											"A student must have exactly one primary guardian."), BUSINESS_IMPORT_ROW_ERRORS(
																																																													HttpStatus.UNPROCESSABLE_ENTITY,
																																																													"The import file contains errors."),

	// RATE_ and SERVER_
	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
			"Too many requests. Try again in {retryAfterSeconds} seconds."), SERVER_INTERNAL_ERROR(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"An unexpected error occurred. Please try again later."), SERVER_BAD_GATEWAY(HttpStatus.BAD_GATEWAY,
							"A required upstream service returned an invalid response."), SERVER_UNAVAILABLE(
									HttpStatus.SERVICE_UNAVAILABLE,
									"The service is temporarily unavailable. Please try again later."), SERVER_TIMEOUT(
											HttpStatus.GATEWAY_TIMEOUT, "The request took too long to process.");

	private final HttpStatus httpStatus;
	private final String defaultMessageTemplate;

	ErrorCode(HttpStatus httpStatus, String defaultMessageTemplate) {
		this.httpStatus = httpStatus;
		this.defaultMessageTemplate = defaultMessageTemplate;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getDefaultMessageTemplate() {
		return defaultMessageTemplate;
	}

	public String formatMessage(Object... args) {
		return defaultMessageTemplate;
	}
}
