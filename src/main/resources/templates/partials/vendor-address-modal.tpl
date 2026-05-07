package templates.partials

div(class: 'modal-overlay', id: 'updateAddressOverlay', style: 'display:none;') {
    div(class: 'modal') {
        div(class: 'modal-header') {
            h3('Update Addresses')
            button(type: 'button', class: 'modal-close', id: 'closeUpdateAddress', '×')
        }

        form(action: '/vendor/addresses', method: 'post', class: 'form-group') {

            input(
                    type: 'hidden',
                    name: (csrfParamName ?: '_csrf'),
                    value: (csrfToken ?: '')
            )

            if (vendorDetail?.addresses && !vendorDetail.addresses.isEmpty()) {
                h4('Existing Addresses')
                vendorDetail.addresses.eachWithIndex { addr, i ->
                    div(class: 'address-edit-card') {
                        // Wrap header in a real header div so the grid-row
                        // column-spanning rules in modal.css apply cleanly
                        // (matches the user-address-modal's structure).
                        div(class: 'address-edit-card-header') {
                            strong("Address #${i + 1}")
                            input(
                                    type: 'hidden',
                                    name: "addresses[${i}].addressId",
                                    value: (addr?.addressId ?: '')
                            )
                        }

                        layout 'partials/_address-fields.tpl',
                                addr: addr,
                                namePrefix: "addresses[${i}]",
                                showTypeField: false,
                                usStates: usStates

                        div(class: 'update-address-footer') {
                            div(class: 'form-control') {
                                if (addr?.isDefault) {
                                    input(type: 'checkbox', name: "addresses[${i}].default", value: 'true', checked: 'checked')
                                } else {
                                    input(type: 'checkbox', name: "addresses[${i}].default", value: 'true')
                                }
                                label('Make default')
                            }
                        }
                    }
                }
            }

            hr()

            div(class: 'modal-section-header') {
                h4('Add New Addresses')
                button(type: 'button', class: 'btn btn-small', id: 'addAddressBlockBtn', '+ Add Address')
            }
            div(id: 'newAddressesContainer') {
                // JS will append blocks here
            }

            div(class: 'modal-actions') {
                button(type: 'button', class: 'btn btn-secondary', id: 'cancelUpdateAddress', 'Cancel')
                button(type: 'submit', class: 'btn', 'Save Changes')
            }
        }
    }
}
