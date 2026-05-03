package templates.user

layout 'layout.tpl',
        title: 'CGS Web | My Account',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        vendorInfo: vendorInfo,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/account.css') },
        content: {
            div(class: 'account-tabs-layout') {

                // Left: Tab list
                div(class: 'account-tabs-nav') {
                    h2('My Account')
                    ul(class: 'tabs-list') {
                        li(class: activeTab == 'profile' ? 'tab-item active' : 'tab-item') {
                            a(href: '/account?tab=profile', 'Personal Info')
                        }
                        li(class: activeTab == 'addresses' ? 'tab-item active' : 'tab-item') {
                            a(href: '/account?tab=addresses', 'Addresses')
                        }
                        li(class: activeTab == 'orders' ? 'tab-item active' : 'tab-item') {
                            a(href: '/account?tab=orders', 'Order History')
                        }
                        li(class: activeTab == 'security' ? 'tab-item active' : 'tab-item') {
                            a(href: '/account?tab=security', 'Security')
                        }
                    }
                }

                // Right: Tab content
                div(class: 'account-tabs-panel') {

                    if (activeTab == 'profile') {
                        div(class: 'info-card') {
                            h2('Personal Info')
                            hr()

                            div(class: 'info-group') {
                                label('Username')
                                span(class: 'readonly-box', user?.username ?: 'N/A')
                            }

                            div(class: 'info-group') {
                                label('Email')
                                span(class: 'readonly-box', user?.email ?: 'N/A')
                            }

                            div(class: 'info-group') {
                                label('Account Role')
                                span(class: 'readonly-box') { yield user?.roles ?: 'USER' }
                            }

                            if (user?.profile) {
                                div(class: 'info-group') {
                                    label('First Name')
                                    span(class: 'readonly-box', user.profile?.firstName ?: 'N/A')
                                }
                                div(class: 'info-group') {
                                    label('Last Name')
                                    span(class: 'readonly-box', user.profile?.lastName ?: 'N/A')
                                }
                                div(class: 'info-group') {
                                    label('Phone')
                                    span(class: 'readonly-box', user.profile?.phoneNumber ?: 'N/A')
                                }
                            }
                        }
                    }

                    if (activeTab == 'addresses') {
                        div(class: 'info-card') {

                            if(message) div(class: 'alert alert-success', message)
                            if(error) div(class: 'alert alert-error', error)

                            h2('Addresses')
                            hr()

                            if (user?.addresses && !user.addresses.isEmpty()) {
                                user.addresses.each { addr ->
                                    div(class: 'address-card') {
                                        div(class: 'address-title') {
                                            strong(addr?.type ?: 'ADDRESS')
                                            if (addr?.isDefault) span(class: 'badge', 'Default')
                                        }
                                        div(class: 'address-line') {
                                            div(addr?.street1 ?: '')
                                            div("${addr?.city ?: ''}, ${addr?.state ?: ''} ${addr?.zip ?: ''}".toString())
                                        }
                                    }
                                }
                            } else {
                                p('No addresses saved yet.')
                            }

                            div(style: 'display:flex; gap: 12px; align-items:center; margin-top: 14px;') {
                                button(type: 'button', class: 'btn-small', id: 'openUpdateAddress', 'Edit Addresses')
                            }
                        }
                        include template: 'partials/user-address-modal.tpl'
                    }

                    if (activeTab == 'orders') {
                        div(class: 'account-tab-content') {
                            div(class: 'account-section-header') {
                                h2('My Orders')
                                p(class: 'account-section-sub',
                                        'Your most recent orders, newest first.')
                            }

                            if (!orders || orders.isEmpty()) {
                                div(class: 'account-empty-state') {
                                    p('You haven\'t placed any orders yet.')
                                    a(href: '/shop', class: 'btn', 'Browse the shop')
                                }
                            } else {
                                div(class: 'order-list') {
                                    orders.content.each { ord ->
                                        layout 'partials/order-card.tpl',
                                                order: ord,
                                                imagesBaseUrl: imagesBaseUrl
                                    }
                                }

                                // Pagination — only renders if there's more than one page.
                                if (ordersTotalPages > 1) {
                                    div(class: 'pagination') {
                                        (0..<ordersTotalPages).each { p ->
                                            if (p == ordersPage) {
                                                span(class: 'page-link active', "${p + 1}")
                                            } else {
                                                a(href: "/account?tab=orders&page=${p}",
                                                        class: 'page-link',
                                                        "${p + 1}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activeTab == 'security') {
                        div(class: 'info-card') {
                            h2('Security')
                            hr()

                            if (message) { div(class: 'alert alert-success', message) }
                            if (error) { div(class: 'alert alert-error', error) }

                            div(class: 'info-group') {
                                label('Password')
                                span(class: 'readonly-box', '••••••••••••')
                                small(class: 'text-muted', 'Password hidden for security')
                            }

                            div(style: 'display:flex; gap: 12px; align-items:center; margin-top: 14px;') {
                                button(type: 'button', class: 'btn-small', id: 'openChangePassword', 'Change Password')
                            }

                            // Modal (hidden by default; toggled by JS)
                            div(class: 'modal-overlay', id: 'changePasswordOverlay', style: 'display:none;') {
                                div(class: 'modal') {
                                    div(class: 'modal-header') {
                                        h3('Change Password')
                                        button(type: 'button', class: 'modal-close', id: 'closeChangePassword', '×')
                                    }

                                    form(action: '/account/password', method: 'post', class: 'form-group') {

                                        // CSRF support (works when Spring Security exposes _csrf in request)
                                        input(
                                                type: 'hidden',
                                                name: (csrfParamName ?: '_csrf'),
                                                value: (csrfToken ?: '')
                                        )

                                        div(class: 'form-control') {
                                            label(for: 'oldPassword', 'Old Password')
                                            input(type: 'password', name: 'oldPassword', id: 'oldPassword', required: 'required')
                                        }

                                        div(class: 'form-control') {
                                            label(for: 'newPassword', 'New Password')
                                            input(type: 'password', name: 'newPassword', id: 'newPassword', required: 'required', minlength: '10')
                                        }

                                        div(class: 'form-control') {
                                            label(for: 'confirmNewPassword', 'Confirm New Password')
                                            input(type: 'password', name: 'confirmNewPassword', id: 'confirmNewPassword', required: 'required', minlength: '10')
                                        }

                                        div(style: 'display:flex; gap: 10px; justify-content:flex-end;') {
                                            button(type: 'button', class: 'btn-small', id: 'cancelChangePassword', 'Cancel')
                                            button(type: 'submit', class: 'btn', style: 'width:auto;', 'Update Password')
                                        }
                                    }
                                }
                            }
                        }
                    }

                    script(src: '/scripts/user/account-security.js') {}
                    script(src: '/scripts/address/address-update.js') {}
                }
            }
        }