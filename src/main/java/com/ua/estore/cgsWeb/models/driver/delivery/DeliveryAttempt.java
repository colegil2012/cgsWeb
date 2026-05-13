package com.ua.estore.cgsWeb.models.driver.delivery;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

/**
 * One row in a Delivery's attempt history. Created every time a driver records
 * an outcome — success, failure, or skip. The latest attempt's outcome should
 * always match the Delivery's current status.
 */
@Data
@NoArgsConstructor
public class DeliveryAttempt {

    private LocalDateTime attemptedAt;

    private DeliveryOutcome outcome;

    /** Free-form notes from the driver explaining the outcome. */
    private String notes;

    /** Future multi-driver hook. Which driver recorded this attempt. */
    @Field(targetType = FieldType.OBJECT_ID)
    private String driverId;

    /**
     * If the route assignment changed between attempts (e.g. delivery was on
     * Route A which got cancelled, then reassigned to Route B for retry), this
     * captures which Route the attempt happened on. Null when the attempt
     * wasn't part of a planned route (e.g. an ad-hoc redelivery).
     */
    @Field(targetType = FieldType.OBJECT_ID)
    private String routeId;
}