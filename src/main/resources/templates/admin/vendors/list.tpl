package templates.admin.vendors

/*
 * Admin vendors list.
 *
 * Path: templates/admin/vendors/list.tpl
 * URL:  /admin/vendors
 *
 * Bindings:
 *   vendors        — List<AdminVendorListItemDTO>
 *   page, totalPages, totalElements
 *   activeSection  — "vendors"
 *   message/error  — flash
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · Vendors',
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
                    header(class: 'admin-page-header admin-page-header-withaction') {
                        div {
                            h1(class: 'admin-page-title', 'Vendors')
                            p(class: 'admin-page-subtitle',
                                    "${totalElements} total")
                        }
                        a(href: '/admin/vendors/new',
                                class: 'admin-btn admin-btn-primary', '+ Add Vendor')
                    }

                    if (message) div(class: 'admin-alert admin-alert-success', message)
                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    if (!vendors || vendors.isEmpty()) {
                        div(class: 'admin-empty-state') {
                            yield 'No vendors yet. '
                            a(href: '/admin/vendors/new', 'Add the first one.')
                        }
                    } else {
                        div(class: 'admin-table-wrapper') {
                            table(class: 'admin-table') {
                                thead {
                                    tr {
                                        th('Name')
                                        th('Slug')
                                        th('Products')
                                        th('Status')
                                        th(class: 'admin-table-actions-col', '')
                                    }
                                }
                                tbody {
                                    vendors.each { v ->
                                        tr(class: v.active ? '' : 'admin-table-row-disabled') {
                                            td {
                                                a(href: "/admin/vendors/${v.id}",
                                                        class: 'admin-link-strong', v.name ?: '—')
                                            }
                                            td { span(class: 'admin-mono', v.slug ?: '—') }
                                            td {
                                                a(href: "/admin/products?vendor=${v.id}",
                                                        class: 'admin-link',
                                                        "${v.productCount} product${v.productCount == 1 ? '' : 's'}")
                                            }
                                            td {
                                                if (v.active) {
                                                    span(class: 'admin-status-pill admin-status-pill-active', 'Active')
                                                } else {
                                                    span(class: 'admin-status-pill admin-status-pill-disabled', 'Inactive')
                                                }
                                            }
                                            td(class: 'admin-table-actions') {
                                                a(href: "/admin/vendors/${v.id}",
                                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                        'Manage')
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
                            if (currentPage > 0) {
                                a(href: "/admin/vendors?page=${currentPage - 1}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', '← Previous')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', '← Previous')
                            }
                            span(class: 'admin-pagination-info', "Page ${currentPage + 1} of ${totalPg}")
                            if (currentPage < totalPg - 1) {
                                a(href: "/admin/vendors?page=${currentPage + 1}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', 'Next →')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', 'Next →')
                            }
                        }
                    }
                }
            }
        }
