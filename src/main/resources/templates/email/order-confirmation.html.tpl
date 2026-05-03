yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
    head {
        meta('http-equiv': 'Content-Type', content: 'text/html; charset=UTF-8')
        title("Order #${order.orderNumber} Received")
    }
    body(style: 'margin:0; padding:24px; background:#f7f3ec; font-family: Arial, sans-serif; color:#5d4037;') {

        // Container — hard-coded styles because email clients ignore most CSS.
        table(width: '100%', cellpadding: '0', cellspacing: '0', border: '0',
                style: 'max-width:640px; margin:0 auto; background:#ffffff; ' +
                        'border:1px solid #d7c4a3; border-radius:14px; overflow:hidden;') {
            tr {
                td(style: 'padding: 24px 28px; background: linear-gradient(135deg, #c8e6c9, #a5d6a7);') {
                    h1(style: 'margin:0; color:#2e7d32; font-size:22px;',
                            "Thank you, ${order.customer?.firstName ?: 'friend'}!")
                    p(style: 'margin:6px 0 0; color:#5d4037;') {
                        yield "Your order "
                        strong("#${order.orderNumber}")
                        yield " was placed on ${placedAt}."
                    }
                }
            }
            tr {
                td(style: 'padding: 24px 28px;') {

                    h2(style: 'margin:0 0 8px; font-size:16px; color:#2e7d32;', 'Items')
                    table(width: '100%', cellpadding: '0', cellspacing: '0',
                            style: 'border-collapse: collapse;') {
                        order.items.each { item ->
                            tr {
                                td(style: 'padding: 8px 0; border-bottom: 1px solid #efe6d2;') {
                                    strong(item.name)
                                    br()
                                    span(style: 'color:#7a6757; font-size:13px;',
                                            "${item.vendorName ?: ''} · Qty ${item.quantity}")
                                }
                                td(align: 'right',
                                        style: 'padding: 8px 0; border-bottom: 1px solid #efe6d2; ' +
                                                'font-variant-numeric: tabular-nums; white-space:nowrap;',
                                        "\$${fmt.apply(item.lineTotal)}")
                            }
                        }
                    }

                    h2(style: 'margin:18px 0 8px; font-size:16px; color:#2e7d32;', 'Totals')
                    table(width: '100%', cellpadding: '0', cellspacing: '0') {
                        tr {
                            td('Subtotal')
                            td(align: 'right', "\$${fmt.apply(order.totals?.subtotal)}")
                        }
                        tr {
                            td('Shipping')
                            td(align: 'right', "\$${fmt.apply(order.totals?.shipping)}")
                        }
                        tr {
                            td('Estimated Tax')
                            td(align: 'right', "\$${fmt.apply(order.totals?.tax)}")
                        }
                        tr {
                            td(style: 'padding-top:6px; border-top: 1px dashed #d7c4a3; font-weight:bold;', 'Total')
                            td(align: 'right',
                                    style: 'padding-top:6px; border-top: 1px dashed #d7c4a3; ' +
                                            'font-weight:bold; color:#2e7d32;',
                                    "\$${fmt.apply(order.totals?.total)}")
                        }
                    }

                    if (order.shipTo) {
                        h2(style: 'margin:18px 0 8px; font-size:16px; color:#2e7d32;', 'Delivering to')
                        p(style: 'margin:0;') {
                            strong("${order.customer?.firstName ?: ''} ${order.customer?.lastName ?: ''}".trim())
                            br()
                            yield order.shipTo.street1 ?: ''
                            if (order.shipTo.street2) { br(); yield order.shipTo.street2 }
                            br()
                            yield "${order.shipTo.city ?: ''}, ${order.shipTo.state ?: ''} ${order.shipTo.zip ?: ''}"
                        }
                    }

                    if (order.deliveryInstructions) {
                        p(style: 'margin:14px 0 0; padding:12px 14px; ' +
                                'background:#f3eedb; border-left:3px solid #689f38; ' +
                                'border-radius:8px; font-style: italic;') {
                            strong('Instructions: ')
                            yield order.deliveryInstructions
                        }
                    }

                    p(style: 'margin: 24px 0 0; text-align: center;') {
                        a(href: orderUrl,
                                style: 'display:inline-block; padding: 10px 20px; ' +
                                        'background:#2e7d32; color:#ffffff; text-decoration:none; ' +
                                        'border-radius:8px; font-weight:bold;',
                                'View your receipt')
                    }
                }
            }
            tr {
                td(style: 'padding: 16px 28px; background:#f7f3ec; color:#7a6757; font-size:12px; text-align:center;') {
                    yield 'Questions? Reply to this email — it goes straight to '
                    a(href: 'mailto:cole@celtechgs.com', 'cole@celtechgs.com')
                    yield '.'
                }
            }
        }
    }
}