package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User { //implements UserDetails {
    @Id
    private String id;
    private String username;
    private String password;
    private String email;
    private String role;

    @Field(targetType = FieldType.OBJECT_ID)
    private String vendorId;

    private UserProfile profile;
    private List<Address> addresses = new ArrayList<>();

    @Data
    public static class UserProfile {
        private String firstName;
        private String middleInit;
        private String lastName;
        private String phoneNumber;
    }

    @Data
    public static class Address {
        private String type;  //SHIPPING, BILLING
        private String street;
        private String city;
        private String state;
        private String zip;
        private boolean isDefault;
    }

}
