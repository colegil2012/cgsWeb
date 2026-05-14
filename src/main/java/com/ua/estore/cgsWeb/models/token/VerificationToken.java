package com.ua.estore.cgsWeb.models.token;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

/**
 * A single-use, expiring token for email verification or password reset.
 *
 * <p><b>Only the hash is stored.</b> The raw token goes in the email link;
 * this document holds its SHA-256 hash. Validation hashes the incoming token
 * and looks up by hash. If the {@code tokens} collection ever leaked, the
 * stored hashes can't be used to verify or reset anything — same reasoning
 * as not storing plaintext passwords.</p>
 *
 * <p><b>Two independent expiry guards:</b></p>
 * <ul>
 *   <li>{@link #expiresAt} drives a Mongo TTL index — expired docs are
 *       physically removed by Mongo's background monitor (runs ~once/minute,
 *       so deletion can lag the timestamp by up to a minute).</li>
 *   <li>{@code TokenService} also checks {@code expiresAt} in application
 *       code, because the TTL monitor's lag means an "expired" token can
 *       still be physically present. The index is cleanup hygiene; the app
 *       does the authoritative expiry check.</li>
 * </ul>
 *
 * <p><b>Single-use</b> is enforced by {@link #usedAt}: a token that's been
 * consumed has this stamped, and {@code TokenService.consume()} rejects any
 * token where it's non-null. This matters because a used token may still be
 * within its TTL window — the TTL index alone wouldn't prevent replay.</p>
 */
@Data
@NoArgsConstructor
@Document(collection = "tokens")
public class VerificationToken {

    @Id
    private String id;

    /**
     * SHA-256 hash of the raw token (hex-encoded). The raw token is never
     * persisted. Indexed unique — lookups are by hash, and two tokens
     * hashing to the same value would be a cryptographic impossibility,
     * so unique also doubles as a sanity guard.
     */
    @Indexed(unique = true)
    private String tokenHash;

    /** Which user this token authorizes action for. */
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    /** Verification vs. password reset. {@code consume()} matches against this. */
    private TokenType type;

    /** When this token was minted. */
    private LocalDateTime createdAt;

    /**
     * When this token stops being valid. Drives the TTL index (see class
     * Javadoc) and is also checked in application code.
     */
    private LocalDateTime expiresAt;

    /**
     * When this token was consumed. Null while the token is still usable.
     * Non-null means the token has been spent and must be rejected, even
     * if it hasn't physically expired yet.
     */
    private LocalDateTime usedAt;
}