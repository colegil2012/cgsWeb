package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.ValidatedAddress;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.repositories.UserRepository;
import com.ua.estore.cgsWeb.services.maps.GoogleAddressValidationService;
import com.ua.estore.cgsWeb.services.maps.ServiceAreaValidationService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserRepository userRepository;
    private final GoogleAddressValidationService googleAddressValidationService;
    private final ServiceAreaValidationService serviceAreaValidationService;

    private static final Set<String> ALLOWED_ADDRESS_TYPES = Set.of("SHIPPING", "BILLING", "ALTERNATE");

    /*********** Update Addresses ***************/

    public void updateUserAddresses(String userId, AddressUpdateWrapper form) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Address form payload is missing.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        List<User.Address> merged = new ArrayList<>();
        if (form.getAddresses() != null) merged.addAll(form.getAddresses());
        if (form.getNewAddresses() != null) merged.addAll(form.getNewAddresses());

        // If the user submitted nothing useful
        if (merged.isEmpty()) {
            throw new IllegalArgumentException("No addresses were submitted.");
        }

        // Remove empty rows (common when user adds a block then leaves it blank)
        merged.removeIf(this::isBlankAddress);

        if (merged.isEmpty()) {
            throw new IllegalArgumentException("No valid addresses were submitted.");
        }

        // Normalize fields (trim strings)
        for (User.Address a : merged) {
            if (isBlank(a.getAddressId())) { a.setAddressId(new ObjectId().toString()); }
            if (a.getType() != null) a.setType(a.getType().trim().toUpperCase());
            if (a.getStreet() != null) a.setStreet(a.getStreet().trim());
            if (a.getCity() != null) a.setCity(a.getCity().trim());
            if (a.getState() != null) a.setState(a.getState().trim());
            if (a.getZip() != null) a.setZip(a.getZip().trim());

            if (isBlank(a.getType())) { throw new IllegalArgumentException("Address type is required (SHIPPING, BILLING, or ALTERNATE)."); }
            if (!ALLOWED_ADDRESS_TYPES.contains(a.getType())) { throw new IllegalArgumentException(
                        "Invalid address type: " + a.getType() + ". Allowed: SHIPPING, BILLING, ALTERNATE." ); }

            if (isBlank(a.getStreet())) throw new IllegalArgumentException("Street is required for each address.");
            if (isBlank(a.getCity())) throw new IllegalArgumentException("City is required for each address.");
            if (isBlank(a.getState())) throw new IllegalArgumentException("State is required for each address.");
            if (isBlank(a.getZip())) throw new IllegalArgumentException("Zip is required for each address.");

            if (!a.getStreet().matches(".*\\d+.*")) { throw new IllegalArgumentException(
                        a.getType() + " address street must include a street number (e.g., \"123 Main St\")."); }
            if (!a.getState().matches("(?i)^[a-z]{2}$")) { throw new IllegalArgumentException(
                        a.getType() + " address state must be a 2-letter code (e.g., \"CA\")." ); }
            if (!a.getZip().matches("^\\d{5}(-\\d{4})?$")) { throw new IllegalArgumentException(
                        a.getType() + " address ZIP must be 5 digits (or ZIP+4 like 12345-6789)." ); }

            // Normalize ZIP for storage (keep DB consistent + compatible with shippers)
            // Keep the raw ZIP for Google validation (ZIP+4 can help accuracy), but store ZIP5.
            String rawZip = a.getZip();
            String zip5 = zip5OrNull(rawZip);
            if (zip5 == null) { throw new IllegalArgumentException(a.getType() + " address ZIP must start with 5 digits."); }
            a.setZip(zip5);

            AddressDTO input = new AddressDTO(a.getStreet(), a.getCity(), a.getState(), rawZip);
            try {
                ValidatedAddress validated = googleAddressValidationService.validateUsHighCertainty(input);
                serviceAreaValidationService.enforceWithinRadiusOrThrow(validated);

                // Store coordinates rounded to 3 decimals (adjust getters to match your ValidatedAddress)
                double lat = validated.lat();
                double lng = validated.lng();
                a.setLatitude(Math.round(lat * 1000.0d) / 1000.0d);
                a.setLongitude(Math.round(lng * 1000.0d) / 1000.0d);

            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(a.getType() + " address error: " + ex.getMessage(), ex);
            }
        }

        // Enforce single default (if multiple checked, keep the last checked as default)
        int lastDefaultIndex = -1;
        for (int i = 0; i < merged.size(); i++) {
            if (merged.get(i).isDefault()) lastDefaultIndex = i;
        }
        if (lastDefaultIndex >= 0) {
            for (int i = 0; i < merged.size(); i++) {
                merged.get(i).setDefault(i == lastDefaultIndex);
            }
        }

        user.setAddresses(merged);
        userRepository.save(user);
    }

    private static String zip5OrNull(String zip) {
        if (zip == null) return null;
        String z = zip.trim();
        if (z.length() >= 5 && z.substring(0, 5).matches("^\\d{5}$")) {
            return z.substring(0, 5);
        }
        return null;
    }

    private boolean isBlankAddress(User.Address a) {
        return a == null || (
                isBlank(a.getType()) &&
                        isBlank(a.getStreet()) &&
                        isBlank(a.getCity()) &&
                        isBlank(a.getState()) &&
                        isBlank(a.getZip())
        );
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
