package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.dto.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.AddressSuggestion;
import com.ua.estore.cgsWeb.services.maps.GooglePlacesAutocompleteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressApiController {

    private final GooglePlacesAutocompleteService autocompleteService;

    public AddressApiController(GooglePlacesAutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping("/suggest")
    public List<AddressSuggestion> suggest(@RequestParam("q") String q) {
        return autocompleteService.suggestUsAddresses(q);
    }

    @GetMapping("/resolve")
    public AddressDTO resolve(@RequestParam("placeId") String placeId) {
        return autocompleteService.resolveUsAddress(placeId);
    }
}
