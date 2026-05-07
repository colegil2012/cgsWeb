package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil
import com.ua.estore.cgsWeb.util.TimeUtil

layout 'layout.tpl',
        title: 'CGS Web | Secure Checkout',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/checkout.css')
            link(rel: 'stylesheet', href: '/css/pages/order-summary.css')
            link(rel: 'stylesheet', href: '/css/pages/order-items.css')
        },
        content: {
            def initialShipping = 0.00
            def initialTax = (subtotal + initialShipping) * (taxRate as BigDecimal)
            def initialTotal = subtotal + initialShipping + initialTax

            div(class: 'hero') {
                div(class: 'hero-content') {
                    img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Text Logo Middle.png', imagesBaseUrl),
                            alt: 'Celtech Logo',
                            class: 'hero-logo float-in')
                    h3("Secure Checkout")
                }
            }

            div(class: 'checkout-page-container') {

                // Left Column 2/3 width
                div(class: 'checkout-page-data') {

                    div(class: 'checkout-section') {
                        h2("Checkout | ${user?.username?.toUpperCase()}\'s Cart (${cartCount})")
                        div(class: 'order-items') {

                            cartVendors.each { vendorId, vendor ->
                                def vendorItems = cartItems.findAll { it.vendorId == vendorId }
                                if (!vendorItems.isEmpty()) {
                                    div(class: 'order-items-vendor-group') {

                                        div(class: 'order-items-vendor') {
                                            div(class: 'order-items-vendor-logo') {
                                                img(src: ImageUrlUtil.resolve(vendor.logoUrl, imagesBaseUrl) ?: '/images/placeholder.jpg')
                                            }

                                            div(class: 'order-items-vendor-name') {
                                                h3(vendor.name)
                                            }
                                        }

                                        vendorItems.each { item ->
                                            div(class: 'order-items-row') {
                                                img(src: ImageUrlUtil.resolve(item.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                        alt: item.name, class: 'order-items-img')
                                                div(class: 'order-items-details') {
                                                    span(class: 'order-items-name', item.name)
                                                    span(class: 'order-items-qty', "Qty: ${item.quantity}")
                                                }
                                                span(class: 'order-items-price', "\$${String.format('%.2f', item.price * item.quantity)}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(class: 'checkout-section') {
                        h2('Delivery Address')

                        def addresses = user?.addresses ?: []
                        if (addresses.isEmpty()) {
                            p(class: 'checkout-no-address', 'No saved addresses. Please add one in your account.')
                        } else {
                            div(class: 'checkout-address-field') {
                                label(for: 'checkout-address-select', 'Deliver to:')
                                select(id: 'checkout-address-select', name: 'selectedAddress', class: 'checkout-select') {
                                    addresses.each { addr ->
                                        def labelText = "${addr.street1}, ${addr.city}, ${addr.state} ${addr.zip}"
                                        def addrId = addr?.addressId ?: ''
                                        if (addr.isDefault()) {
                                            option(value: addrId, selected: 'selected', labelText)
                                        } else {
                                            option(value: addrId, labelText)
                                        }
                                    }
                                }
                            }

                            // Show selected address details
                            div(id: 'selected-address-preview', class: 'address-preview') {
                                def defaultAddr = addresses.find { it.isDefault() } ?: addresses[0]
                                p(class: 'address-preview-line') {
                                    strong("${user.profile.firstName} ${user.profile.lastName}")
                                }
                                p(id: 'selected-address-street-1', class: 'address-preview-line', defaultAddr.street1 ?: '')
                                p(id: 'selected-address-street-2',
                                        class: 'address-preview-line' + (defaultAddr.street2 ? '' : ' is-hidden'),
                                        defaultAddr.street2 ?: '')
                                p(id: 'selected-address-city-state-zip', class: 'address-preview-line', "${defaultAddr.city}, ${defaultAddr.state} ${defaultAddr.zip}")
                            }

                            div(class: 'checkout-section-note') {
                                img(src: ImageUrlUtil.resolve('/images/site-images/CGS Logo.png', imagesBaseUrl),
                                        alt: 'Delivery provided by Celtech General Store')
                            }
                        }
                    }

                    div(class: 'checkout-section') {
                        h2("Payment")

                        div(id: 'payment-status-container', class: 'payment-status') {}
                        div(id: 'card-container', class: 'card-container-box') {}
                        if (squareCustomerExists) {
                            if (savedUserCards != null) {
                                div(class: 'saved-user-card-container') {
                                    h3("Saved Payments")
                                    savedUserCards.eachWithIndex { card, i ->
                                        def isExpired = TimeUtil.isCardExpired(card.expYear.intValue(), card.expMonth.intValue())
                                        def cardClasses = isExpired ? 'saved-user-card expired' : 'saved-user-card'
                                        def formattedExp = String.format('%02d/%s', card.expMonth, String.valueOf(card.expYear).takeRight(2))

                                        div(class: cardClasses, 'data-card-id': card.cardId, 'data-exp-month': card.expMonth, 'data-exp-year': card.expYear) {
                                            span(class: 'saved-card-brand', card.cardBrand)
                                            span(class: 'saved-card-number', "**** **** **** ${card.last4}")
                                            span(class: 'saved-card-exp', formattedExp)
                                        }
                                    }
                                }
                            }
                        }

                        div(class: 'checkout-section-note') {
                            img(src: ImageUrlUtil.resolve('/images/site-images/Square_Logo_2025_Black.png', imagesBaseUrl), alt: 'Secure Checkout provided by Square')
                        }
                    }
                }

                // order-summary.css / shared styling with cart
                div(class: 'checkout-summary') {
                    h2('Order Summary')
                    div(class: 'order-summary summary-details', id: 'order-summary', 'data-subtotal': subtotal, 'data-tax-rate': taxRate) {
                        div(class: 'summary-row') {
                            span('Subtotal')
                            span(class: 'summary-value', id: 'subtotal-value', "\$${String.format('%.2f', subtotal)}")
                        }

                        div(id: 'shipping-estimates') {
                            if (shippingEstimates) {
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
                            span(class: 'summary-value', id: 'tax-value', "\$${String.format('%.2f', initialTax)}")
                        }
                        hr()
                        div(class: 'summary-row total') {
                            span('Total')
                            span(class: 'summary-value', id: 'total-value', "\$${String.format('%.2f', initialTotal)}")
                        }

                        // Form Submit, hidden fields for checkout
                        form(id: 'payment-form', method: 'POST', action: '/checkout/submit') {
                            input(
                                    type: 'hidden',
                                    name: (csrfParamName ?: '_csrf'),
                                    value: (csrfToken ?: '')
                            )
                            input(type: 'hidden', name: 'sourceId', id: 'source-id')
                            input(type: 'hidden', name: 'totalCents', id: 'total-cents', value: totalCents)
                            input(type: 'hidden', name: 'tipCents', value: '0')
                            input(type: 'hidden', name: 'selectedAddress', id: 'selected-address-input')
                            input(type: 'hidden', name: 'deliveryInstructions', id: 'delivery-instructions-input')
                            input(type: 'hidden', name: 'idempotencyKey', id: 'idempotency-key-input')

                            button(id: 'card-button', type: 'button', class: 'btn btn-pill btn-block', "Checkout")
                        }
                        div(class: 'checkout-footer') {
                            img(src: ImageUrlUtil.resolve('/images/site-images/CGS Logo.png', imagesBaseUrl),
                                    alt: 'Delivery provided by Celtech General Store')
                        }
                    }
                }
            }

            include template: 'partials/checkout-confirm-modal.tpl'

            script(src: '/scripts/shop/checkout.js') {}
            script(src: '/scripts/shop/checkout-confirm.js') {}
            script(src: '/scripts/shop/checkout-submit.js') {}

            script {
                yieldUnescaped """
                    window.USER_ADDRESSES = ${jsonAddresses};
            """
            }
        }
