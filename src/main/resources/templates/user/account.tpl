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

            // Hero header (parallel to vendor-portal's hero)
            div(class: 'hero') {
                div(class: 'hero-content') {
                    h1(class: 'hero-vendor-name', "${user?.username?.toUpperCase()}\'s Account")
                }
            }

            div(class: 'account-portal-container') {

                // ---- Tab nav -----------------------------------------------
                nav(class: 'account-portal-tabs') {
                    ul {
                        li(class: activeTab == 'profile' ? 'account-portal-tab active' : 'account-portal-tab') {
                            a(href: '/account?tab=profile', 'Personal Info')
                        }
                        li(class: activeTab == 'addresses' ? 'account-portal-tab active' : 'account-portal-tab') {
                            a(href: '/account?tab=addresses', 'Addresses')
                        }
                        li(class: activeTab == 'orders' ? 'account-portal-tab active' : 'account-portal-tab') {
                            a(href: '/account?tab=orders', 'Order History')
                        }
                        li(class: activeTab == 'security' ? 'account-portal-tab active' : 'account-portal-tab') {
                            a(href: '/account?tab=security', 'Security')
                        }
                    }
                }

                // ---- Tab content panel -------------------------------------
                div(class: 'account-portal-panel') {

                    // ════════ PERSONAL INFO TAB ══════════════════════════
                    if (activeTab == 'profile') {
                        div(class: 'account-portal-section') {
                            div(class: 'account-portal-section-header') {
                                h2('Personal Info')
                            }

                            div(class: 'info-group-list') {
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
                    }

                    // ════════ ADDRESSES TAB ══════════════════════════════
                    if (activeTab == 'addresses') {
                        div(class: 'account-portal-section') {
                            div(class: 'account-portal-section-header') {
                                h2('Addresses')
                            }

                            if (message) div(class: 'alert alert-success', message)
                            if (error) div(class: 'alert alert-error', error)

                            if (user?.addresses && !user.addresses.isEmpty()) {
                                div(class: 'account-address-list') {
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
                                }
                            } else {
                                p(class: 'account-address-list-empty', 'No addresses saved yet.')
                            }

                            div(class: 'account-portal-actions') {
                                button(type: 'button', class: 'btn btn-small', id: 'openUpdateAddress', 'Edit Addresses')
                            }
                        }

                        include template: 'partials/user-address-modal.tpl'
                    }

                    // ════════ ORDER HISTORY TAB ══════════════════════════
                    if (activeTab == 'orders') {
                        div(class: 'account-portal-section') {
                            div(class: 'account-portal-section-header') {
                                h2('My Orders')
                                p(class: 'account-portal-section-sub',
                                        'Your most recent orders, newest first.')
                            }

                            if (!orders || orders.isEmpty()) {
                                div(class: 'account-portal-empty-state') {
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

                                if (ordersTotalPages > 1) {
                                    div(class: 'pagination-container') {
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

                    // ════════ SECURITY TAB ═══════════════════════════════
                    if (activeTab == 'security') {
                        div(class: 'account-portal-section') {
                            div(class: 'account-portal-section-header') {
                                h2('Security')
                            }

                            if (message) div(class: 'alert alert-success', message)
                            if (error) div(class: 'alert alert-error', error)

                            div(class: 'info-group-list') {
                                div(class: 'info-group') {
                                    label('Password')
                                    span(class: 'readonly-box', '••••••••••••')
                                    small('Password hidden for security')
                                }
                            }

                            div(class: 'account-portal-actions') {
                                button(type: 'button', class: 'btn btn-small', id: 'openChangePassword', 'Change Password')
                            }
                        }

                        include template: 'partials/change-password-modal.tpl'
                    }
                }
            }

            script(src: '/scripts/user/account-security.js') {}
            script(src: '/scripts/address/address-update.js') {}
        }
