package com.ua.estore.cgsWeb.support;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.User;

import java.util.List;

public class TestUsers {

    private TestUsers() {}

    public static User sessionUser() {
       return sessionUser("test-user", List.of("USER"));
    }

    public static User sessionUser(String username, List<String> roles) {
        User u = new User();
        u.setId("test-id");
        u.setUsername(username);
        u.setRoles(roles);

        User.UserProfile p = new User.UserProfile();
        p.setFirstName("Test");
        u.setProfile(p);

        return u;
    }

    public static Cart sessionCart() {
        return sessionCartFor(sessionUser());
    }

    public static Cart sessionCartFor(User user) {
        Cart cart = new Cart();
        cart.setUserId(user.getId());
        return cart;
    }
}
