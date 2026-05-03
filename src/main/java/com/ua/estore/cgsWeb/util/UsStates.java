package com.ua.estore.cgsWeb.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical list of US state / territory 2-letter postal codes used in address forms.
 *
 *
 */
public final class UsStates {

    /** {@code code -> "Code – Full Name"} for option labels. Insertion-ordered. */
    public static final Map<String, String> CODE_TO_LABEL;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("IN", "IN – Indiana");
        m.put("KY", "KY – Kentucky");
        m.put("OH", "OH – Ohio");
        CODE_TO_LABEL = java.util.Collections.unmodifiableMap(m);
    }

    private UsStates() {}
}