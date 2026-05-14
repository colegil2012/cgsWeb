package com.ua.estore.cgsWeb.models.user;

import com.ua.estore.cgsWeb.models.address.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String username;
    private String password;
    private String email;
    private List<String> roles;
    private boolean enabled;
    private boolean emailVerified;

    @Field(targetType = FieldType.OBJECT_ID)
    private String vendorId;

    private UserProfile profile;
    private List<Address> addresses = new ArrayList<>();

    @Data
    public static class UserProfile implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String firstName;
        private String middleInit;
        private String lastName;
        private String phoneNumber;
    }

}
