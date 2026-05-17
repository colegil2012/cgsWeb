package templates.admin.orders

/*
 * Admin order detail.
 *
 * Path: templates/admin/orders/detail.tpl
 * URL:  /admin/orders/{id}
 *
 * Bindings:
 *   order          — Order
 *   delivery       — Delivery or null (lazy-created; may not exist)
 *   route          — Route or null (the order's current/most-recent route)
 *   activeSection  — "orders"
 */

layout 'layout.tpl',
        title: "CGS Web | Admin · Order ${order.orderNumber ?: order.id}",
        user: user,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/admin.css')
            link(rel: 'stylesheet', href: '/css/components/admin-table.css')
        },
        content: {
            div(class: 'admin-shell') {
                include template: 'partials/admin-sidebar.tpl'

                section(class: 'admin-content') {
                    header(class: 'admin-page-header') {
                        div(class: 'admin-breadcrumb') {
                            a(href: '/admin/orders', '← Back to Orders')
                        }
                        h1(class: 'admin-page-title', "Order ${order.orderNumber ?: order.id}") {}
                        p(class: 'admin-page-subtitle') {
                            String st = order.status ? order.status.name() : 'UNKNOWN'
                            span(class: "admin-status-pill admin-order-status-${st.toLowerCase()}", st)
                        }
                    }

                    div(class: 'admin-edit-grid') {

                        // ---- LEFT: items + totals ----
                        div {
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Line Items')
                                if (!order.items || order.items.isEmpty()) {
                                    p(class: 'admin-card-body admin-card-hint', 'No items on this order.')
                                } else {
                                    div(class: 'admin-table-wrapper') {
                                        table(class: 'admin-table') {
                                            thead {
                                                tr {
                                                    th('Product')
                                                    th('Vendor')
                                                    th('Qty')
                                                    th('Unit Price')
                                                    th('Line Total')
                                                }
                                            }
                                            tbody {
                                                order.items.each { item ->
                                                    tr {
                                                        td {
                                                            span(class: 'admin-link-strong', item.name ?: '—')
                                                            if (item.sku) {
                                                                br()
                                                                span(class: 'admin-mono admin-text-tiny', "SKU ${item.sku}")
                                                            }
                                                            if (item.itemNote) {
                                                                br()
                                                                span(class: 'admin-text-tiny', "Note: ${item.itemNote}")
                                                            }
                                                        }
                                                        td(item.vendorName ?: '—')
                                                        td(item.quantity != null ? "${item.quantity}" : '—')
                                                        td(item.priceAtPurchase != null ? "\$${item.priceAtPurchase}" : '—')
                                                        td(item.lineTotal != null ? "\$${item.lineTotal}" : '—')
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Totals')
                                dl(class: 'admin-dl admin-dl-row') {
                                    def t = order.totals
                                    dt('Subtotal'); dd(t?.subtotal != null ? "\$${t.subtotal}" : '—')
                                    dt('Tax');      dd(t?.tax != null ? "\$${t.tax}" : '—')
                                    dt('Shipping'); dd(t?.shipping != null ? "\$${t.shipping}" : '—')
                                    dt('Discount'); dd(t?.discount != null ? "\$${t.discount}" : '—')
                                    dt('Total');    dd(t?.total != null ? "\$${t.total}" : '—')
                                }
                            }
                        }

                        // ---- RIGHT: customer, ship-to, route link, timeline ----
                        aside(class: 'admin-actions-panel') {

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Customer')
                                def c = order.customer
                                if (c == null) {
                                    p(class: 'admin-card-body admin-card-hint', 'No customer snapshot.')
                                } else {
                                    dl(class: 'admin-dl') {
                                        dt('Name')
                                        dd(((c.firstName ?: '') + ' ' + (c.lastName ?: '')).trim() ?: '—')
                                        dt('Email'); dd(c.email ?: '—')
                                        dt('Phone'); dd(c.phone ?: '—')
                                    }
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Ship To')
                                def s = order.shipTo
                                if (s == null) {
                                    p(class: 'admin-card-body admin-card-hint', 'No shipping address.')
                                } else {
                                    p(class: 'admin-card-body') {
                                        yield(s.street1 ?: '')
                                        if (s.street2) { br(); yield(s.street2) }
                                        br()
                                        yield(((s.city ?: '') + ', ' + (s.state ?: '') + ' ' + (s.zip ?: '')).trim())
                                    }
                                }
                                if (order.deliveryInstructions) {
                                    p(class: 'admin-card-hint', "Instructions: ${order.deliveryInstructions}")
                                }
                            }

                            // ---- Cross-link: the route this order is/was on ----
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Delivery & Route')
                                if (delivery == null) {
                                    p(class: 'admin-card-body admin-card-hint',
                                            'No delivery record yet for this order.')
                                } else {
                                    dl(class: 'admin-dl') {
                                        dt('Delivery status')
                                        dd {
                                            String ds = delivery.status ? delivery.status.name() : 'UNKNOWN'
                                            span(class: "admin-status-pill admin-delivery-status-${ds.toLowerCase()}", ds)
                                        }
                                    }
                                    if (route != null) {
                                        p(class: 'admin-card-body') {
                                            yield 'On route '
                                            a(href: "/admin/routes/${route.id}",
                                                    class: 'admin-link-strong',
                                                    route.routeNumber ?: route.id)
                                        }
                                    } else {
                                        p(class: 'admin-card-body admin-card-hint',
                                                'Not currently linked to a route.')
                                    }
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Timeline')
                                dl(class: 'admin-dl') {
                                    dt('Placed');    dd(order.placedAt?.toString() ?: '—')
                                    dt('Updated');   dd(order.updatedAt?.toString() ?: '—')
                                    if (order.deliveredAt) { dt('Delivered'); dd(order.deliveredAt.toString()) }
                                    if (order.cancelledAt) { dt('Cancelled'); dd(order.cancelledAt.toString()) }
                                    if (order.refundedAt)  { dt('Refunded');  dd(order.refundedAt.toString()) }
                                }
                            }
                        }
                    }
                }
            }
        }
