package com.ua.estore.cgsWeb.services.shipping;

import com.ua.estore.cgsWeb.models.shipping.RateCard;
import com.ua.estore.cgsWeb.repositories.shipping.RateCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loads the currently-active {@link RateCard} from Mongo.
 * Falls back to a safe hard-coded default if none exists (fresh DB / tests),
 * so estimates can never throw at runtime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingRateService {

    private final RateCardRepository rateCardRepository;

    public RateCard getActiveRateCard() {
        return rateCardRepository.findFirstByActiveTrueOrderByVersionDesc()
                .orElseGet(this::fallbackCard);
    }

    private RateCard fallbackCard() {
        log.warn("No active RateCard found in Mongo — using built-in fallback pricing.");
        RateCard card = new RateCard();
        card.setId("fallback");
        card.setName("Built-in Fallback");
        card.setVersion(0);
        card.setActive(true);
        card.setBaseFee(new BigDecimal("4.99"));
        card.setPerMileRate(new BigDecimal("0.55"));
        card.setMinimumFee(new BigDecimal("4.99"));
        card.setFreeShippingThreshold(new BigDecimal("100.00"));
        card.setEffectiveFrom(LocalDateTime.now());
        return card;
    }
}