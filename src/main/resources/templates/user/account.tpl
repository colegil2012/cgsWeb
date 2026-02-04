package templates.user

layout 'layout.tpl',
        title: 'CGS Web | My Account',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        vendorInfo: vendorInfo,
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
                                span(class: 'readonly-box') { yield user?.role ?: 'CUSTOMER' }
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

                            if(addrErr) div(class: 'alert alert-error', addrErr)
                            if(addrMsg) div(class: 'alert alert-success', addrMsg)

                            h2('Addresses')
                            hr()

                            if (user?.addresses && !user.addresses.isEmpty()) {
                                user.addresses.each { addr ->
                                    div(class: 'address-card') {
                                        div(class: 'address-title') {
                                            strong(addr?.type ?: 'ADDRESS')
                                            if (addr?.isDefault) span(class: 'badge', 'Default')
                                        }
                                        div(class: 'address-lines') {
                                            div(addr?.street ?: '')
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
                        include template: 'partials/address-modal.tpl'
                    }

                    if (activeTab == 'orders') {
                        div(class: 'info-card') {
                            h2('Order History')
                            hr()

                            if (!orders || orders.content == null || orders.content.isEmpty()) {
                                p('No orders found yet.')
                            } else {
                                orders.content.each { o ->
                                    div(class: 'order-card') {
                                        div(class: 'order-card-header') {
                                            div(class: 'order-meta') {
                                                strong(o?.orderNumber ?: 'Order')
                                                span(class: 'order-status', o?.status ?: 'UNKNOWN')
                                            }
                                            div(class: 'order-date') {
                                                yield o?.createdAt ? o.createdAt.toString() : ''
                                            }
                                        }

                                        if (o?.totals) {
                                            div(class: 'order-totals') {
                                                div { span(class: 'label', 'Subtotal: ');
                                                    span("\$${o.totals.subtotal}") }
                                                div { span(class: 'label', 'Tax: '); span("\$${o.totals.tax}") }
                                                div { span(class: 'label', 'Shipping: ');
                                                    span("\$${o.totals.shipping}") }
                                                div(class: 'order-total-line') {
                                                    span(class: 'label', 'Total: ')
                                                    strong("\$${o.totals.total}")
                                                }
                                            }
                                        }

                                        if (o?.items && !o.items.isEmpty()) {
                                            div(class: 'order-items') {
                                                h4('Items')
                                                table(class: 'order-items-table') {
                                                    thead {
                                                        tr {
                                                            th('Item')
                                                            th('Qty')
                                                            th('Price')
                                                        }
                                                    }
                                                    tbody {
                                                        o.items.each { item ->
                                                            tr {
                                                                td(item?.name ?: 'Item')
                                                                td(item?.quantity ?: 0)
                                                                td(item?.priceAtPurchase != null ? "\$${item.priceAtPurchase}" : '')
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Pagination
                                if (orders.totalPages > 1) {
                                    div(class: 'pagination-container') {
                                        if (orders.number > 0) {
                                            a(class: 'page-link',
                                                    href: "/account?tab=orders&orderPage=${orders.number - 1}",
                                                    '← Prev')
                                        }

                                        (0..<orders.totalPages).each { pNum ->
                                            a(class: pNum == orders.number ? 'page-link active' : 'page-link',
                                                    href: "/account?tab=orders&orderPage=${pNum}",
                                                    "${pNum + 1}")
                                        }

                                        if (orders.number < orders.totalPages - 1) {
                                            a(class: 'page-link',
                                                    href: "/account?tab=orders&orderPage=${orders.number + 1}",
                                                    'Next →')
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

                            if (pwErr) {
                                div(class: 'alert alert-error', pwErr)
                            }
                            if (pwMsg) {
                                div(class: 'alert alert-success', pwMsg)
                            }

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
                                                name: (_csrf?.parameterName ?: '_csrf'),
                                                value: (_csrf?.token ?: '')
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

                    script(src: '/scripts/account-security.js') {}
                }
            }
        }