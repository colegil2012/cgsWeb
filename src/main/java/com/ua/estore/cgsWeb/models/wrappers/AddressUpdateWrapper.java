package com.ua.estore.cgsWeb.models.wrappers;

import com.ua.estore.cgsWeb.models.Address;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AddressUpdateWrapper {
    private List<Address> addresses = new ArrayList<>();
    private List<Address> newAddresses = new ArrayList<>();
}
