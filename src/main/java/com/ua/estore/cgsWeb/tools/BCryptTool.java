package com.ua.estore.cgsWeb.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTool {
    public static void main(String[] args) {
        var encoder = new BCryptPasswordEncoder();

        if (args.length == 0) {
            System.out.println("Usage: BCryptTool <password1> <password2> ...");
            System.out.println("Example: BCryptTool cole daria carter brynlee test");
            return;
        }

        for (String raw : args) {
            System.out.println(raw + " => " + encoder.encode(raw));
        }
    }
}
