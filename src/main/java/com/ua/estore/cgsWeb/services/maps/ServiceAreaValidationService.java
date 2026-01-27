package com.ua.estore.cgsWeb.services.maps;

import com.ua.estore.cgsWeb.config.props.ServiceAreaProperties;
import com.ua.estore.cgsWeb.models.dto.ValidatedAddress;
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
            throw new IllegalArgumentException(validated != null ? validated.message() : "Address validation failed.");
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
