package com.ua.estore.cgsWeb.models.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CardDTO implements Serializable {
    private String id;
    private String cardBrand;
    private String last4;
    private Long expMonth;
    private Long expYear;
}
