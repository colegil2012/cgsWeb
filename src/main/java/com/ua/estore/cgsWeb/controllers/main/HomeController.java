package com.ua.estore.cgsWeb.controllers.main;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "main/home";
    }

    @GetMapping("/about")
    public String about() { return "main/about"; }
}
