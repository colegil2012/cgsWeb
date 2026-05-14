package com.ua.estore.cgsWeb.services.token;

import com.ua.estore.cgsWeb.config.props.TokenProperties;
import com.ua.estore.cgsWeb.models.token.TokenType;
import com.ua.estore.cgsWeb.models.token.VerificationToken;
import com.ua.estore.cgsWeb.repositories.token.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * The shared engine for single-use, expiring tokens — email verification and
 * password reset both run through here.
 *
 * <h3>Token shape</h3>
 * Tokens are 32 bytes of {@link SecureRandom} entropy, base64url-encoded
 * (URL-safe, no padding) so they drop straight into an email link without
 * escaping. 256 bits of entropy means the raw token is not guessable or
 * brute-forceable; that's why the stored hash uses plain SHA-256 rather than
 * bcrypt. Bcrypt's deliberate slowness exists to protect <em>low</em>-entropy
 * secrets (passwords); it buys nothing here and would only slow validation.
 *
 * <h3>What's stored</h3>
 * Only the SHA-256 hash of the token. The raw value exists exactly twice:
 * in the email that's sent, and in the link the user clicks. It never
 * touches the database. {@link #consume} hashes the incoming token and
 * looks up by hash.
 *
 * <h3>Three rejection guards in {@link #consume}</h3>
 * <ol>
 *   <li><b>Type match</b> — a token minted as EMAIL_VERIFICATION can't be
 *       spent on a PASSWORD_RESET endpoint. The caller passes the expected
 *       type.</li>
 *   <li><b>Not expired</b> — checked in application code against
 *       {@code expiresAt}. The TTL index physically removes expired docs,
 *       but its background monitor lags by up to a minute, so we can't rely
 *       on absence-means-expired.</li>
 *   <li><b>Not already used</b> — {@code usedAt} must be null. A consumed
 *       token may still be within its TTL window; without this check it
 *       could be replayed.</li>
 * </ol>
 *
 * <h3>Issuing invalidates prior tokens</h3>
 * {@link #issue} deletes any outstanding tokens of the same type for the
 * same user before minting a new one. If a user requests two password
 * resets, only the most recent link works — the older one is gone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    /** 32 bytes = 256 bits of entropy. Comfortably beyond brute-force range. */
    private static final int TOKEN_BYTES = 32;

    private final VerificationTokenRepository tokenRepository;
    private final TokenProperties tokenProperties;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Mint a new token for a user. Returns the <b>raw</b> token — this is the
     * only moment it exists in plaintext on the server side. The caller
     * (a mailer) drops it into the email link. Only the hash is persisted.
     *
     * <p>Any prior unused tokens of the same type for this user are deleted
     * first, so older links stop working.</p>
     *
     * @param userId the user the token authorizes action for
     * @param type   verification or password reset
     * @return the raw token to embed in the email link
     */
    public String issue(String userId, TokenType type) {
        // Invalidate prior outstanding tokens of this type for this user.
        List<VerificationToken> existing = tokenRepository.findByUserIdAndType(userId, type);
        if (!existing.isEmpty()) {
            tokenRepository.deleteAll(existing);
            log.debug("Invalidated {} prior {} token(s) for user {}", existing.size(), type, userId);
        }

        String rawToken = generateRawToken();
        String hash = sha256Hex(rawToken);

        LocalDateTime now = LocalDateTime.now();
        VerificationToken token = new VerificationToken();
        token.setTokenHash(hash);
        token.setUserId(userId);
        token.setType(type);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(ttlFor(type)));
        // usedAt left null — token is fresh

        tokenRepository.save(token);
        log.info("Issued {} token for user {} (expires {})", type, userId, token.getExpiresAt());

        return rawToken;
    }

    /**
     * Validate and consume a token. On success, stamps {@code usedAt} so the
     * token can't be replayed, and returns the token record (the caller needs
     * {@code userId} to act on the right account).
     *
     * <p>Returns {@link Optional#empty()} for every failure mode — unknown
     * token, wrong type, expired, already used. The caller cannot distinguish
     * <em>why</em> validation failed, which is deliberate: a verification or
     * reset endpoint that says "this token is expired" vs "this token doesn't
     * exist" leaks information. One opaque failure for all cases.</p>
     *
     * @param rawToken     the token from the email link
     * @param expectedType the type the calling endpoint expects
     * @return the consumed token record, or empty if validation failed
     */
    public Optional<VerificationToken> consume(String rawToken, TokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String hash = sha256Hex(rawToken);
        VerificationToken token = tokenRepository.findByTokenHash(hash).orElse(null);

        if (token == null) {
            log.debug("Token consume failed: no token matches the provided hash");
            return Optional.empty();
        }

        // Guard 1: type match
        if (token.getType() != expectedType) {
            log.warn("Token consume failed: type mismatch (token is {}, endpoint expected {})",
                    token.getType(), expectedType);
            return Optional.empty();
        }

        // Guard 2: not expired (app-level check — see class Javadoc on TTL lag)
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Token consume failed: expired (expiresAt={})", token.getExpiresAt());
            return Optional.empty();
        }

        // Guard 3: not already used
        if (token.getUsedAt() != null) {
            log.warn("Token consume failed: already used (usedAt={})", token.getUsedAt());
            return Optional.empty();
        }

        // Passed all guards — stamp and persist so it can't be reused.
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        log.info("Consumed {} token for user {}", token.getType(), token.getUserId());

        return Optional.of(token);
    }

    /**
     * Peek at a token without consuming it. Useful for a "is this link still
     * valid" check before rendering a password-reset form — you don't want to
     * burn the token just by loading the page. The actual reset POST still
     * calls {@link #consume}.
     *
     * <p>Same opaque-failure contract as {@link #consume}: empty for any
     * failure mode.</p>
     */
    public Optional<VerificationToken> peek(String rawToken, TokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(rawToken);
        VerificationToken token = tokenRepository.findByTokenHash(hash).orElse(null);
        if (token == null
                || token.getType() != expectedType
                || token.getExpiresAt() == null
                || token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getUsedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    // ====================================================================
    // Internals
    // ====================================================================

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }

    /**
     * SHA-256 of the raw token, hex-encoded. Deterministic — the same raw
     * token always produces the same hash, which is what makes hash-based
     * lookup work.
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK spec — this cannot happen on a
            // conformant runtime. Fail loud rather than limp on.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private java.time.Duration ttlFor(TokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> tokenProperties.verificationTtl();
            case PASSWORD_RESET     -> tokenProperties.passwordResetTtl();
        };
    }
}