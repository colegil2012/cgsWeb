package com.ua.estore.cgsWeb.services.maps;

import com.ua.estore.cgsWeb.config.props.ServiceAreaProperties;
import com.ua.estore.cgsWeb.models.dto.address.ValidatedAddress;
import com.ua.estore.cgsWeb.util.GeoDistance;
import org.springframework.stereotype.Service;

@Service
public class ServiceAreaValidationService {

    private final ServiceAreaProperties props;

    public ServiceAreaValidationService(ServiceAreaProperties props) {
        this.props = props;
    }

    public void enforceWithinRadiusOrThrow(ValidatedAddress validated) {
        if (validated == null || !validated.valid()) {
            String msg = validated != null ? validated.message() : "Address validation failed.";

            // Make the common Google “inferred components” failure actionable for humans
            if (msg != null && msg.toLowerCase().contains("inferred components")) {
                msg = "Please enter the full street address exactly (include street number, city, state, and ZIP). "
                        + "Example: \"123 Main St, Springfield, IL 62704\".";
            }

            throw new IllegalArgumentException(msg);
        }

        double miles = GeoDistance.haversineMiles(
                props.originLat(),
                props.originLng(),
                validated.lat(),
                validated.lng()
        );

        if (miles > props.radiusMiles()) {
            throw new IllegalArgumentException(
                    "Address is outside our service area (" + String.format("%.1f", miles) + " miles away). " +
                            "Please use an address within " + (int) props.radiusMiles() + " miles."
            );
        }
    }
}
