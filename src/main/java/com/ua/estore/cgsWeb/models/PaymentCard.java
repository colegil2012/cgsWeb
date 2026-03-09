package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCard {
    private String cardId;
    private String cardBrand;
    private String last4;
    private Long expMonth;
    private Long expYear;
    private boolean enabled;
}
