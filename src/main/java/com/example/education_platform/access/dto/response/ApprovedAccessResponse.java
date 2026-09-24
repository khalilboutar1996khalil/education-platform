package com.example.education_platform.access.dto.response;

/**
 * @param temporaryPassword returned once, exactly as an invitation does, because without SMTP
 *                          configured the mail goes nowhere and the admin needs something to pass on
 */
public record ApprovedAccessResponse(AccessRequestResponse request, String temporaryPassword) {
}
