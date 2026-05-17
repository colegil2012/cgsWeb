package templates.admin.routes

/*
 * Admin routes list.
 *
 * Path: templates/admin/routes/list.tpl
 * URL:  /admin/routes  (optionally ?status=...&page=...)
 *
 * Bindings:
 *   routes         — List<AdminRouteListItemDTO>
 *   page, totalPages, totalElements
 *   statusFilter   — normalized RouteStatus name, or null
 *   activeSection  — "routes"
 *   error          — flash
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · Routes',
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
                        h1(class: 'admin-page-title', 'Routes')
                        p(class: 'admin-page-subtitle', "${totalElements} total")
                    }

                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    form(action: '/admin/routes', method: 'get', class: 'admin-filter-bar') {
                        label(for: 'status-filter', class: 'admin-filter-label', 'Status:')
                        select(name: 'status', id: 'status-filter', class: 'admin-filter-select',
                                onchange: 'this.form.submit()') {
                            def statuses = ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
                            Map allAttrs = [value: '']
                            if (statusFilter == null) allAttrs['selected'] = 'selected'
                            option(allAttrs, 'All statuses')
                            statuses.each { s ->
                                Map sAttrs = [value: s]
                                if (statusFilter == s) sAttrs['selected'] = 'selected'
                                option(sAttrs, s.replace('_', ' '))
                            }
                        }
                        noscript {
                            button(type: 'submit', class: 'admin-btn admin-btn-secondary', 'Apply')
                        }
                    }

                    if (!routes || routes.isEmpty()) {
                        div(class: 'admin-empty-state', 'No routes match.')
                    } else {
                        div(class: 'admin-table-wrapper') {
                            table(class: 'admin-table') {
                                thead {
                                    tr {
                                        th('Route #')
                                        th('Status')
                                        th('Stops')
                                        th('Distance')
                                        th('Duration')
                                        th('Created')
                                        th(class: 'admin-table-actions-col', '')
                                    }
                                }
                                tbody {
                                    routes.each { r ->
                                        tr {
                                            td {
                                                a(href: "/admin/routes/${r.id}",
                                                        class: 'admin-link-strong',
                                                        r.routeNumber ?: r.id)
                                            }
                                            td {
                                                String st = r.status ? r.status.name() : 'UNKNOWN'
                                                span(class: "admin-status-pill admin-route-status-${st.toLowerCase()}",
                                                        st.replace('_', ' '))
                                            }
                                            td("${r.stopCount}")
                                            td {
                                                if (r.distanceMeters != null) {
                                                    yield "${(r.distanceMeters / 1000.0).round(1)} km"
                                                } else { yield '—' }
                                            }
                                            td {
                                                if (r.durationSeconds != null) {
                                                    yield "${Math.round(r.durationSeconds / 60.0)} min"
                                                } else { yield '—' }
                                            }
                                            td(r.createdAt != null ? r.createdAt.toString() : '—')
                                            td(class: 'admin-table-actions') {
                                                a(href: "/admin/routes/${r.id}",
                                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                        'View')
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ((totalPages ?: 0) > 1) {
                        div(class: 'admin-pagination') {
                            int currentPage = page ?: 0
                            int totalPg = totalPages ?: 1
                            String qs = statusFilter != null ? "&status=${statusFilter}" : ''
                            if (currentPage > 0) {
                                a(href: "/admin/routes?page=${currentPage - 1}${qs}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', '← Previous')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', '← Previous')
                            }
                            span(class: 'admin-pagination-info', "Page ${currentPage + 1} of ${totalPg}")
                            if (currentPage < totalPg - 1) {
                                a(href: "/admin/routes?page=${currentPage + 1}${qs}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', 'Next →')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', 'Next →')
                            }
                        }
                    }
                }
            }
        }
