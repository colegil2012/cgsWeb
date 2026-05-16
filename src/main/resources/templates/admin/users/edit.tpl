package templates.admin.users

/*
 * Admin edit-user view.
 *
 * Path: templates/admin/users/edit.tpl
 * URL:  /admin/users/{id} (rendered by AdminUserController.editForm)
 *
 * Bindings:
 *   targetUser       — User being edited
 *   isSelf           — boolean: true when viewing the admin's own row
 *   canAssignVendor  — Round 1A: false unless the user already has VENDOR.
 *                      VENDOR role is shown but only addable via the Vendors
 *                      section (Round 1B).
 *   activeSection    — "users"
 *   message / error  — flash messages
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · Edit User',
        user: user,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/admin.css')
        },
        content: {
            div(class: 'admin-shell') {
                include template: 'partials/admin-sidebar.tpl'

                section(class: 'admin-content') {
                    header(class: 'admin-page-header') {
                        div(class: 'admin-breadcrumb') {
                            a(href: '/admin/users', '← Back to Users')
                        }
                        h1(class: 'admin-page-title', "Edit: ${targetUser.username}")
                        if (isSelf) {
                            p(class: 'admin-page-subtitle admin-self-notice',
                                    "This is your own account — some actions are disabled.")
                        }
                    }

                    if (message) div(class: 'admin-alert admin-alert-success', message)
                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    div(class: 'admin-edit-grid') {

                        // ============================================================
                        // LEFT: profile + roles form
                        // ============================================================
                        section(class: 'admin-card admin-card-form') {
                            h2(class: 'admin-card-title', 'Profile & Roles')

                            form(action: "/admin/users/${targetUser.id}",
                                    method: 'post',
                                    class: 'admin-form') {
                                input(
                                        type: 'hidden',
                                        name: (csrfParamName ?: '_csrf'),
                                        value: (csrfToken ?: '')
                                )

                                fieldset(class: 'admin-fieldset') {
                                    legend('Profile')

                                    div(class: 'admin-form-row admin-form-row-3') {
                                        div(class: 'admin-form-field') {
                                            label(for: 'firstName', 'First name')
                                            input(type: 'text', id: 'firstName', name: 'firstName',
                                                    value: targetUser.profile?.firstName ?: '')
                                        }
                                        div(class: 'admin-form-field admin-form-field-narrow') {
                                            label(for: 'middleInit', 'MI')
                                            input(type: 'text', id: 'middleInit', name: 'middleInit',
                                                    maxlength: '1',
                                                    value: targetUser.profile?.middleInit ?: '')
                                        }
                                        div(class: 'admin-form-field') {
                                            label(for: 'lastName', 'Last name')
                                            input(type: 'text', id: 'lastName', name: 'lastName',
                                                    value: targetUser.profile?.lastName ?: '')
                                        }
                                    }

                                    div(class: 'admin-form-field') {
                                        label(for: 'phone', 'Phone')
                                        input(type: 'tel', id: 'phone', name: 'phone',
                                                value: targetUser.profile?.phoneNumber ?: '')
                                    }

                                    div(class: 'admin-form-field') {
                                        label(for: 'email', 'Email')
                                        input(type: 'email', id: 'email', name: 'email',
                                                value: targetUser.email ?: '')
                                        p(class: 'admin-form-hint',
                                                'Changing the email will mark it unverified. The user can re-verify via the reset/verification email flow.')
                                    }
                                }

                                fieldset(class: 'admin-fieldset') {
                                    legend('Roles')

                                    div(class: 'admin-role-checkbox-group') {
                                        // USER — always present implicitly. Showing the
                                        // checkbox is informational; the service enforces
                                        // a USER baseline when no other role is set.
                                        boolean hasUser = targetUser.roles?.contains('USER')
                                        label(class: 'admin-role-checkbox') {
                                            input(type: 'checkbox', name: 'roles', value: 'USER',
                                                    (hasUser ? [checked: 'checked'] : [:]))
                                            span(class: 'admin-role-pill admin-role-pill-user', 'USER')
                                            span(class: 'admin-role-desc', 'Standard customer account.')
                                        }

                                        // ADMIN
                                        boolean hasAdmin = targetUser.roles?.contains('ADMIN')
                                        boolean adminDisabled = isSelf && hasAdmin
                                        label(class: 'admin-role-checkbox' + (adminDisabled ? ' admin-role-checkbox-disabled' : '')) {
                                            Map adminAttrs = [type: 'checkbox', name: 'roles', value: 'ADMIN']
                                            if (hasAdmin) adminAttrs['checked'] = 'checked'
                                            if (adminDisabled) adminAttrs['disabled'] = 'disabled'
                                            input(adminAttrs)
                                            span(class: 'admin-role-pill admin-role-pill-admin', 'ADMIN')
                                            span(class: 'admin-role-desc',
                                                    adminDisabled
                                                            ? 'You can\'t remove your own admin role.'
                                                            : 'Full portal access. Be careful — admin role is powerful.')
                                            // Disabled checkboxes don't submit. Carry the role
                                            // forward via a hidden input so the server still sees it.
                                            if (adminDisabled) {
                                                input(type: 'hidden', name: 'roles', value: 'ADMIN')
                                            }
                                        }

                                        // VENDOR
                                        boolean hasVendor = targetUser.roles?.contains('VENDOR')
                                        boolean vendorDisabled = !hasVendor && !canAssignVendor
                                        label(class: 'admin-role-checkbox' + (vendorDisabled ? ' admin-role-checkbox-disabled' : '')) {
                                            Map vendorAttrs = [type: 'checkbox', name: 'roles', value: 'VENDOR']
                                            if (hasVendor) vendorAttrs['checked'] = 'checked'
                                            if (vendorDisabled) vendorAttrs['disabled'] = 'disabled'
                                            input(vendorAttrs)
                                            span(class: 'admin-role-pill admin-role-pill-vendor', 'VENDOR')
                                            span(class: 'admin-role-desc',
                                                    vendorDisabled
                                                            ? 'Add a vendor via the Vendors section to assign this role. (Coming in Round 1B.)'
                                                            : 'Access to the vendor portal.')
                                        }
                                    }
                                }

                                div(class: 'admin-form-actions') {
                                    button(type: 'submit', class: 'admin-btn admin-btn-primary',
                                            'Save Changes')
                                    a(href: '/admin/users', class: 'admin-btn admin-btn-secondary',
                                            'Cancel')
                                }
                            }
                        }

                        // ============================================================
                        // RIGHT: account actions sidebar
                        // ============================================================
                        aside(class: 'admin-actions-panel') {

                            // ---- Account status ----
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Account Status')
                                p(class: 'admin-card-body') {
                                    yield 'Currently: '
                                    if (targetUser.isEnabled()) {
                                        span(class: 'admin-status-pill admin-status-pill-active', 'Active')
                                    } else {
                                        span(class: 'admin-status-pill admin-status-pill-disabled', 'Disabled')
                                    }
                                }
                                p(class: 'admin-card-body admin-card-hint') {
                                    if (targetUser.isEmailVerified()) {
                                        yield 'Email is verified.'
                                    } else {
                                        yield 'Email is NOT verified.'
                                    }
                                }

                                if (isSelf) {
                                    p(class: 'admin-card-hint',
                                            'You can\'t disable your own account.')
                                } else if (targetUser.isEnabled()) {
                                    form(action: "/admin/users/${targetUser.id}/disable",
                                            method: 'post',
                                            class: 'admin-inline-form') {
                                        input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                                value: (csrfToken ?: ''))
                                        button(type: 'submit',
                                                class: 'admin-btn admin-btn-danger admin-btn-block',
                                                onclick: "return confirm('Disable this account? They won\\'t be able to log in.');",
                                                'Disable Account')
                                    }
                                    p(class: 'admin-card-hint',
                                            'Soft delete — the user record stays for order history, but login is blocked.')
                                } else {
                                    form(action: "/admin/users/${targetUser.id}/enable",
                                            method: 'post',
                                            class: 'admin-inline-form') {
                                        input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                                value: (csrfToken ?: ''))
                                        button(type: 'submit',
                                                class: 'admin-btn admin-btn-primary admin-btn-block',
                                                'Re-enable Account')
                                    }
                                }
                            }

                            // ---- Password reset ----
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Password Reset')

                                if (isSelf) {
                                    p(class: 'admin-card-body',
                                            'Use the account page to reset your own password.')
                                } else if (!targetUser.email) {
                                    p(class: 'admin-card-body admin-card-hint',
                                            "Can't send a reset link — no email on file.")
                                } else {
                                    p(class: 'admin-card-body',
                                            "Sends a password-reset link to ${targetUser.email}. The user picks their own new password.")
                                    form(action: "/admin/users/${targetUser.id}/send-reset",
                                            method: 'post',
                                            class: 'admin-inline-form') {
                                        input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                                value: (csrfToken ?: ''))
                                        button(type: 'submit',
                                                class: 'admin-btn admin-btn-primary admin-btn-block',
                                                'Send Reset Email')
                                    }
                                }
                            }

                            // ---- Identity ----
                            section(class: 'admin-card') {
                                h2(class: 'admin-card-title', 'Identity')
                                dl(class: 'admin-dl') {
                                    dt('User ID')
                                    dd(class: 'admin-mono', targetUser.id)
                                    if (targetUser.vendorId) {
                                        dt('Vendor ID')
                                        dd(class: 'admin-mono', targetUser.vendorId)
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
