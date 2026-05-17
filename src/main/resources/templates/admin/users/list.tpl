package templates.admin.users

/*
 * Admin users list view.
 *
 * Path: templates/admin/users/list.tpl
 * URL:  /admin/users (rendered by AdminUserController.list)
 *
 * Bindings:
 *   users           — List<AdminUserListItemDTO> for the current page
 *   page            — zero-based page index
 *   totalPages      — total page count
 *   totalElements   — total row count across all pages
 *   roleFilter      — normalized role filter ("ADMIN"/"VENDOR"/"USER"/null)
 *   activeSection   — "users" (drives sidebar highlight)
 *
 *   message / error — optional flash messages from RedirectAttributes
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · Users',
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
                        h1(class: 'admin-page-title', 'Users')
                        p(class: 'admin-page-subtitle',
                                "${totalElements} total · viewing page ${(page ?: 0) + 1} of ${Math.max(1, totalPages ?: 1)}")
                    }

                    if (message) div(class: 'admin-alert admin-alert-success', message)
                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    // ---- Filter bar ----
                    form(action: '/admin/users', method: 'get', class: 'admin-filter-bar') {
                        label(for: 'role-filter', class: 'admin-filter-label', 'Filter by role:')
                        select(name: 'role', id: 'role-filter', class: 'admin-filter-select',
                                onchange: 'this.form.submit()') {
                            // One attribute map per option — see the note in
                            // products/list.tpl. Two separate maps don't merge.
                            Map allAttrs = [value: '']
                            if (roleFilter == null) allAttrs['selected'] = 'selected'
                            option(allAttrs, 'All roles')

                            Map adminAttrs = [value: 'ADMIN']
                            if (roleFilter == 'ADMIN') adminAttrs['selected'] = 'selected'
                            option(adminAttrs, 'Admins')

                            Map vendorAttrs = [value: 'VENDOR']
                            if (roleFilter == 'VENDOR') vendorAttrs['selected'] = 'selected'
                            option(vendorAttrs, 'Vendors')

                            Map userAttrs = [value: 'USER']
                            if (roleFilter == 'USER') userAttrs['selected'] = 'selected'
                            option(userAttrs, 'Regular users')
                        }
                        // Reset page when filter changes — no `page` param means page=0
                        noscript {
                            button(type: 'submit', class: 'admin-btn admin-btn-secondary', 'Apply')
                        }
                    }

                    // ---- Table ----
                    if (!users || users.isEmpty()) {
                        div(class: 'admin-empty-state',
                                'No users match this filter.')
                    } else {
                        div(class: 'admin-table-wrapper') {
                            table(class: 'admin-table') {
                                thead {
                                    tr {
                                        th('Username')
                                        th('Name')
                                        th('Email')
                                        th('Roles')
                                        th('Status')
                                        th(class: 'admin-table-actions-col', '')
                                    }
                                }
                                tbody {
                                    users.each { u ->
                                        tr(class: u.enabled ? '' : 'admin-table-row-disabled') {
                                            td {
                                                span(class: 'admin-mono', u.username ?: '—')
                                                if (u.isCurrentUser()) {
                                                    span(class: 'admin-self-marker', '(you)')
                                                }
                                            }
                                            td(u.displayName ?: '—')
                                            td(class: 'admin-table-email', u.email ?: '—')
                                            td {
                                                if (u.roles && !u.roles.isEmpty()) {
                                                    u.roles.each { role ->
                                                        span(class: "admin-role-pill admin-role-pill-${role.toLowerCase()}", role)
                                                    }
                                                } else {
                                                    yield '—'
                                                }
                                            }
                                            td {
                                                if (!u.enabled) {
                                                    span(class: 'admin-status-pill admin-status-pill-disabled', 'Disabled')
                                                } else if (!u.emailVerified) {
                                                    span(class: 'admin-status-pill admin-status-pill-unverified', 'Email Unverified')
                                                } else {
                                                    span(class: 'admin-status-pill admin-status-pill-active', 'Active')
                                                }
                                            }
                                            td(class: 'admin-table-actions') {
                                                a(href: "/admin/users/${u.id}",
                                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                        'Edit')
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- Pagination ----
                    if ((totalPages ?: 0) > 1) {
                        div(class: 'admin-pagination') {
                            int currentPage = page ?: 0
                            int totalPg = totalPages ?: 1
                            String filterQuery = roleFilter ? "&role=${roleFilter}" : ''

                            if (currentPage > 0) {
                                a(href: "/admin/users?page=${currentPage - 1}${filterQuery}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                        '← Previous')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled',
                                        '← Previous')
                            }

                            span(class: 'admin-pagination-info',
                                    "Page ${currentPage + 1} of ${totalPg}")

                            if (currentPage < totalPg - 1) {
                                a(href: "/admin/users?page=${currentPage + 1}${filterQuery}",
                                        class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                        'Next →')
                            } else {
                                span(class: 'admin-btn admin-btn-secondary admin-btn-sm admin-btn-disabled',
                                        'Next →')
                            }
                        }
                    }
                }
            }
        }
