package com.ua.estore.cgsWeb.models.token;

/**
 * Discriminator for the shared {@code tokens} collection. Both token types
 * live in one collection; {@code TokenService.consume()} takes an expected
 * type and rejects mismatches, so a verification token can't be replayed
 * against the password-reset endpoint (or vice versa).
 */
public enum TokenType {

    /** Emailed at signup. Consuming it flips the user's {@code emailVerified} + {@code enabled} flags. */
    EMAIL_VERIFICATION,

    /** Emailed on a password-reset request. Consuming it authorizes a password change. */
    PASSWORD_RESET
}