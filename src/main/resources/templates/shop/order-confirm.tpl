package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

import java.time.format.DateTimeFormatter

/*
 * Order receipt page rendered at /checkout/confirmation/{orderId}.
 * Inputs (model):
 *   order         – Order document with snapshotted customer/items/totals/shipTo
 *   orderVendors  – Map<String, Vendor>  for grouping items by vendor
 *   user          – session user (for nav)
 *   cartItems     – passed for layout consistency (post-checkout this is the
 *                   freshly-cleared cart, expected to be empty here)
 *   cartCount     – likewise; layout reads it for the cart badge
 *   imagesBaseUrl – DO Spaces base url for prod, otherwise local /images
 */

def DATE_FMT = DateTimeFormatter.ofPattern('MMMM d, yyyy \'at\' h:mm a')

def fmt = { val ->
    val == null ? '0.00' : String.format('%.2f', val)
}

def placedAt = order?.placedAt?.format(DATE_FMT) ?: '—'

layout 'layout.tpl',
        title: "CGS Web | Order ${order?.orderNumber ?: ''}",
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/order-confirm.css')
            link(rel: 'stylesheet', href: '/css/pages/order-items.css')
            link(rel: 'stylesheet', href: '/css/pages/order-summary.css')
        },
        content: {

            div(class: 'confirmation-page') {

                /* ---- Banner: centered text logo + thank-you ---------------- */
                div(class: 'confirmation-banner') {
                    div(class: 'confirmation-banner-logo') {
                        img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Text Logo Middle.png', imagesBaseUrl),
                                alt: 'Celtech General Store')
                    }
                    div(class: 'confirmation-banner-text') {
                        h1("Thank you, ${order?.customer?.firstName ?: 'friend'}!")
                        p {
                            yield 'Your order '
                            span(class: 'confirmation-order-number',
                                    "#${order?.orderNumber ?: '(pending)'}")
                            yield " was placed on ${placedAt}."
                        }
                        p('We\'ve emailed a copy of this receipt for your records.')
                    }
                }

                /* ---- Two-column layout ------------------------------------ */
                div(class: 'confirmation-grid') {

                    /* Left: items + delivery */
                    div(class: 'confirmation-left') {

                        div(class: 'confirmation-section') {
                            h2('Items in this order')

                            if (!order?.items || order.items.isEmpty()) {
                                p(class: 'confirmation-empty',
                                        'No items recorded for this order.')
                            } else {
                                div(class: 'order-items') {
                                    orderVendors.each { vendorId, vendor ->
                                        def vendorItems = order.items.findAll {
                                            it.vendorId == vendorId
                                        }
                                        if (vendorItems.isEmpty()) return

                                        div(class: 'order-items-vendor-group') {
                                            div(class: 'order-items-vendor') {
                                                div(class: 'order-items-vendor-logo') {
                                                    img(src: ImageUrlUtil.resolve(
                                                            vendor.logoUrl,
                                                            imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                            alt: vendor.name)
                                                }
                                                div(class: 'order-items-vendor-name') {
                                                    h3(vendor.name)
                                                }
                                            }

                                            vendorItems.each { item ->
                                                div(class: 'order-items-row') {
                                                    img(src: ImageUrlUtil.resolve(
                                                            item.imageUrl,
                                                            imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                            alt: item.name,
                                                            class: 'order-items-img')
                                                    div(class: 'order-items-details') {
                                                        span(class: 'order-items-name', item.name)
                                                        span(class: 'order-items-qty',
                                                                "Qty: ${item.quantity}")
                                                    }
                                                    span(class: 'order-items-price',
                                                            "\$${fmt(item.lineTotal)}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        div(class: 'confirmation-section') {
                            h2('Delivering to')
                            def shipTo = order?.shipTo
                            div(class: 'confirmation-address-block') {
                                if (shipTo) {
                                    p {
                                        strong("${order.customer?.firstName ?: ''} " +
                                                "${order.customer?.lastName ?: ''}".trim())
                                    }
                                    p(shipTo.street1 ?: '')
                                    if (shipTo.street2) {
                                        p(shipTo.street2)
                                    }
                                    p("${shipTo.city ?: ''}, ${shipTo.state ?: ''} ${shipTo.zip ?: ''}")
                                    if (order.customer?.phone) {
                                        p("Phone on file: ${order.customer.phone}")
                                    }
                                } else {
                                    p('Delivery address unavailable.')
                                }
                            }

                            if (order?.deliveryInstructions) {
                                p(class: 'confirmation-instructions') {
                                    strong('Instructions: ')
                                    yield order.deliveryInstructions
                                }
                            }
                        }
                    }

                    /* Right: totals + actions (sticky)
                     * Adopts the shared .checkout-summary + .order-summary
                     * components from order-summary.css. */
                    div(class: 'confirmation-right') {

                        div(class: 'checkout-summary') {
                            h2('Order Summary')
                            def t = order?.totals
                            div(class: 'order-summary') {
                                div(class: 'summary-row') {
                                    span('Subtotal')
                                    span(class: 'summary-value', "\$${fmt(t?.subtotal)}")
                                }
                                div(class: 'summary-row') {
                                    span('Shipping')
                                    span(class: 'summary-value', "\$${fmt(t?.shipping)}")
                                }
                                if (t?.discount && t.discount.signum() > 0) {
                                    div(class: 'summary-row') {
                                        span('Discount')
                                        span(class: 'summary-value', "-\$${fmt(t.discount)}")
                                    }
                                }
                                div(class: 'summary-row') {
                                    span('Estimated Tax')
                                    span(class: 'summary-value', "\$${fmt(t?.tax)}")
                                }
                                div(class: 'summary-row total') {
                                    span('Total')
                                    span(class: 'summary-value', "\$${fmt(t?.total)}")
                                }
                            }

                            div(class: 'confirmation-actions') {
                                a(href: '/shop', class: 'btn', 'Continue shopping')
                                a(href: '/account?tab=orders', class: 'btn btn-secondary',
                                        'View my orders')

                                if (order?.status?.name() == 'PENDING') {
                                    button(type: 'button',
                                            class: 'btn btn-cancel-order',
                                            id: 'cancelOrderBtn',
                                            'data-order-id': order.id,
                                            'data-placed-at': order.placedAt?.toString() ?: '',
                                            'data-window-seconds': cancelWindowSeconds,
                                            'Cancel this order')
                                    p(class: 'confirmation-cancel-help',
                                            id: 'cancelOrderHelp',
                                            "You can cancel within ${cancelWindowSeconds.intdiv(60)} minutes of placing your order.")
                                }
                            }
                        }
                    }

                    if (recommendations && !recommendations.isEmpty()) {
                        div(class: 'confirmation-section confirmation-recommendations') {
                            h2('More from the makers')
                            p(class: 'confirmation-recs-sub',
                                    'Picks from the small farms and crafters in your order.')

                            div(class: 'confirmation-recs-grid') {
                                recommendations.each { rec ->
                                    a(href: "/shop/view/${rec.id}", class: 'confirmation-rec-card') {
                                        div(class: 'confirmation-rec-image') {
                                            img(src: ImageUrlUtil.resolve(rec.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg',
                                                    alt: rec.name ?: '')
                                        }
                                        div(class: 'confirmation-rec-body') {
                                            span(class: 'confirmation-rec-name', rec.name ?: '')
                                            span(class: 'confirmation-rec-price') {
                                                if (rec.salePrice != null && rec.salePrice.signum() > 0) {
                                                    span(class: 'rec-price-sale', "\$${String.format('%.2f', rec.salePrice)}")
                                                    span(class: 'rec-price-original', "\$${String.format('%.2f', rec.price)}")
                                                } else {
                                                    yield "\$${String.format('%.2f', rec.price ?: 0)}"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                /* ---- "What's next" strip ------------------------------------- */
                div(class: 'confirmation-next-steps') {
                    div(class: 'confirmation-next-step') {
                        h4('Order received')
                        p("We've recorded order #${order?.orderNumber ?: ''} and it's queued for the next delivery route.")
                    }
                    div(class: 'confirmation-next-step') {
                        h4('Scheduling')
                        p("You'll get an email when your delivery is scheduled — usually within a day.")
                    }
                    div(class: 'confirmation-next-step') {
                        h4('Day of delivery')
                        p('A text with a tracking link goes out the morning your driver heads out.')
                    }
                }
            }
            if (order?.status?.name() == 'PENDING') {
                script(src: '/scripts/shop/order-cancel.js') {}
            }
        }
