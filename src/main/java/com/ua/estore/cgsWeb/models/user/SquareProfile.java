package com.ua.estore.cgsWeb.models.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquareProfile {
    private String squareCustomerId;
    private String idempotencyKey;

    private List<PaymentCard> paymentCards;
}
