package templates

layout 'layout.tpl',
        title: 'CGS Web | My Account',
        username: username,
        cartItems: cartItems,
        role: role,
        user: user,
        vendorInfo: vendorInfo,
        vendorProducts: vendorProducts,
        content: {
            div(class: 'account-container') {

                // Sidebar: User Information
                div(class: 'info-card') {
                    h2('User Profile')
                    hr()

                    div(class: 'info-group') {
                        label('Username')
                        span(class: 'readonly-box', user?.username ?: 'N/A')
                    }

                    div(class: 'info-group') {
                        label('Account Role')
                        span(class: 'readonly-box') {
                            yield user?.role ?: 'CUSTOMER'
                        }
                    }

                    div(class: 'info-group') {
                        label('Security')
                        span(class: 'readonly-box', '••••••••••••')
                        small(class: 'text-muted', 'Password hidden for security')
                    }

                    if (user?.vendorId) {
                        div(class: 'info-group') {
                            label('Vendor Name')
                            span(class: 'readonly-box', vendorInfo?.name ?: 'Loading...')
                        }
                        div(class: 'info-group') {
                            label('Vendor Identifier')
                            span(class: 'readonly-box', user.vendorId)
                        }
                    }
                }

                // Main Content: Vendor Inventory (Conditional)
                div {
                    if (vendorProducts != null) {
                        div(class: 'vendor-section') {
                            div(class: 'vendor-header') {
                                h3(style: 'margin:0', 'Inventory Management')
                                span(class: 'badge-vendor', 'Active Vendor')
                            }

                            table(class: 'vendor-table') {
                                thead {
                                    tr {
                                        th('Product Name')
                                        th('Price')
                                        th('Stock Status')
                                    }
                                }
                                tbody {
                                    vendorProducts.each { product ->
                                        tr {
                                            td(style: 'font-weight:bold', product.name)
                                            td("\$${product.price}")
                                            td {
                                                // Simple logic for stock display
                                                if (product.stock && product.stock > 0) {
                                                    span(style: 'color: green', "In Stock (${product.stock})")
                                                } else {
                                                    span(style: 'color: red', 'Out of Stock')
                                                }
                                            }
                                        }
                                    }
                                    if (vendorProducts.isEmpty()) {
                                        tr {
                                            td(colspan: 3, style: 'text-align:center; padding: 40px;', 'You haven\'t listed any products yet.')
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Helpful message for regular customers
                        div(class: 'info-card', style: 'text-align:center; padding: 50px;') {
                            h3('Welcome to your Dashboard!')
                            p('As a valued customer, you can track your orders and manage your profile here.')
                            a(href: '/shop', class: 'btn-search', style: 'text-decoration:none; display:inline-block; margin-top:15px;', 'Start Shopping')
                        }
                    }
                }
            }
        }