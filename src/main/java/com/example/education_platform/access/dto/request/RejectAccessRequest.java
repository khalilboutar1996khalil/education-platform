package com.example.education_platform.access.dto.request;

import jakarta.validation.constraints.Size;

/** @param note why it was refused, for the admin's own record */
public record RejectAccessRequest(@Size(max = 1000) String note) {
}
