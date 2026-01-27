package com.ua.estore.cgsWeb.models.wrappers;

import com.ua.estore.cgsWeb.models.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AddressUpdateWrapper {
    private List<User.Address> addresses = new ArrayList<>();
    private List<User.Address> newAddresses = new ArrayList<>();
}
