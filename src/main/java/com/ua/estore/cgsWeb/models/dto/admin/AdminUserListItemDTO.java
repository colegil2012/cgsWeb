package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.user.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Flat projection of {@link User} for the admin user-list view.
 *
 * <p>Purpose: keep the password hash, addresses, and other sensitive or
 * verbose fields out of the rendering path entirely. The list view doesn't
 * need them, and exposing them via a template binding risks leaking via
 * future template additions or accidental {@code ${user}} stringification
 * in logs.</p>
 *
 * <p>Built by {@code AdminUserService.toListItem}.</p>
 */
@Getter
@Builder
public class AdminUserListItemDTO {

    private String id;
    private String username;
    private String email;
    private String displayName;          // "First Last" or username if no profile
    private List<String> roles;
    private boolean enabled;
    private boolean emailVerified;
    private String vendorId;             // null unless VENDOR role
    private boolean isCurrentUser;       // set by the service when building the list,
    // used by the template to hide self-action buttons

    public static AdminUserListItemDTO from(User user, String currentUserId) {
        String displayName = null;
        User.UserProfile profile = user.getProfile();
        if (profile != null) {
            String first = profile.getFirstName();
            String last = profile.getLastName();
            if (first != null || last != null) {
                displayName = (first == null ? "" : first.trim())
                        + (first != null && last != null ? " " : "")
                        + (last == null ? "" : last.trim());
                displayName = displayName.isBlank() ? null : displayName;
            }
        }
        if (displayName == null) displayName = user.getUsername();

        return AdminUserListItemDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(displayName)
                .roles(user.getRoles() == null ? List.of() : user.getRoles())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .vendorId(user.getVendorId())
                .isCurrentUser(currentUserId != null && currentUserId.equals(user.getId()))
                .build();
    }
}