package com.ua.estore.cgsWeb.services.admin;

import com.ua.estore.cgsWeb.models.dto.admin.AdminUserListItemDTO;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.repositories.user.UserRepository;
import com.ua.estore.cgsWeb.services.user.AccountService;
import com.ua.estore.cgsWeb.services.user.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Admin-side user management.
 *
 * <p>Two classes of guard live here, both as defense-in-depth — the UI hides
 * the unsafe actions, but the service refuses them too:</p>
 *
 * <ul>
 *   <li><b>Self-action guard.</b> An admin cannot disable themselves, remove
 *       their own admin role, or trigger a reset email to themselves through
 *       this admin UI. Self-actions go through the normal account UI.</li>
 *   <li><b>Last-admin guard.</b> If a role mutation would leave zero users
 *       with the admin role, refuse. Without this, an admin could brick the
 *       portal by stripping their own role (or, with the self-action guard,
 *       by stripping someone else's after a confused click).</li>
 * </ul>
 *
 * <p>VENDOR role coupling: adding the VENDOR role to a user requires a
 * vendorId, which we don't have a picker for in Round 1A. For now, this
 * service refuses to ADD the VENDOR role (you can remove it, you can keep it
 * on an existing vendor). Round 1B's vendor management will add the
 * vendorId-assignment flow and lift this restriction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    static final String ROLE_ADMIN = "ADMIN";
    static final String ROLE_VENDOR = "VENDOR";
    static final String ROLE_USER = "USER";

    static final Set<String> KNOWN_ROLES = Set.of(ROLE_ADMIN, ROLE_VENDOR, ROLE_USER);

    private static final int PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final CredentialService credentialService;
    private final AccountService accountService;

    /* ============================================================================
     * Read
     * ============================================================================ */

    /**
     * Paged list of users with an optional role filter.
     *
     * <p>Role filter is case-insensitive and normalizes against
     * {@link #KNOWN_ROLES}; anything else returns the unfiltered list to
     * avoid silently dropping rows when the param is malformed.</p>
     *
     * @param roleFilter        null/empty/unknown = no filter; otherwise filters to users
     *                          whose roles list contains the matching role (uppercase)
     * @param page              zero-based page index
     * @param currentUserId     the admin viewing the list — used to mark the
     *                          self-row so the UI can hide self-action buttons
     */
    public PagedListResult list(String roleFilter, int page, String currentUserId) {
        String normalized = normalizeRoleFilter(roleFilter);
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                PAGE_SIZE,
                Sort.by("username").ascending()
        );

        Page<User> userPage = (normalized == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByRolesContaining(normalized, pageable);

        List<AdminUserListItemDTO> items = userPage.getContent().stream()
                .map(u -> AdminUserListItemDTO.from(u, currentUserId))
                .toList();

        return new PagedListResult(
                items,
                userPage.getNumber(),
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                normalized
        );
    }

    /**
     * Load a single user for the edit view.
     */
    public Optional<User> findById(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        return userRepository.findById(userId);
    }

    /* ============================================================================
     * Mutations — profile + roles
     * ============================================================================ */

    /**
     * Update a user's profile fields and role assignments in one call.
     *
     * <p>Profile updates are simple field overwrites. Role updates go through
     * the guards described in the class javadoc.</p>
     *
     * @param targetUserId  the user being edited
     * @param currentUserId the admin doing the edit (for self-guard)
     * @param firstName     new first name (null clears)
     * @param middleInit    new middle init
     * @param lastName      new last name
     * @param phone         new phone
     * @param email         new email (null = unchanged; empty/blank clears)
     * @param requestedRoles the role list the admin wants this user to have
     * @return the saved user
     * @throws IllegalArgumentException on guard violations — message is
     *                                   user-facing.
     */
    public User updateUser(String targetUserId,
                           String currentUserId,
                           String firstName,
                           String middleInit,
                           String lastName,
                           String phone,
                           String email,
                           List<String> requestedRoles) {

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // ---- Roles ----
        List<String> normalizedRequested = normalizeRoles(requestedRoles);
        List<String> currentRoles = user.getRoles() == null ? List.of() : user.getRoles();

        Set<String> currentSet = new LinkedHashSet<>(currentRoles);
        Set<String> requestedSet = new LinkedHashSet<>(normalizedRequested);

        // VENDOR-role coupling: refuse to ADD vendor role without a vendorId.
        // Round 1B will lift this when there's a vendorId picker.
        if (requestedSet.contains(ROLE_VENDOR)
                && !currentSet.contains(ROLE_VENDOR)
                && (user.getVendorId() == null || user.getVendorId().isBlank())) {
            throw new IllegalArgumentException(
                    "Can't assign the Vendor role without a vendor. Use the Vendors section to add a vendor and assign this user.");
        }

        // Self-guard: an admin can't strip their own admin role.
        if (targetUserId.equals(currentUserId)
                && currentSet.contains(ROLE_ADMIN)
                && !requestedSet.contains(ROLE_ADMIN)) {
            throw new IllegalArgumentException(
                    "You can't remove your own admin role. Have another admin do this if needed.");
        }

        // Last-admin guard: if this change would leave zero admins, refuse.
        if (currentSet.contains(ROLE_ADMIN) && !requestedSet.contains(ROLE_ADMIN)) {
            long adminCount = userRepository.countByRolesContaining(ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException(
                        "At least one user must have the admin role.");
            }
        }

        user.setRoles(new ArrayList<>(requestedSet));

        // ---- Profile fields ----
        User.UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new User.UserProfile();
            user.setProfile(profile);
        }
        profile.setFirstName(trimOrNull(firstName));
        profile.setMiddleInit(trimOrNull(middleInit));
        profile.setLastName(trimOrNull(lastName));
        profile.setPhoneNumber(trimOrNull(phone));

        // ---- Email ----
        // null email param = leave unchanged. Empty/blank string clears.
        // Changing email does NOT re-trigger verification automatically —
        // that's a separate "send verification" action if you want it.
        // We do flip emailVerified to false when the address changes, so
        // the next "send reset" or login flow can re-verify if needed.
        if (email != null) {
            String newEmail = email.trim();
            String existing = user.getEmail() == null ? "" : user.getEmail();
            if (!newEmail.equalsIgnoreCase(existing)) {
                user.setEmail(newEmail.isEmpty() ? null : newEmail);
                user.setEmailVerified(false);
            }
        }

        User saved = userRepository.save(user);
        log.info("Admin {} updated user {} (roles={}, email={})",
                currentUserId, saved.getId(), saved.getRoles(), saved.getEmail());
        return saved;
    }

    /* ============================================================================
     * Mutations — enable/disable, password reset
     * ============================================================================ */

    /**
     * Soft-delete: sets {@code enabled = false}. The user record stays for
     * order/audit history. Login is blocked at {@link com.ua.estore.cgsWeb.security.CustomUserDetails#isEnabled()}.
     *
     * <p>Note: existing sessions are NOT killed. The disabled user keeps
     * their session until it expires or they log out. For your one-admin-
     * with-a-backup setup this is fine; if it ever matters, we'd add a
     * session-purge step here.</p>
     */
    public User disableUser(String targetUserId, String currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("You can't disable your own account.");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Last-admin guard applies here too — disabling the only other admin
        // can't be allowed.
        if (user.getRoles() != null && user.getRoles().contains(ROLE_ADMIN)) {
            long activeAdmins = userRepository.countByRolesContainingAndEnabledTrue(ROLE_ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException(
                        "At least one admin must remain enabled.");
            }
        }

        user.setEnabled(false);
        User saved = userRepository.save(user);
        log.info("Admin {} disabled user {}", currentUserId, saved.getId());
        return saved;
    }

    /**
     * Re-enable a previously disabled user. The inverse of {@link #disableUser}.
     */
    public User enableUser(String targetUserId, String currentUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(true);
        User saved = userRepository.save(user);
        log.info("Admin {} re-enabled user {}", currentUserId, saved.getId());
        return saved;
    }

    /**
     * Trigger a password-reset email for the target user. The admin doesn't
     * see or type the new password — they kick off the same machinery the
     * user would for a self-reset. The email lands at the user's mailbox
     * with a reset link.
     *
     * <p>Reuses {@link AccountService#sendPasswordResetEmail}, which is async
     * and enumeration-oracle-safe. From the admin's perspective this always
     * looks successful; if the email is missing or undeliverable the failure
     * shows up in the mail logs, not here.</p>
     */
    public void sendPasswordResetEmail(String targetUserId, String currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException(
                    "Use the account page to reset your own password.");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Can't send a reset link — this user has no email on file.");
        }

        accountService.sendPasswordResetEmail(user.getEmail());
        log.info("Admin {} triggered password reset email for user {}",
                currentUserId, targetUserId);
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    static String normalizeRoleFilter(String roleFilter) {
        if (roleFilter == null) return null;
        String trimmed = roleFilter.trim();
        if (trimmed.isEmpty()) return null;
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return KNOWN_ROLES.contains(upper) ? upper : null;
    }

    static List<String> normalizeRoles(List<String> roles) {
        if (roles == null) return new ArrayList<>();
        Set<String> result = new LinkedHashSet<>();
        for (String r : roles) {
            if (r == null) continue;
            String upper = r.trim().toUpperCase(Locale.ROOT);
            if (KNOWN_ROLES.contains(upper)) result.add(upper);
        }
        // Every user keeps at least USER. Without this, removing all roles
        // leaves an empty list and CustomUserDetails.getAuthorities() returns
        // empty, which Spring treats as "anonymous-equivalent." Better to
        // enforce a baseline.
        if (!result.contains(ROLE_USER) && !result.contains(ROLE_ADMIN) && !result.contains(ROLE_VENDOR)) {
            result.add(ROLE_USER);
        }
        return new ArrayList<>(result);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ============================================================================
     * Result wrapper
     * ============================================================================ */

    /** Paged list result for the user-list view. */
    public record PagedListResult(
            List<AdminUserListItemDTO> items,
            int page,
            int totalPages,
            long totalElements,
            String roleFilter           // normalized; null when unfiltered
    ) {}
}