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
                        strong("Address #${i + 1}")

                        input(
                                type: 'hidden',
                                name: "addresses[${i}].addressId",
                                value: (addr?.addressId ?: '')
                        )

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