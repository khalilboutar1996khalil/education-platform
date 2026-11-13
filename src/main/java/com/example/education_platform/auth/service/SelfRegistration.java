package com.example.education_platform.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Whether students may create their own account. Off in production: every student is accepted by
 * the admin, through a book coupon or an access request with its plan. A bean rather than a plain
 * property so tests can switch it without starting another application context.
 */
@Component
public class SelfRegistration {

    private boolean enabled;

    public SelfRegistration(@Value("${app.auth.self-registration-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
