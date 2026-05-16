package templates.admin.vendors

/*
 * Admin vendor detail page.
 *
 * Path: templates/admin/vendors/detail.tpl
 * URL:  /admin/vendors/{id}
 *
 * Bindings:
 *   vendor         — Vendor
 *   assignedUsers  — List<User> currently assigned to this vendor
 *   productCount   — long
 *   activeSection  — "vendors"
 *   message/error  — flash
 *
 * The assign-user modal is plain markup hidden by CSS; admin-user-search.js
 * drives the search box and result list, and submits the assign form.
 */

layout 'layout.tpl',
        title: "CGS Web | Admin · ${vendor.name}",
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
                            div(class: 'admin-breadcrumb') {
                                a(href: '/admin/vendors', '← Back to Vendors')
                            }
                            h1(class: 'admin-page-title', vendor.name)
                            p(class: 'admin-page-subtitle') {
                                if (vendor.active) {
                                    span(class: 'admin-status-pill admin-status-pill-active', 'Active')
                                } else {
                                    span(class: 'admin-status-pill admin-status-pill-disabled', 'Inactive')
                                }
                            }
                        }
                        a(href: "/admin/vendors/${vendor.id}/edit",
                                class: 'admin-btn admin-btn-secondary', 'Edit Vendor')
                    }

                    if (message) div(class: 'admin-alert admin-alert-success', message)
                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    div(class: 'admin-edit-grid') {

                        // ---- LEFT: vendor info + products link ----
                        div {
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Details')
                                dl(class: 'admin-dl') {
                                    dt('Slug')
                                    dd(class: 'admin-mono', vendor.slug ?: '—')
                                    dt('Description')
                                    dd(vendor.description ?: '—')
                                    if (vendor.logoUrl) {
                                        dt('Logo URL')
                                        dd(class: 'admin-mono', vendor.logoUrl)
                                    }
                                    dt('Vendor ID')
                                    dd(class: 'admin-mono', vendor.id)
                                }
                            }

                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Products')
                                p(class: 'admin-card-body',
                                        "This vendor has ${productCount} product${productCount == 1 ? '' : 's'}.")
                                div(class: 'admin-form-actions admin-form-actions-flush') {
                                    a(href: "/admin/products?vendor=${vendor.id}",
                                            class: 'admin-btn admin-btn-primary', 'View Products')
                                    a(href: "/admin/products/new?vendor=${vendor.id}",
                                            class: 'admin-btn admin-btn-secondary', '+ Add Product')
                                }
                            }
                        }

                        // ---- RIGHT: assigned users ----
                        aside(class: 'admin-actions-panel') {
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Managed By')

                                if (!assignedUsers || assignedUsers.isEmpty()) {
                                    p(class: 'admin-card-body admin-card-hint',
                                            'No user accounts assigned to this vendor yet.')
                                } else {
                                    ul(class: 'admin-assigned-list') {
                                        assignedUsers.each { au ->
                                            li(class: 'admin-assigned-item') {
                                                div(class: 'admin-assigned-info') {
                                                    span(class: 'admin-assigned-name',
                                                            au.username ?: '—')
                                                    if (au.email) {
                                                        span(class: 'admin-assigned-email', au.email)
                                                    }
                                                }
                                                form(action: "/admin/vendors/${vendor.id}/unassign-user",
                                                        method: 'post',
                                                        class: 'admin-inline-form') {
                                                    input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                                            value: (csrfToken ?: ''))
                                                    input(type: 'hidden', name: 'userId', value: au.id)
                                                    button(type: 'submit',
                                                            class: 'admin-btn admin-btn-secondary admin-btn-sm',
                                                            onclick: "return confirm('Unassign this user from the vendor? Their vendor role will be removed.');",
                                                            'Unassign')
                                                }
                                            }
                                        }
                                    }
                                }

                                button(type: 'button',
                                        class: 'admin-btn admin-btn-primary admin-btn-block',
                                        id: 'open-assign-modal',
                                        '+ Assign a User')
                            }
                        }
                    }
                }
            }

            // ---- Assign-user modal ----
            // Hidden by default (.admin-modal[hidden]). admin-user-search.js
            // handles open/close, the search call, and result selection.
            div(class: 'admin-modal-backdrop', id: 'assign-modal', hidden: 'hidden') {
                div(class: 'admin-modal') {
                    h3(class: 'admin-modal-title', 'Assign a User to This Vendor')
                    p(class: 'admin-modal-desc',
                            'Search by username or email. A user already assigned to another vendor must be unassigned there first.')

                    div(class: 'admin-modal-search') {
                        input(type: 'text', id: 'user-search-input',
                                class: 'admin-modal-search-input',
                                placeholder: 'Type a name or email…',
                                autocomplete: 'off')
                    }

                    div(class: 'admin-modal-results', id: 'user-search-results') {
                        p(class: 'admin-modal-hint', 'Start typing to search.')
                    }

                    // The actual assign submission. userId is filled in by JS
                    // when a result is picked.
                    form(action: "/admin/vendors/${vendor.id}/assign-user",
                            method: 'post',
                            id: 'assign-user-form') {
                        input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                value: (csrfToken ?: ''))
                        input(type: 'hidden', name: 'userId', id: 'assign-user-id', value: '')
                    }

                    div(class: 'admin-modal-actions') {
                        button(type: 'button', class: 'admin-btn admin-btn-secondary',
                                id: 'close-assign-modal', 'Cancel')
                    }
                }
            }

            script(src: '/scripts/admin-user-search.js') {}
        }
