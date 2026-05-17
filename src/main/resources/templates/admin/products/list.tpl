package templates.admin.products

/*
 * Admin products list.
 *
 * Path: templates/admin/products/list.tpl
 * URL:  /admin/products  (optionally ?vendor=<id>)
 *
 * Bindings:
 *   products         — List<AdminProductListItemDTO>
 *   page, totalPages, totalElements
 *   vendorFilter     — vendor id if scoped, else null
 *   filterVendorName — vendor name if scoped
 *   allVendors       — List<Vendor> (id+name) for the filter dropdown
 *   activeSection    — "products"
 *   message/error    — flash
 *
 * Shows all products regardless of stock — the admin needs to see 0-stock
 * items to restock them. Each row has an inline "set stock" form.
 */

String currentListUrl = vendorFilter ? "/admin/products?vendor=${vendorFilter}" : '/admin/products'

layout 'layout.tpl',
        title: 'CGS Web | Admin · Products',
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
                            h1(class: 'admin-page-title',
                                    filterVendorName ? "Products · ${filterVendorName}" : 'Products')
                            p(class: 'admin-page-subtitle', "${totalElements} total")
                        }
                        a(href: vendorFilter
                                ? "/admin/products/new?vendor=${vendorFilter}"
                                : '/admin/products/new',
                                class: 'admin-btn admin-btn-primary', '+ Add Product')
                    }

                    if (message) div(class: 'admin-alert admin-alert-success', message)
                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    // ---- Vendor filter ----
                    form(action: '/admin/products', method: 'get', class: 'admin-filter-bar') {
                        label(for: 'vendor-filter', class: 'admin-filter-label', 'Filter by vendor:')
                        select(name: 'vendor', id: 'vendor-filter', class: 'admin-filter-select',
                                onchange: 'this.form.submit()') {
                            // Build ONE attribute map per option. Passing two
                            // separate maps (value:... , then a conditional
                            // [selected:...] map) does NOT merge into the
                            // element's attributes in MarkupTemplateEngine —
                            // it mis-binds and the value attribute is lost.
                            Map allAttrs = [value: '']
                            if (vendorFilter == null) allAttrs['selected'] = 'selected'
                            option(allAttrs, 'All vendors')

                            allVendors.each { v ->
                                Map vAttrs = [value: v.id]
                                if (vendorFilter == v.id) vAttrs['selected'] = 'selected'
                                option(vAttrs, v.name ?: v.id)
                            }
                        }
                        noscript {
                            button(type: 'submit', class: 'admin-btn admin-btn-secondary', 'Apply')
                        }
                    }

                    if (!products || products.isEmpty()) {
                        div(class: 'admin-empty-state') {
                            yield 'No products found. '
                            a(href: vendorFilter
                                    ? "/admin/products/new?vendor=${vendorFilter}"
                                    : '/admin/products/new',
                                    'Add one.')
                        }
                    } else {
                        div(class: 'admin-table-wrapper') {
                            table(class: 'admin-table') {
                                thead {
                                    tr {
                                        th('Product')
                                        th('SKU')
                                        th('Vendor')
                                        th('Price')
                                        th('Stock')
                                        th('Storefront')
                                        th(class: 'admin-table-actions-col', '')
                                    }
                                }
                                tbody {
                                    products.each { p ->
                                        tr {
                                            td {
                                                a(href: "/admin/products/${p.id}",
                                                        class: 'admin-link-strong', p.name ?: '—')
                                            }
                                            td { span(class: 'admin-mono', p.sku ?: '—') }
                                            td(p.vendorName ?: '—')
                                            td(p.price != null ? "\$${p.price}" : '—')
                                            td {
                                                // Inline set-stock form. Posts to the
                                                // set-stock action; returnTo brings us
                                                // back to this list (filter preserved).
                                                form(action: "/admin/products/${p.id}/set-stock",
                                                        method: 'post',
                                                        class: 'admin-stock-form') {
                                                    input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                                            value: (csrfToken ?: ''))
                                                    input(type: 'hidden', name: 'returnTo',
                                                            value: currentListUrl)
                                                    input(type: 'number', name: 'stock', min: '0',
                                                            class: 'admin-stock-input',
                                                            value: p.stock != null ? p.stock : 0)
                                                    button(type: 'submit',
                                                            class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                            'Set')
                                                }
                                            }
                                            td {
                                                // Storefront visibility = active AND stock>0.
                                                boolean visible = p.active && p.stock != null && p.stock > 0
                                                if (visible) {
                                                    span(class: 'admin-status-pill admin-status-pill-active', 'Visible')
                                                } else if (!p.active) {
                                                    span(class: 'admin-status-pill admin-status-pill-disabled', 'Inactive')
                                                } else {
                                                    span(class: 'admin-status-pill admin-status-pill-unverified', 'Out of stock')
                                                }
                                            }
                                            td(class: 'admin-table-actions') {
                                                a(href: "/admin/products/${p.id}",
                                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                        'Edit')
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
                            String vq = vendorFilter ? "&vendor=${vendorFilter}" : ''
                            if (currentPage > 0) {
                                a(href: "/admin/products?page=${currentPage - 1}${vq}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', '← Previous')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', '← Previous')
                            }
                            span(class: 'admin-pagination-info', "Page ${currentPage + 1} of ${totalPg}")
                            if (currentPage < totalPg - 1) {
                                a(href: "/admin/products?page=${currentPage + 1}${vq}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm', 'Next →')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled', 'Next →')
                            }
                        }
                    }
                }
            }
        }
