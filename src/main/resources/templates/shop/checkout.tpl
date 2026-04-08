package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil
import com.ua.estore.cgsWeb.util.TimeUtil

layout 'layout.tpl',
        title: 'CGS Web | Secure Checkout',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        headContent: {
            script(src: 'https://sandbox.web.squarecdn.com/v1/square.js') {}
            link(rel: 'stylesheet', href: '/css/pages/checkout.css') },
        content: {
            def subtotal = cartItems.collect { it.price * it.quantity }.sum() ?: 0.00
            def shipping = totalShipping ?: 0.00
            def tax = ( subtotal + shipping ) * 0.07 // Example 7% tax
            def finalTotal = subtotal + shipping + tax
            def totalCents = Math.round(finalTotal * 100) as long

            div(class: 'checkout-page-container') {

                // Left Column 2/3 width
                div(class: 'checkout-page-data') {

                    div(class: 'checkout-section') {
                        h2("Checkout | ${user?.username?.toUpperCase()}\'s Cart (${cartCount})")
                        div(class: 'checkout-items') {

                            cartVendors.each { vendorId, vendor ->
                                def vendorItems = cartItems.findAll { it.vendorId == vendorId }
                                if (!vendorItems.isEmpty()) {
                                    div(class: 'checkout-vendor-group') {

                                        div(class: 'checkout-vendor-header') {
                                            div(class: 'checkout-vendor-logo') {
                                                img(src: ImageUrlUtil.resolve(vendor.logo_url, imagesBaseUrl) ?: '/images/placeholder.jpg')
                                            }

                                            div(class: 'checkout-vendor-name') {
                                                h3(vendor.name)
                                            }

                                            def shipFrom = vendor.addresses?.find { it.isDefault() } ?: vendor.addresses?.getAt(0)
                                            if (shipFrom) {
                                                div(class: 'checkout-ship-from') {
                                                    div(class: 'ship-from-label') {
                                                        img(src: ImageUrlUtil.resolve('/images/site-images/Roadie Circle R Icon_COLOR.png', imagesBaseUrl))
                                                        span('Pickup from: ')
                                                    }
                                                    div(class: 'ship-from-address') {
                                                        if (shipFrom.street2 != null) {
                                                            span("${shipFrom.street1} ${shipFrom.street2}")
                                                        } else {
                                                            span("${shipFrom.street1}")
                                                        }
                                                        span("${shipFrom.city}, ${shipFrom.state} ${shipFrom.zip}")
                                                    }
                                                }
                                            }
                                        }

                                        vendorItems.each { item ->
                                            div(class: 'checkout-item-row') {
                                                img(src: ImageUrlUtil.resolve(item.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                        alt: item.name, class: 'checkout-item-img')
                                                div(class: 'checkout-item-details') {
                                                    span(class: 'checkout-item-name', item.name)
                                                    span(class: 'checkout-item-qty', "Qty: ${item.quantity}")
                                                }
                                                span(class: 'checkout-item-price', "\$${String.format('%.2f', item.price * item.quantity)}")
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
                            div(class: 'filter-group') {
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
                                p(id: 'selected-address-street-2', class: 'address-preview-line',
                                        style: defaultAddr.street2 ? '' : 'display: none;',
                                        defaultAddr.street2 ?: '')
                                p(id: 'selected-address-city-state-zip', class: 'address-preview-line', "${defaultAddr.city}, ${defaultAddr.state} ${defaultAddr.zip}")
                            }

                            div(class: 'checkout-section-note') {
                                img(src: ImageUrlUtil.resolve('/images/site-images/Roadie UPS Logo Horiz_BROWN.png', imagesBaseUrl), alt: 'Delivery provided by Roadie')
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

                // ===== RIGHT COLUMN: Order Summary and payment 1/3 =====
                div(class: 'checkout-summary') {
                        h2('Order Summary')
                        div(class: 'summary-details', id: 'order-summary', 'data-subtotal': subtotal, 'data-tax-rate': 0.07) {
                            div(class: 'summary-row') {
                                span('Subtotal')
                                span(class: 'summary-value', id: 'subtotal-value', "\$${String.format('%.2f', subtotal)}")
                            }

                            div(id: 'shipping-estimates') {
                                if (shippingEstimates) {
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

                            // Form Submit, hidden fields for checkout
                            form(id: 'payment-form', method: 'POST', action: '/checkout/submit') {
                                input(type: 'hidden', name: 'sourceId', id: 'source-id')
                                input(type: 'hidden', name: 'totalCents', id: 'total-cents', value: totalCents)
                                input(type: 'hidden', name: 'tipCents', value: '0')
                                input(type: 'hidden', name: 'selectedAddress', id: 'selected-address-input')

                                button(id: 'card-button', type: 'button', class: 'btn-checkout',
                                        "Checkout")
                            }
                            div(class: 'checkout-footer') {
                                span(class: 'spacer') {}
                                img(src: ImageUrlUtil.resolve('/images/site-images/Roadie UPS Logo Horiz_BROWN.png', imagesBaseUrl), alt: 'Delivery provided by Roadie')
                                span(class: 'spacer') {}
                                img(src: ImageUrlUtil.resolve('/images/site-images/Square_Logo_2025_Black.png', imagesBaseUrl), alt: 'Secure Checkout provided by Square')
                                span(class: 'spacer') {}
                            }
                        }
                    }
                }

            script(src: '/scripts/shop/checkout.js') {}

            script {
                yieldUnescaped """
                    window.SQUARE_APP_ID = '${squareAppId}';
                    window.SQUARE_LOCATION_ID = '${squareLocationId}';
                    window.USER_ADDRESSES = ${jsonAddresses};
            """
            }
        }
