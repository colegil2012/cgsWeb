package templates.admin.orders

/*
 * Admin orders list.
 *
 * Path: templates/admin/orders/list.tpl
 * URL:  /admin/orders  (optionally ?status=...&q=...&page=...)
 *
 * Bindings:
 *   orders         — List<AdminOrderListItemDTO>
 *   page, totalPages, totalElements
 *   statusFilter   — normalized OrderStatus name, or null
 *   search         — current search term, or null
 *   activeSection  — "orders"
 *   error          — flash
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · Orders',
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
                        h1(class: 'admin-page-title', 'Orders')
                        p(class: 'admin-page-subtitle', "${totalElements} total")
                    }

                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    // ---- Filter + search bar (single GET form) ----
                    form(action: '/admin/orders', method: 'get', class: 'admin-filter-bar') {
                        label(for: 'status-filter', class: 'admin-filter-label', 'Status:')
                        select(name: 'status', id: 'status-filter', class: 'admin-filter-select') {
                            // Single attribute map per option — never two maps.
                            def statuses = ['PENDING', 'PAID', 'CANCELLED', 'REFUNDED', 'DELIVERED']
                            Map allAttrs = [value: '']
                            if (statusFilter == null) allAttrs['selected'] = 'selected'
                            option(allAttrs, 'All statuses')
                            statuses.each { s ->
                                Map sAttrs = [value: s]
                                if (statusFilter == s) sAttrs['selected'] = 'selected'
                                option(sAttrs, s)
                            }
                        }

                        input(type: 'search', name: 'q', class: 'admin-filter-search',
                                placeholder: 'Order # or customer name…',
                                value: search ?: '')

                        button(type: 'submit', class: 'admin-btn admin-btn-secondary', 'Apply')
                        if (statusFilter != null || search != null) {
                            a(href: '/admin/orders',
                                    class: 'admin-btn admin-btn-secondary', 'Clear')
                        }
                    }

                    if (!orders || orders.isEmpty()) {
                        div(class: 'admin-empty-state', 'No orders match.')
                    } else {
                        div(class: 'admin-table-wrapper') {
                            table(class: 'admin-table') {
                                thead {
                                    tr {
                                        th('Order #')
                                        th('Customer')
                                        th('Status')
                                        th('Items')
                                        th('Total')
                                        th('Placed')
                                        th(class: 'admin-table-actions-col', '')
                                    }
                                }
                                tbody {
                                    orders.each { o ->
                                        tr {
                                            td {
                                                a(href: "/admin/orders/${o.id}",
                                                        class: 'admin-link-strong',
                                                        o.orderNumber ?: o.id)
                                            }
                                            td(o.customerName ?: '—')
                                            td {
                                                String st = o.status ? o.status.name() : 'UNKNOWN'
                                                span(class: "admin-status-pill admin-order-status-${st.toLowerCase()}", st)
                                            }
                                            td("${o.itemCount}")
                                            td(o.total != null ? "\$${o.total}" : '—')
                                            td(o.placedAt != null ? o.placedAt.toString() : '—')
                                            td(class: 'admin-table-actions') {
                                                a(href: "/admin/orders/${o.id}",
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
                            String qs = ''
                            if (statusFilter != null) qs += "&status=${statusFilter}"
                            if (search != null) qs += "&q=${search}"
                            if (currentPage > 0) {
                                a(href: "/admin/orders?page=${currentPage - 1}${qs}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', '← Previous')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', '← Previous')
                            }
                            span(class: 'admin-pagination-info', "Page ${currentPage + 1} of ${totalPg}")
                            if (currentPage < totalPg - 1) {
                                a(href: "/admin/orders?page=${currentPage + 1}${qs}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', 'Next →')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', 'Next →')
                            }
                        }
                    }
                }
            }
        }
