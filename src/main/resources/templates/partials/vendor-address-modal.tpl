package templates.partials

div(class: 'modal-overlay', id: 'updateAddressOverlay', style: 'display:none;') {
    div(class: 'modal') {
        div(class: 'modal-header') {
            h3('Update Addresses')
            button(type: 'button', class: 'modal-close', id: 'closeUpdateAddress', '×')
        }

        form('data-address-scope': 'vendor', action: '/vendor/addresses', method: 'post', class: 'form-group') {

            input(
                    type: 'hidden',
                    name: (_csrf?.parameterName ?: '_csrf'),
                    value: (_csrf?.token ?: '')
            )

            if (vendorDetail?.addresses && !vendorDetail.addresses.isEmpty()) {
                h4('Existing Addresses')
                vendorDetail.addresses.eachWithIndex { addr, i ->
                    div(class: 'address-edit-card') {
                        strong("Address #${i + 1}")

                        input(
                                type: 'hidden',
                                name: "vAddresses[${i}].addressId",
                                value: (addr?.addressId ?: '')
                        )

                        div(class: 'form-control') {
                            label("Street")
                            input(type: 'text', name: "vAddresses[${i}].street", value: (addr?.street ?: ''))
                        }
                        div(class: 'form-control') {
                            label("City")
                            input(type: 'text', name: "vAddresses[${i}].city", value: (addr?.city ?: ''))
                        }
                        div(class: 'form-control') {
                            label("State")
                            input(
                                    type: 'text',
                                    name: "vAddresses[${i}].state",
                                    value: (addr?.state ?: ''),
                                    required: 'required',
                                    pattern: '^[A-Za-z]{2}$',
                                    title: 'Use 2-letter state code (e.g., KY)',
                                    maxlength: '2')
                        }
                        div(class: 'form-control') {
                            label("Zip")
                            input(
                                    type: 'text',
                                    name: "vAddresses[${i}].zip",
                                    value: (addr?.zip ?: ''),
                                    required: 'required',
                                    pattern: '^\\d{5}(-\\d{4})?$',
                                    title: 'Use ZIP (12345) or ZIP+4 (12345-6789)',
                                    inputmode: 'numeric'
                            )
                        }

                        div(class: 'update-address-footer') {
                            div(class: 'form-control') {
                                if (addr?.isDefault) {
                                    input(type: 'checkbox', name: "vAddresses[${i}].default", value: 'true', checked: 'checked')
                                } else {
                                    input(type: 'checkbox', name: "vAddresses[${i}].default", value: 'true')
                                }
                                label(style: 'margin:0;', 'Make default')
                            }
                        }
                    }
                }
            }

            hr()

            div(style: 'display:flex; justify-content:space-between; align-items:center; gap: 12px;') {
                h4('Add New Addresses')
                button(type: 'button', class: 'btn-small', id: 'addAddressBlockBtn', '+ Add Address')
            }
            div(id: 'newAddressesContainer') {
                // JS will append blocks here
            }

            div(style: 'display:flex; gap: 10px; justify-content:flex-end; margin-top: 10px;') {
                button(type: 'button', class: 'btn-small', id: 'cancelUpdateAddress', 'Cancel')
                button(type: 'submit', class: 'btn', style: 'width:auto;', 'Save Changes')
            }
        }
    }
}

