package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Home',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        content: {
            //Calculate total, subtotal and tax
            def subtotal = cartItems.collect { it.price * it.quantity }.sum() ?: 0.00
            def shipping = totalShipping ?: 0.00
            def tax = ( subtotal + shipping ) * 0.07 // Example 7% tax
            def finalTotal = subtotal + shipping + tax

            if(cartItems == null || cartItems.isEmpty()) {
                div(class: 'empty-cart-message') {
                    h3('Your cart is empty!')
                    p('It looks like you haven\'t added any products to your cart yet.')
                    a(href: '/shop', class: 'btn-search', 'Start Shopping')
                }
            } else {

                div(class: 'cart-page-container') {
                    div(class: 'cart-main') {
                        div(class: 'cart-header') {
                            span(class: 'cart-header-text', "${user.username.toUpperCase()}'s Cart:")
                        }
                        div(class: 'cart-items') {
                            div(class: 'cart-grid') {
                                cartItems.each { item ->
                                    div(class: 'cart-card') {
                                        div(class: "cart-card-section") {
                                            img(src: ImageUrlUtil.resolve(item.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg', alt: item.name)
                                        }
                                        div(class: "cart-card-section") {
                                            p(class: 'cart-product-name', item.name)
                                            p(class: 'cart-product-vendor', "By: ${item.vendorName}")
                                        }
                                        div(class: "cart-card-section") {
                                            p(class: 'cart-product-price', item.price)
                                        }
                                        div(class: "cart-card-section quantity-controls") {
                                            a(href: "/cart/remove/${item.id}", class: 'btn-qty minus', '-')
                                            span(class: 'cart-product-qy', item.quantity)
                                            a(href: "/cart/add/${item.id}", class: 'btn-qty plus', '+')
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(class: 'cart-summary') {
                        h2('Order Summary')
                        div(class: 'summary-details',
                            id: 'order-summary',
                            'data-subtotal': subtotal,
                            'data-tax-rate': 0.07
                        ) {
                            div(class: 'summary-row') {
                                span('Subtotal')
                                span(class: 'summary-value', id: 'subtotal-value', "\$${String.format('%.2f', subtotal)}")
                            }

                            div(id: 'shipping-estimates') {
                                if(shippingEstimates) {
                                    shippingEstimates.each { estimate ->
                                        div(class: 'summary-row shipping-detail') {
                                            span("Shipping (${estimate.vendor})")
                                            span(class: 'summary-value', "\$${String.format('%.2f', estimate.cost)}")
                                        }
                                    }
                                }
                            }
                            div(class: 'summary-row') {
                                span('Estimated Tax')
                                span(class: 'summary-value', id: 'tax-value', "\$${String.format('%.2f', tax)}")
                            }
                            hr()
                            div(class: 'summary-row total') {
                                span('Total')
                                span(class: 'summary-value', id: 'total-value', "\$${String.format('%.2f', finalTotal)}")
                            }
                        }
                        div(class: 'payment-methods') {
                            p('Accepted Payments:')
                            div(class: 'payment-icons') {
                                span(class: 'payment-badge', 'Visa')
                                span(class: 'payment-badge', 'MasterCard')
                                span(class: 'payment-badge', 'PayPal')
                            }
                        }

                        div(class: 'cart-actions-block') {
                            div(class: 'filter-group ship-to-row') {
                                label(for: 'shipping-dropdown', 'Ship To...')

                                div(class: 'ship-to-controls') {
                                    def addresses = user?.addresses ?: []
                                    if (addresses.isEmpty()) {
                                        select(id: 'shipping-dropdown', name: 'shippingAddressIndex', disabled: 'disabled') {
                                            option(value: '', 'No saved addresses (add one in your account)')
                                        }
                                    } else {
                                        select(id: 'shipping-dropdown', name: 'shippingAddressId') {
                                            addresses.each { a ->
                                                def labelText = "${a.street1}, ${a.city}, ${a.state} ${a.zip}"
                                                def addrId = a?.addressId ?: ''
                                                if (a.isDefault()) {
                                                    option(value: addrId, selected: 'selected', labelText)
                                                } else {
                                                    option(value: addrId, labelText)
                                                }
                                            }
                                        }
                                    }

                                    button(type: 'button', class: 'btn-small', id: 'openUpdateAddress', 'Edit')
                                }
                            }

                            a(href: '/checkout', class: 'btn-checkout', 'Proceed to Checkout')
                        }
                    }
                    include template: 'partials/user-address-modal.tpl'
                }
            }
            script(src: '/scripts/cart/cart-update.js') {}
            script(src: '/scripts/address/address-update.js') {}
        }

