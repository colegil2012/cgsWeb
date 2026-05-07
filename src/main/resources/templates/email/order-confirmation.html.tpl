/*
 * HTML order receipt email.
 *
 * Email rendering rules of engagement:
 *   - Inline styles only. CSS files don't get loaded; <style> blocks are
 *     unreliable across clients (Outlook/Gmail-Web mangle them).
 *   - Tables for layout. Flexbox/grid are unsafe in Outlook.
 *   - Hex values hardcoded. They're chosen to match the current design
 *     tokens — when tokens change, this file needs a manual update. The
 *     mapping is documented next to each color below.
 *
 * Bindings expected from the email-rendering side:
 *   order        — Order document (with snapshotted items + totals + shipTo)
 *   placedAt     — pre-formatted date string from the caller
 *   fmt          — java.util.function.Function<BigDecimal,String> ("0.00" formatter)
 *   orderUrl     — absolute receipt link (e.g. https://celtechgs.com/checkout/confirmation/{id})
 *
 * NOTE: the Celtech text-logo URL is hardcoded below. If the site domain
 * changes or the asset moves, update LINE 25.
 */

yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
    head {
        meta('http-equiv': 'Content-Type', content: 'text/html; charset=UTF-8')
        title("Order #${order.orderNumber} Received")
    }
    /* Body — light brown base (#faf6f3 = --color-bg-soft / --color-brown-cream),
     *         body text in brand brown (#5a3a1e = --color-brown-deep). */
    body(style: 'margin:0; padding:24px; background:#faf6f3; ' +
            'font-family: Arial, sans-serif; color:#5a3a1e;') {

        // Outer container — white card with brown-soft border (#d7ccc8).
        // 14px radius (≈ --radius-xl) + light shadow for the floating-receipt
        // feel. overflow:hidden so the rounded corners clip child rows.
        table(width: '100%', cellpadding: '0', cellspacing: '0', border: '0',
                style: 'max-width:640px; margin:0 auto; background:#ffffff; ' +
                        'border:1px solid #d7ccc8; border-radius:14px; ' +
                        'overflow:hidden;') {

            /* ---- Logo header strip ----------------------------------------
             * Centered Celtech text logo on a soft cream background. No
             * dark green hero — the email stays light to feel like a
             * receipt rather than a celebration. */
            tr {
                td(align: 'center',
                        style: 'padding: 28px 28px 18px; background:#33691e; ' +
                                'border-bottom: 1px solid #d7ccc8;') {
                    img(src: 'https://cgsweb-img.nyc3.cdn.digitaloceanspaces.com/site-images/Celtech%20Text%20Logo%20Middle.png',
                            alt: 'Celtech General Store',
                            width: '240',
                            style: 'display:block; max-width:240px; width:100%; ' +
                                    'height:auto; margin:0 auto;')
                }
            }

            /* ---- Thank-you + order info ----------------------------------- */
            tr {
                td(align: 'center',
                        style: 'padding: 22px 28px 14px; background:#ffffff;') {
                    h1(style: 'margin:0; color:#33691e; font-size:22px; ' +
                            'font-weight:bold;',
                            "Thank you, ${order.customer?.firstName ?: 'friend'}!")
                    p(style: 'margin:8px 0 0; color:#5a3a1e; font-size:15px;') {
                        yield "Your order "
                        strong(style: 'color:#33691e;', "#${order.orderNumber}")
                        yield " was placed on ${placedAt}."
                    }
                }
            }

            /* ---- Items, totals, deliver-to, instructions ------------------ */
            tr {
                td(style: 'padding: 24px 28px;') {

                    /* Items table */
                    h2(style: 'margin:0 0 10px; padding-bottom:6px; ' +
                            'border-bottom: 2px solid #e8f5e9; ' +
                            'font-size:16px; color:#33691e;', 'Items')
                    table(width: '100%', cellpadding: '0', cellspacing: '0',
                            style: 'border-collapse: collapse;') {
                        order.items.each { item ->
                            tr {
                                td(style: 'padding: 10px 0; ' +
                                        'border-bottom: 1px solid #faf6f3; ' +
                                        'color:#5a3a1e;') {
                                    strong(style: 'color:#5a3a1e;', item.name)
                                    br()
                                    span(style: 'color:#7a6757; font-size:13px;',
                                            "${item.vendorName ?: ''} \u00b7 Qty ${item.quantity}")
                                }
                                td(align: 'right',
                                        style: 'padding: 10px 0; ' +
                                                'border-bottom: 1px solid #faf6f3; ' +
                                                'color:#5a3a1e; font-weight:bold; ' +
                                                'font-variant-numeric: tabular-nums; ' +
                                                'white-space:nowrap;',
                                        "\$${fmt.apply(item.lineTotal)}")
                            }
                        }
                    }

                    /* Totals table */
                    h2(style: 'margin:22px 0 10px; padding-bottom:6px; ' +
                            'border-bottom: 2px solid #e8f5e9; ' +
                            'font-size:16px; color:#33691e;', 'Totals')
                    table(width: '100%', cellpadding: '0', cellspacing: '0',
                            style: 'border-collapse: collapse;') {
                        tr {
                            td(style: 'padding:4px 0; color:#5a3a1e;', 'Subtotal')
                            td(align: 'right',
                                    style: 'padding:4px 0; color:#5a3a1e; ' +
                                            'font-variant-numeric: tabular-nums;',
                                    "\$${fmt.apply(order.totals?.subtotal)}")
                        }
                        tr {
                            td(style: 'padding:4px 0; color:#5a3a1e;', 'Shipping')
                            td(align: 'right',
                                    style: 'padding:4px 0; color:#5a3a1e; ' +
                                            'font-variant-numeric: tabular-nums;',
                                    "\$${fmt.apply(order.totals?.shipping)}")
                        }
                        tr {
                            td(style: 'padding:4px 0; color:#5a3a1e;', 'Estimated Tax')
                            td(align: 'right',
                                    style: 'padding:4px 0; color:#5a3a1e; ' +
                                            'font-variant-numeric: tabular-nums;',
                                    "\$${fmt.apply(order.totals?.tax)}")
                        }
                        tr {
                            td(style: 'padding-top:8px; margin-top:4px; ' +
                                    'border-top: 1px dashed #d7ccc8; ' +
                                    'color:#33691e; font-weight:bold; font-size:16px;',
                                    'Total')
                            td(align: 'right',
                                    style: 'padding-top:8px; ' +
                                            'border-top: 1px dashed #d7ccc8; ' +
                                            'color:#33691e; font-weight:bold; ' +
                                            'font-size:16px; ' +
                                            'font-variant-numeric: tabular-nums;',
                                    "\$${fmt.apply(order.totals?.total)}")
                        }
                    }

                    /* Deliver-to */
                    if (order.shipTo) {
                        h2(style: 'margin:22px 0 10px; padding-bottom:6px; ' +
                                'border-bottom: 2px solid #e8f5e9; ' +
                                'font-size:16px; color:#33691e;', 'Delivering to')
                        table(width: '100%', cellpadding: '0', cellspacing: '0',
                                style: 'border-collapse: collapse;') {
                            tr {
                                td(style: 'padding: 12px 14px; background:#faf6f3; ' +
                                        'border:1px solid #d7ccc8; border-radius:8px; ' +
                                        'color:#5a3a1e; line-height:1.5;') {
                                    strong("${order.customer?.firstName ?: ''} " +
                                            "${order.customer?.lastName ?: ''}".trim())
                                    br()
                                    yield order.shipTo.street1 ?: ''
                                    if (order.shipTo.street2) {
                                        br(); yield order.shipTo.street2
                                    }
                                    br()
                                    yield "${order.shipTo.city ?: ''}, ${order.shipTo.state ?: ''} ${order.shipTo.zip ?: ''}"
                                }
                            }
                        }
                    }

                    /* Delivery instructions callout — green-soft tint with
                     * green-bright left border, matches the .confirmation-
                     * instructions block on the site receipt. */
                    if (order.deliveryInstructions) {
                        p(style: 'margin:16px 0 0; padding:12px 14px; ' +
                                'background:#e8f5e9; border-left:3px solid #558b2f; ' +
                                'border-radius:8px; color:#5a3a1e; ' +
                                'font-style: italic; line-height:1.5;') {
                            strong(style: 'color:#33691e;', 'Instructions: ')
                            yield order.deliveryInstructions
                        }
                    }

                    /* CTA — solid green-deep button, white text. Padded for
                     * touch; centered in the row. */
                    p(style: 'margin: 28px 0 0; text-align: center;') {
                        a(href: orderUrl,
                                style: 'display:inline-block; padding: 12px 28px; ' +
                                        'background:#33691e; color:#ffffff; ' +
                                        'text-decoration:none; border-radius:8px; ' +
                                        'font-weight:bold; font-size:15px;',
                                'View your receipt')
                    }
                }
            }

            /* ---- Footer --------------------------------------------------- */
            tr {
                td(align: 'center',
                        style: 'padding: 18px 28px; background:#faf6f3; ' +
                                'border-top:1px solid #d7ccc8; ' +
                                'color:#7a6757; font-size:12px;') {
                    yield 'Questions? Reply to this email \u2014 it goes straight to '
                    a(href: 'mailto:cole@celtechgs.com',
                            style: 'color:#33691e; text-decoration:none;',
                            'cole@celtechgs.com')
                    yield '.'
                    br()
                    span(style: 'color:#a99c8d;', 'Celtech General Store \u00b7 celtechgs.com')
                }
            }
        }
    }
}
