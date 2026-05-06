package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | My Cart',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/cart.css')
            link(rel: 'stylesheet', href: '/css/pages/order-summary.css')
        },
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
                    a(href: '/shop', class: 'btn', 'Start Shopping')
                }
            } else {

                div(class: 'hero') {
                    div(class: 'hero-content') {
                        img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Text Logo Middle.png', imagesBaseUrl),
                                alt: 'Celtech Logo',
                                class: 'hero-logo float-in')
                        h3("Secure Checkout")
                    }
                }

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
                                            img(src: ImageUrlUtil.resolve(item.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                    alt: item.name)
                                        }
                                        div(class: "cart-card-section") {
                                            p(class: 'cart-product-name', item.name)
                                            p(class: 'cart-product-vendor', "From ${item.vendorName}")
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

                    // order-summary.js / share style with checkout
                    div(class: 'checkout-summary') {
                        h2('Order Summary')
                        div(class: 'order-summary summary-details',
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
                                            span("Shipping")
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

                        div(class: 'cart-actions-block') {
                            div(class: 'ship-to-row') {
                                label(for: 'shipping-dropdown', 'Deliver To...')

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

                                    button(type: 'button', class: 'btn btn-small', id: 'openUpdateAddress', 'Edit')
                                }
                            }

                            a(id: 'checkout-link', href: '/checkout', class: 'btn btn-pill btn-block', 'Proceed to Checkout')
                        }
                        div(class: 'checkout-footer') {
                            img(src: ImageUrlUtil.resolve('/images/site-images/CGS Logo.png', imagesBaseUrl),
                                    alt: 'Delivery provided by Celtech General Store')
                        }
                    }
                    include template: 'partials/user-address-modal.tpl'
                }
            }
            script(src: '/scripts/shop/cart.js') {}
            script(src: '/scripts/address/address-update.js') {}
        }
