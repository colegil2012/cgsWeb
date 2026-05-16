package com.ua.estore.cgsWeb.services.admin;

import com.ua.estore.cgsWeb.models.dto.admin.AdminVendorListItemDTO;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.vendor.Vendor;
import com.ua.estore.cgsWeb.repositories.shop.ProductRepository;
import com.ua.estore.cgsWeb.repositories.user.UserRepository;
import com.ua.estore.cgsWeb.repositories.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Admin-side vendor management: vendor CRUD, slug generation, and the
 * user↔vendor assignment lifecycle.
 *
 * <h3>Assignment rules (Round 1B decisions)</h3>
 * <ul>
 *   <li><b>Block reassignment.</b> A user already linked to a vendor
 *       (non-null {@code vendorId}) cannot be assigned to a different one.
 *       They must be unassigned first. This keeps the user↔vendor link
 *       unambiguous.</li>
 *   <li><b>Many users per vendor.</b> Multiple user accounts may be assigned
 *       to the same vendor. Nothing here enforces a single "owner."</li>
 *   <li><b>Unassign clears both sides.</b> Unassigning a user nulls their
 *       {@code vendorId} and removes the {@code VENDOR} role.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVendorService {

    private static final String ROLE_VENDOR = "VENDOR";
    private static final int PAGE_SIZE = 50;
    private static final int USER_SEARCH_LIMIT = 10;

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /* ============================================================================
     * Read
     * ============================================================================ */

    public PagedVendorResult list(int page) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by("name").ascending());
        Page<Vendor> vendorPage = vendorRepository.findAll(pageable);

        List<AdminVendorListItemDTO> items = vendorPage.getContent().stream()
                .map(v -> AdminVendorListItemDTO.from(
                        v, productRepository.countByVendorId(v.getId())))
                .toList();

        return new PagedVendorResult(
                items, vendorPage.getNumber(), vendorPage.getTotalPages(),
                vendorPage.getTotalElements());
    }

    public Optional<Vendor> findById(String vendorId) {
        if (vendorId == null || vendorId.isBlank()) return Optional.empty();
        return vendorRepository.findById(vendorId);
    }

    /** Users currently assigned to this vendor — shown on the detail page. */
    public List<User> usersForVendor(String vendorId) {
        if (vendorId == null || vendorId.isBlank()) return List.of();
        return userRepository.findByVendorId(vendorId);
    }

    public long productCount(String vendorId) {
        if (vendorId == null) return 0;
        return productRepository.countByVendorId(vendorId);
    }

    /* ============================================================================
     * Create / update
     * ============================================================================ */

    public Vendor createVendor(String name, String description, String slugOverride,
                               boolean active, String logoUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Vendor name is required.");
        }
        Vendor vendor = new Vendor();
        vendor.setName(name.trim());
        vendor.setDescription(trimOrNull(description));
        vendor.setLogoUrl(trimOrNull(logoUrl));
        vendor.setActive(active);
        vendor.setSlug(resolveSlug(slugOverride, name, null));
        vendor.setCreatedAt(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor created: id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Vendor updateVendor(String vendorId, String name, String description,
                               String slugOverride, boolean active, String logoUrl) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Vendor name is required.");
        }
        vendor.setName(name.trim());
        vendor.setDescription(trimOrNull(description));
        vendor.setLogoUrl(trimOrNull(logoUrl));
        vendor.setActive(active);
        vendor.setSlug(resolveSlug(slugOverride, name, vendorId));
        if (vendor.getCreatedAt() == null) vendor.setCreatedAt(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor updated: id={}", saved.getId());
        return saved;
    }

    /* ============================================================================
     * User assignment
     * ============================================================================ */

    /**
     * Search users for the assignment modal. Case-insensitive match against
     * username / email / profile name. Input is regex-escaped before the
     * query so metacharacters in the search box are literal.
     */
    public List<UserSearchResult> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        String escaped = escapeRegex(query.trim());
        return userRepository.searchByNameOrEmail(escaped).stream()
                .limit(USER_SEARCH_LIMIT)
                .map(UserSearchResult::from)
                .toList();
    }

    /**
     * Assign a user to a vendor. Sets {@code User.vendorId} and adds the
     * VENDOR role.
     *
     * @throws IllegalArgumentException if the user or vendor is missing, or
     *         if the user is ALREADY assigned to a vendor (block rule — they
     *         must be unassigned first).
     */
    public void assignUserToVendor(String userId, String vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Block rule: a user already linked to a vendor can't be moved
        // directly — unassign first. If they're already on THIS vendor,
        // that's a no-op success rather than an error.
        if (user.getVendorId() != null && !user.getVendorId().isBlank()) {
            if (user.getVendorId().equals(vendorId)) {
                log.info("User {} already assigned to vendor {} — no-op", userId, vendorId);
                return;
            }
            throw new IllegalArgumentException(
                    "This user is already assigned to another vendor. Unassign them there first.");
        }

        user.setVendorId(vendor.getId());

        List<String> roles = user.getRoles() == null
                ? new ArrayList<>() : new ArrayList<>(user.getRoles());
        if (!roles.contains(ROLE_VENDOR)) roles.add(ROLE_VENDOR);
        user.setRoles(roles);

        userRepository.save(user);
        log.info("Assigned user {} to vendor {}", userId, vendorId);
    }

    /**
     * Unassign a user from a vendor. Clears {@code vendorId} and removes the
     * VENDOR role. No-op-safe: if the user isn't assigned to this vendor,
     * throws so the admin knows the action didn't apply.
     */
    public void unassignUserFromVendor(String userId, String vendorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getVendorId() == null || !user.getVendorId().equals(vendorId)) {
            throw new IllegalArgumentException(
                    "This user isn't assigned to this vendor.");
        }

        user.setVendorId(null);

        List<String> roles = user.getRoles() == null
                ? new ArrayList<>() : new ArrayList<>(user.getRoles());
        roles.remove(ROLE_VENDOR);
        // Don't leave the user role-less — keep a USER baseline.
        if (roles.isEmpty()) roles.add("USER");
        user.setRoles(roles);

        userRepository.save(user);
        log.info("Unassigned user {} from vendor {}", userId, vendorId);
    }

    /* ============================================================================
     * Slug helpers
     * ============================================================================ */

    /**
     * Resolve the slug to persist. If the admin typed an override, slugify
     * and use that; otherwise slugify the name. Either way, ensure
     * uniqueness — on collision, append -2, -3, ...
     *
     * @param excludeVendorId when updating, the vendor's own id so its
     *                        current slug doesn't count as a collision
     */
    private String resolveSlug(String slugOverride, String name, String excludeVendorId) {
        String base = (slugOverride != null && !slugOverride.isBlank())
                ? slugify(slugOverride)
                : slugify(name);
        if (base.isBlank()) base = "vendor";

        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, excludeVendorId)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, String excludeVendorId) {
        Optional<Vendor> existing = vendorRepository.findBySlug(slug);
        if (existing.isEmpty()) return false;
        // If the only match is the vendor we're editing, it's not a collision.
        return excludeVendorId == null || !existing.get().getId().equals(excludeVendorId);
    }

    static String slugify(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")   // drop non-alphanumerics
                .replaceAll("[\\s-]+", "-")         // whitespace/dashes -> single dash
                .replaceAll("^-|-$", "");           // trim leading/trailing dash
    }

    /** Escape regex metacharacters so user search input is treated literally. */
    static String escapeRegex(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\\\.\\[\\]{}()*+?^$|]", "\\\\$0");
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ============================================================================
     * Result types
     * ============================================================================ */

    public record PagedVendorResult(
            List<AdminVendorListItemDTO> items,
            int page,
            int totalPages,
            long totalElements
    ) {}

    /** Minimal user shape for the assign-user search modal JSON response. */
    public record UserSearchResult(
            String id,
            String username,
            String email,
            String displayName,
            boolean alreadyAssigned   // true if this user already has a vendorId
    ) {
        static UserSearchResult from(User u) {
            String display = null;
            if (u.getProfile() != null) {
                String f = u.getProfile().getFirstName();
                String l = u.getProfile().getLastName();
                if (f != null || l != null) {
                    display = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
                }
            }
            if (display == null || display.isBlank()) display = u.getUsername();

            return new UserSearchResult(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    display,
                    u.getVendorId() != null && !u.getVendorId().isBlank()
            );
        }
    }
}