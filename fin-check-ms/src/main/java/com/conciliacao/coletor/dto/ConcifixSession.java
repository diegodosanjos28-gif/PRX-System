package com.conciliacao.coletor.dto;

/**
 * Session cookies and CSRF token extracted after the Conciflex login flow.
 * Used as headers in all subsequent API calls.
 * NEVER log any of these values.
 */
public record ConcifixSession(
    String laravelSession,
    String xsrfToken,   // URL-decoded value of the XSRF-TOKEN cookie
    String cfClearance, // Cloudflare clearance cookie
    String csrfToken    // Laravel _token from the csrf-token meta tag
) {

    /** Returns a redacted representation — safe for logging. */
    @Override
    public String toString() {
        return "ConcifixSession[laravelSession=***, xsrfToken=***, cfClearance=***, csrfToken=***]";
    }
}
