package templates.admin.routes

/*
 * Admin route detail.
 *
 * Path: templates/admin/routes/detail.tpl
 * URL:  /admin/routes/{id}
 *
 * Bindings:
 *   route          — Route
 *   stops          — List<AdminRouteStopView> (each joined to delivery + order)
 *   activeSection  — "routes"
 */

layout 'layout.tpl',
        title: "CGS Web | Admin · Route ${route.routeNumber ?: route.id}",
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
                            a(href: '/admin/routes', '← Back to Routes')
                        }
                        h1(class: 'admin-page-title', "Route ${route.routeNumber ?: route.id}") {}
                        p(class: 'admin-page-subtitle') {
                            String st = route.status ? route.status.name() : 'UNKNOWN'
                            span(class: "admin-status-pill admin-route-status-${st.toLowerCase()}",
                                    st.replace('_', ' '))
                        }
                    }

                    div(class: 'admin-edit-grid') {

                        // ---- LEFT: stops ----
                        div {
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Stops')
                                if (!stops || stops.isEmpty()) {
                                    p(class: 'admin-card-body admin-card-hint', 'This route has no stops.')
                                } else {
                                    div(class: 'admin-table-wrapper') {
                                        table(class: 'admin-table') {
                                            thead {
                                                tr {
                                                    th('#')
                                                    th('Customer')
                                                    th('Address')
                                                    th('Delivery status')
                                                    th('Order')
                                                }
                                            }
                                            tbody {
                                                stops.each { s ->
                                                    tr {
                                                        td("${s.sequence}")
                                                        td {
                                                            if (s.deliveryResolved) {
                                                                yield(s.customerName ?: '—')
                                                            } else {
                                                                span(class: 'admin-text-muted',
                                                                        'delivery missing')
                                                            }
                                                        }
                                                        td(s.addressLine ?: '—')
                                                        td {
                                                            if (s.deliveryStatus) {
                                                                String ds = s.deliveryStatus.name()
                                                                span(class: "admin-status-pill admin-delivery-status-${ds.toLowerCase()}", ds)
                                                            } else {
                                                                yield '—'
                                                            }
                                                        }
                                                        td {
                                                            if (s.orderResolved) {
                                                                a(href: "/admin/orders/${s.orderId}",
                                                                        class: 'admin-link-strong',
                                                                        s.orderNumber ?: s.orderId)
                                                            } else {
                                                                span(class: 'admin-text-muted', '—')
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ---- RIGHT: route meta ----
                        aside(class: 'admin-actions-panel') {
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Summary')
                                dl(class: 'admin-dl') {
                                    def t = route.totals
                                    dt('Stops')
                                    dd(t?.stopCount != null ? "${t.stopCount}" : "${stops ? stops.size() : 0}")
                                    dt('Distance')
                                    dd(t?.distanceMeters != null
                                            ? "${(t.distanceMeters / 1000.0).round(1)} km" : '—')
                                    dt('Duration')
                                    dd(t?.durationSeconds != null
                                            ? "${Math.round(t.durationSeconds / 60.0)} min" : '—')
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Timeline')
                                dl(class: 'admin-dl') {
                                    dt('Created');   dd(route.createdAt?.toString() ?: '—')
                                    dt('Updated');   dd(route.updatedAt?.toString() ?: '—')
                                    if (route.startedAt)   { dt('Started');   dd(route.startedAt.toString()) }
                                    if (route.completedAt) { dt('Completed'); dd(route.completedAt.toString()) }
                                }
                            }

                            if (route.optimization) {
                                section(class: 'admin-card') {
                                    h2(class: 'admin-card-title', 'Optimization')
                                    dl(class: 'admin-dl') {
                                        def opt = route.optimization
                                        dt('Optimizer'); dd(opt.optimizerName ?: '—')
                                        dt('Geometry');  dd(opt.geometryProvider ?: '—')
                                        if (opt.optimizedAt)  { dt('Optimized at'); dd(opt.optimizedAt.toString()) }
                                        if (opt.elapsedMillis != null) { dt('Elapsed'); dd("${opt.elapsedMillis} ms") }
                                        if (opt.optimizerVersion) { dt('Version'); dd(opt.optimizerVersion) }
                                    }
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Identity')
                                dl(class: 'admin-dl') {
                                    dt('Route ID'); dd(class: 'admin-mono', route.id)
                                }
                            }
                        }
                    }
                }
            }
        }
