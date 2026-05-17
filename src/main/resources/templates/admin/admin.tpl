package templates.admin

import com.ua.estore.cgsWeb.util.ImageUrlUtil

/*
 * Admin home / dashboard.
 *
 * Path: templates/admin/admin.tpl
 * URL:  /admin (rendered by AdminController.dashboard)
 *
 * Layout shell:
 *   - Outer storefront layout (so the storefront header + footer + Manage
 *     dropdown remain available — the admin doesn't lose context)
 *   - Inside .admin-shell: the admin sidebar (via the partial) on the left,
 *     .admin-content on the right
 *
 * Each admin page renders the same outer shell. The unique content goes in
 * the .admin-content area.
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin',
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
                                        h1(class: 'admin-page-title', 'Dashboard')
                                        p(class: 'admin-page-subtitle',
                                                'Manage users, vendors, routes, and orders from one place.')
                                }

                                div(class: 'admin-dashboard-grid') {
                                        a(href: '/admin/users', class: 'admin-dashboard-tile') {
                                                span(class: 'admin-dashboard-tile-label', 'Users')
                                                p(class: 'admin-dashboard-tile-desc',
                                                        'View and edit accounts, manage roles, send password resets.')
                                                span(class: 'admin-dashboard-tile-cta', 'Manage Users →')
                                        }
                                        a(href: '/admin/vendors', class: 'admin-dashboard-tile admin-dashboard-tile') {
                                                span(class: 'admin-dashboard-tile-label', 'Vendors')
                                                p(class: 'admin-dashboard-tile-desc',
                                                        'Add vendors, assign user accounts, manage product catalogs and inventory.')
                                                span(class: 'admin-dashboard-tile-cta', 'Manage Vendors →')
                                        }
                                        a(href: '/admin/routes', class: 'admin-dashboard-tile admin-dashboard-tile') {
                                                span(class: 'admin-dashboard-tile-label', 'Routes')
                                                p(class: 'admin-dashboard-tile-desc',
                                                        'Browse planned, in-progress, and completed delivery routes.')
                                                span(class: 'admin-dashboard-tile-cta', 'Manage Routes →')
                                        }
                                        a(href: '/admin/orders', class: 'admin-dashboard-tile admin-dashboard-tile') {
                                                span(class: 'admin-dashboard-tile-label', 'Orders')
                                                p(class: 'admin-dashboard-tile-desc',
                                                        'Filter orders by status, drill into line item detail.')
                                                span(class: 'admin-dashboard-tile-cta', 'Manage Orders →')
                                        }
                                }
                        }
                }
        }
