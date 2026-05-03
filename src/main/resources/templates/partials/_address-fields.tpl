package templates.partials

/*
 * Renders one address-card body (every field except the wrapping <div class='address-edit-card'>
 * and the surrounding "default" checkbox row). Used by both user-address-modal.tpl and
 * vendor-address-modal.tpl so the field set stays in sync.
 *
 * Bindings expected from the caller:
 *   addr            – the Address being rendered (or null for blank-state)
 *   namePrefix      – form name prefix, e.g. "addresses[2]"
 *   showTypeField   – boolean; user modal sets true, vendor modal false
 *   usStates        – the Map<String,String> from GlobalModelAdvice
 */

if (showTypeField) {
    div(class: 'form-control') {
        label("Type")
        def t = addr?.type?.trim()?.toUpperCase()
        select(name: "${namePrefix}.type", required: 'required') {
            ['SHIPPING', 'BILLING', 'ALTERNATE'].each { typeOption ->
                if (typeOption == t) option(value: typeOption, selected: 'selected', typeOption)
                else                 option(value: typeOption, typeOption)
            }
        }
    }
}

div(class: 'form-control') {
    label("Street")
    input(type: 'text', name: "${namePrefix}.street1", value: (addr?.street1 ?: ''))
}

div(class: 'form-control') {
    label("Apt / Suite (optional)")
    input(type: 'text', name: "${namePrefix}.street2", value: (addr?.street2 ?: ''))
}

div(class: 'form-control') {
    label("City")
    input(type: 'text', name: "${namePrefix}.city", value: (addr?.city ?: ''))
}

div(class: 'form-control') {
    label("State")
    def selectedState = (addr?.state ?: '').toString().trim().toUpperCase()
    select(name: "${namePrefix}.state", required: 'required') {
        option(value: '', '— Select state —')
        usStates.each { code, niceLabel ->
            if (code == selectedState) option(value: code, selected: 'selected', niceLabel)
            else                       option(value: code, niceLabel)
        }
    }
}

div(class: 'form-control') {
    label("Zip")
    input(
            type: 'text',
            name: "${namePrefix}.zip",
            value: (addr?.zip ?: ''),
            required: 'required',
            pattern: '^\\d{5}(-\\d{4})?$',
            title: 'Use ZIP (12345) or ZIP+4 (12345-6789)',
            inputmode: 'numeric'
    )
}