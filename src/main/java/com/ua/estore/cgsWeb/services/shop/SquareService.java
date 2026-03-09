package com.ua.estore.cgsWeb.services.shop;

import com.squareup.square.SquareClient;
import com.squareup.square.core.SquareApiException;
import com.squareup.square.core.SyncPagingIterable;
import com.squareup.square.types.*;
import com.ua.estore.cgsWeb.config.props.SquareProperties;
import com.ua.estore.cgsWeb.models.PaymentCard;
import com.ua.estore.cgsWeb.models.SquareProfile;
import com.ua.estore.cgsWeb.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquareService {

    private final SquareClient client;
    private final SquareProperties squareProperties;

    /**********************************************************
     * Square Client Configuration Accessors
     *********************************************************/

    public String getApplicationId() { return squareProperties.applicationId(); }
    public String getLocationId() { return squareProperties.locationId(); }

    /**********************************************************
     * Create Payment Object in Square
     *********************************************************/

    public CreatePaymentResponse createPayment(String sourceId, String idempotencyKey,
                                               long amountInCents, long tipAmountInCents,
                                               Currency currency) {

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .sourceId(sourceId)
                .idempotencyKey(idempotencyKey)
                .amountMoney(Money.builder()
                        .amount(amountInCents)
                        .currency(currency)
                        .build())
                .tipMoney(Money.builder()
                        .amount(tipAmountInCents)
                        .currency(currency)
                        .build())
                .autocomplete(true)
                .build();

        try {
            return client.payments().create(request);
        } catch (SquareApiException e) {
            log.error("Square API error: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }

    /**********************************************************
     * Check if Square Customer Exists by Reference ID (User ID)
     *********************************************************/

    public boolean squareCustomerExists(User user) {
        SquareProfile sProfile = user.getSquareProfile();

        GetCustomerResponse response = client.customers().get(GetCustomersRequest.builder()
                .customerId(sProfile.getSquareCustomerId())
                .build()
        );

        return response.getCustomer().isPresent();
    }

    /**********************************************************
     * Create Customer Object in Square
     *********************************************************/

    public String createCustomer(User user) {
        User.UserProfile profile = user.getProfile();

        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .idempotencyKey(UUID.nameUUIDFromBytes(user.getId().getBytes(StandardCharsets.UTF_8)).toString())
                .givenName(profile != null ? profile.getFirstName() : null)
                .familyName(profile != null ? profile.getLastName() : null)
                .emailAddress(user.getEmail())
                .phoneNumber(profile != null ? profile.getPhoneNumber() : null)
                .referenceId(user.getId())
                .build();

        try {
            CreateCustomerResponse response = client.customers().create(request);
            String customerId = response.getCustomer()
                    .flatMap(Customer::getId)
                    .orElseThrow(() -> new RuntimeException("Customer created but no ID returned"));

            log.info("Square customer created with ID: {}", customerId);
            return customerId;
        } catch (SquareApiException e) {
            log.error("Square API error creating customer: {}", e.getMessage());
            throw new RuntimeException("Failed to create Square customer: " + e.getMessage());
        }
    }

    /********************************************************************
     * Retrive Cards for user
     *******************************************************************/

    public List<PaymentCard> getUserCards(User user) {
        SquareProfile squareProfile = user.getSquareProfile();
        if (squareProfile == null || squareProfile.getSquareCustomerId() == null) {
            log.warn("No Square profile found for user {}", user.getId());
            throw new RuntimeException("No Square profile found for user " + user.getId());
        }

        List<PaymentCard> cardViewObjects = new ArrayList<>();

        SyncPagingIterable<Card> cards = client.cards().list(ListCardsRequest.builder()
                .customerId(squareProfile.getSquareCustomerId())
                .build());

        for(Card c : cards) {
            PaymentCard pc = new PaymentCard();
            pc.setCardId(String.valueOf(c.getId().orElse(null)));
            pc.setCardBrand(String.valueOf(c.getCardBrand().orElse(null)));
            pc.setLast4(String.valueOf(c.getLast4().orElse(null)));
            pc.setExpMonth(c.getExpMonth().orElse(0L));
            pc.setExpYear(c.getExpYear().orElse(0L));
            cardViewObjects.add(pc);
        }

        return cardViewObjects;
    }
}
