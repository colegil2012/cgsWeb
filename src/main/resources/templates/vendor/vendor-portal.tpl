package templates.vendor

layout 'layout.tpl',
        title: 'CGS Web | Vendor',
        user: user,
        cartItems: cartItems,
        vendorDetail: vendorDetail,
        content: {
            div(class: 'hero') {
                h1('Vendor Dashboard')
                p('List a new product for sale in the Celtech General Store.')
            }
            div(class: 'wide-container') {
                // Alerts Section
                if (successMessages || errorMessages) {
                    div(class: 'alert-wrapper') {
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
                }

                if (vendorDetail) {
                    div(class: 'info-card vendor-profile-card') {
                        h2 'Your Vendor Profile'

                        div(class: 'vendor-item-row') {
                            // Column 1: Sidebar Logo
                            div(class: 'info-group vendor-profile-logo-group') {
                                img(src: vendorDetail.logo_url ?: '/images/site-images/default-vendor.png', alt: 'Logo Preview')
                                label(for: 'vendorLogoUpload', class: 'upload-link', 'Change Logo')
                                input(type: 'file', id: 'vendorLogoUpload', name: 'vendorLogo', style: 'display:none;', accept: 'image/*')
                            }

                            // Get default address for display
                            def addr = vendorDetail.addresses?.find { it.isDefault } ?: (vendorDetail.addresses?.isEmpty() ? null : vendorDetail.addresses[0])

                            // Row 1: 2 Columns
                            div(class: 'info-group', style: 'grid-column: 2 / span 1;') {
                                label('Business Name')
                                span(class: 'readonly-box', vendorDetail.name)
                            }
                            div(class: 'info-group', style: 'grid-column: 3 / span 2;') {
                                label('Street Address')
                                span(class: 'readonly-box', addr?.street ?: 'No Address Set')
                            }

                            // Row 2: 2 Columns
                            div(class: 'info-group', style: 'grid-column: 2 / span 1;') {
                                label('Address Type')
                                span(class: 'readonly-box', addr?.type ?: 'N/A')
                            }
                            div(class: 'info-group', style: 'grid-column: 3 / span 2;') {
                                label('City')
                                span(class: 'readonly-box', addr?.city ?: '-')
                            }

                            // Row 3: 3 Columns
                            div(class: 'info-group', style: 'grid-column: 2 / span 1;') {
                                label('State')
                                span(class: 'readonly-box', addr?.state ?: '-')
                            }
                            div(class: 'info-group', style: 'grid-column: 3 / span 1;') {
                                label('Zip')
                                span(class: 'readonly-box', addr?.zip ?: '-')
                            }
                            div(class: 'info-group', style: 'grid-column: 4 / span 1;') {
                                label('Status')
                                span(class: 'readonly-box', vendorDetail.active ? 'Active' : 'Inactive')
                            }
                        }
                    }
                }
            }

            div(class: 'container add-item-container') {
                div(style: 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; border-bottom: 2px solid var(--primary-green); padding-bottom: 1rem;') {
                    h2(style: 'margin:0; border:none; padding:0', 'Inventory Entry')
                    button(type: 'button', class: 'btn-add-row', id: 'addRowBtn', '+ Add Another Item')
                }

                form(action: '/vendor/portal/add-products', method: 'post', id: 'vendorForm', enctype: 'multipart/form-data') {
                    input(type: 'hidden', name: 'vendorId', value: vendorDetail?.id)

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
                                    categories.each { name, id ->
                                        option(value: name, id) }
                                }
                            }
                            div(class: 'info-group') {
                                label('Price ($)')
                                input(type: 'number', name: 'products[0].price', step: '0.01', min: '0.01', required: 'required')
                            }
                            div(class: 'info-group') {
                                label('Sale Price ($)')
                                input(type: 'number', name: 'products[0].salePrice', step: '0.01', min: '0.01', required: 'required')
                            }
                            div(class: 'info-group') {
                                label('Stock')
                                input(type: 'number', name: 'products[0].stock', min: '1', required: 'required')
                            }
                            div(class: 'info-group') {
                                label('Low Stock Threshold')
                                input(type: 'number', name: 'products[0].lowStockThreshold', min: '1', required: 'required')
                            }
                            div(class: 'info-group full-width attributes-container') {
                                div(class: 'info-group') {
                                    label('Weight (lb)')
                                    input(type: 'text', name: 'products[0].attributes.weight', placeholder: 'e.g. 1.5kg')
                                }
                                div(class: 'info-group') {
                                    label('L (in)')
                                    input(type: 'number', name: 'products[0].attributes.length', step: '0.1')
                                }
                                div(class: 'info-group') {
                                    label('W (in)')
                                    input(type: 'number', name: 'products[0].attributes.width', step: '0.1')
                                }
                                div(class: 'info-group') {
                                    label('H (in)')
                                    input(type: 'number', name: 'products[0].attributes.height', step: '0.1')
                                }
                            }
                            div(class: 'info-group full-width') {
                                label('Upload Image')
                                input(type: 'file', name: 'productImages[0]', accept: 'image/*')
                            }
                            div(class: 'info-group full-width') {
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