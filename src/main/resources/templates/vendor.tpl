package templates

layout 'layout.tpl',
        title: 'CGS Web | Vendor',
        user: user,
        cartItems: cartItems,
        content: {
            div(class: 'container') {
                div(style: 'grid-column: span 2') {
                    if (successMessages) {
                        div(class: 'alert alert-success') {
                            ul(style: 'margin:0; padding-left: 20px;') {
                                successMessages.each { msg -> li(msg) }
                            }
                        }
                    }
                    if (errorMessages) {
                        div(class: 'alert alert-error') {
                            ul(style: 'margin:0; padding-left: 20px;') {
                                errorMessages.each { msg -> li(msg) }
                            }
                        }
                    }
                }
                h1('Vendor Dashboard')
                p('List a new product for sale in the Celtech General Store.')
            }

            div(class: 'container add-item-container') {
                div(style: 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 2px solid var(--primary-green); padding-bottom: 1rem;') {
                    h2(style: 'margin:0; border:none; padding:0', 'Inventory Entry')
                    button(type: 'button', class: 'btn-add-row', id: 'addRowBtn', '+ Add Another Item')
                }

                form(action: '/vendor/add-products', method: 'post', id: 'vendorForm', enctype: 'multipart/form-data') {
                    input(type: 'hidden', name: 'vendorId', value: vendorId)

                    div(id: 'itemsContainer') {
                        div(class: 'vendor-item-row', 'data-index': '0') {
                            div(class: 'row-number') {
                                span('Product #1')
                            }
                            div(class: 'info-group') {
                                label('Product Name')
                                input(type: 'text', name: 'products[0].name', required: 'required', placeholder: 'e.g. Aleppo Salsa')
                            }
                            div(class: 'info-group') {
                                label('Category')
                                select(name: 'products[0].category') {
                                    categories.each { cat -> option(value: cat, cat) }
                                }
                            }
                            div(class: 'info-group') {
                                label('Price ($)')
                                input(type: 'number', name: 'products[0].price', step: '0.01', min: '0.01', required: 'required')
                            }
                            div(class: 'info-group') {
                                label('Stock')
                                input(type: 'number', name: 'products[0].stock', min: '1', required: 'required')
                            }
                            div(class: 'info-group two-thirds') { // Updated class
                                label('Upload Image')
                                input(type: 'file', name: 'productImages[0]', accept: 'image/*')
                            }
                            div(class: 'info-group full-width') { // Updated class
                                label('Description')
                                textarea(name: 'products[0].description', rows: '2', required: 'required', placeholder: 'Short description of your product...') {}
                            }
                        }
                    }

                    div(style: 'text-align: right; margin-top: 20px;') {
                        button(type: 'submit', class: 'btn-search', 'Submit All Products')
                    }
                }
            }
            script(src: '/scripts/vendor.js') {}
        }