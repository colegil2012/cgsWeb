package templates.vendor

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Vendor',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        vendorDetail: vendorDetail,
        products: products,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/vendor-portal.css') },
        content: {

            div(class: 'hero') {
                div(class: 'hero-content') {
                    div(class: 'hero-vendor-logo') {
                        img(src: ImageUrlUtil.resolve(vendorDetail?.logoUrl, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                alt: vendorDetail?.name)
                    }
                    h1(class: 'hero-vendor-name', "Vendor Portal")
                }
            }

            div(class: 'vendor-portal-container') {

                // ---- Alerts ------------------------------------------------
                if (message || error) {
                    div(class: 'alert-wrapper') {
                        if (message) {
                            div(class: 'alert alert-success') {
                                ul {
                                    message.each { msg -> li(msg) }
                                }
                            }
                        }
                        if (error) {
                            div(class: 'alert alert-error') {
                                ul {
                                    error.each { msg -> li(msg) }
                                }
                            }
                        }
                    }
                }

                // ---- Tab nav -----------------------------------------------
                nav(class: 'vendor-portal-tabs') {
                    ul {
                        li(class: activeTab == 'profile' ? 'vendor-portal-tab active' : 'vendor-portal-tab') {
                            a(href: '/vendor/portal?tab=profile', 'Profile')
                        }
                        li(class: activeTab == 'addresses' ? 'vendor-portal-tab active' : 'vendor-portal-tab') {
                            a(href: '/vendor/portal?tab=addresses', 'Addresses')
                        }
                        li(class: activeTab == 'inventory' ? 'vendor-portal-tab active' : 'vendor-portal-tab') {
                            a(href: '/vendor/portal?tab=inventory', 'Inventory')
                        }
                        li(class: activeTab == 'orders' ? 'vendor-portal-tab active' : 'vendor-portal-tab') {
                            a(href: '/vendor/portal?tab=orders', 'Orders')
                        }
                    }
                }

                // ---- Tab content panel -------------------------------------
                div(class: 'vendor-portal-panel') {

                    // ════════ PROFILE TAB ════════════════════════════════
                    if (activeTab == 'profile') {

                        div(class: 'vendor-portal-section') {
                            div(class: 'vendor-portal-section-header') {
                                h2('Profile')
                            }

                            div(class: 'vendor-portal-profile-row') {

                                // ---- Left half: logo + change button ----
                                div(class: 'vendor-portal-profile-logo') {
                                    div(class: 'vendor-logo-preview-wrapper') {
                                        img(id: 'vendorLogoPreview',
                                                class: 'vendor-logo-preview',
                                                src: ImageUrlUtil.resolve(vendorDetail?.logoUrl, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                                alt: 'Logo Preview')
                                    }
                                    button(type: 'button', class: 'btn btn-small', id: 'changeLogoBtn', 'Change Logo')
                                }

                                // ---- Right half: read-only status ----
                                div(class: 'vendor-portal-profile-status') {
                                    def isActive = vendorDetail?.active
                                    div(class: 'vendor-status-row') {
                                        span(class: 'label', 'Listing:')
                                        if (isActive) {
                                            span(class: 'vendor-status-pill is-active', 'Active')
                                        } else {
                                            span(class: 'vendor-status-pill is-inactive', 'Inactive')
                                        }
                                    }
                                    p(class: 'vendor-status-help',
                                            'Status is managed by Celtech. Reach out if you need this changed.')
                                }
                            }

                            // Hidden file input form, paired with #changeLogoBtn via vendor.js.
                            // Position in markup is irrelevant since it's display:none.
                            form(id: 'vendorLogoForm',
                                    action: '/vendor/portal/update-logo',
                                    method: 'post',
                                    enctype: 'multipart/form-data') {
                                input(type: 'hidden',
                                        name: (csrfParamName ?: '_csrf'),
                                        value: (csrfToken ?: ''))
                                input(type: 'hidden', id: 'vendorId', name: 'vendorId', value: vendorDetail?.id)
                                input(type: 'file', id: 'vendorLogoUpload', name: 'vendorLogo',
                                        accept: 'image/*',
                                        class: 'is-hidden')
                            }
                        }
                    }

                    // ════════ ADDRESSES TAB ══════════════════════════════
                    if (activeTab == 'addresses') {

                        div(class: 'vendor-portal-section') {
                            div(class: 'vendor-portal-section-header') {
                                h2('Vendor Addresses')
                            }

                            div(class: 'vendor-portal-address-block') {
                                if (vendorDetail?.addresses && !vendorDetail.addresses.isEmpty()) {
                                    div(class: 'vendor-address-list') {
                                        vendorDetail.addresses.each { addr ->
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
                                    p('No addresses saved yet.')
                                }

                                div(class: 'vendor-address-actions') {
                                    button(type: 'button', class: 'btn btn-small', id: 'openUpdateAddress', 'Edit Addresses')
                                }
                            }
                        }

                        include template: 'partials/vendor-address-modal.tpl'
                    }

                    // ════════ INVENTORY TAB (placeholder) ════════════════
                    if (activeTab == 'inventory') {
                        div(class: 'vendor-portal-empty-state') {
                            h3('Inventory is managed for you')
                            p("We're currently handling inventory and stock for partner vendors at the store level.")
                            p('If we open this up to vendor-managed listings or supplementary stock in the future, you\'ll see them here.')
                        }
                    }

                    // ════════ ORDERS TAB (placeholder) ═══════════════════
                    if (activeTab == 'orders') {
                        div(class: 'vendor-portal-empty-state') {
                            h3('Order management coming soon')
                            p("There's nothing to show here yet \u2014 orders fulfilled by Celtech don't surface to vendors today.")
                            p('When we add direct-to-vendor or supplementary-stock workflows, your orders will appear here.')
                        }
                    }
                }
            }

            script(src: '/scripts/address/address-update.js') {}
            script(src: '/scripts/vendor/vendor.js') {}
        }

