package templates.vendor

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Vendor',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        vendorDetail: vendorDetail,
        products: products,
        headContent: { link(rel: 'stylesheet', href: '/css/pages/vendor-portal.css') },
        content: {

            div(class: 'hero') {
                h1("${vendorDetail.name}'s Portal")
                p('Manage your Vendor Profile and list items for sale at Celtech General Store.')
            }

            div(class: 'vendor-portal-container') {
                // Alerts Section
                if (message || error) {
                    div(class: 'alert-wrapper') {
                        if (message) {
                            div(class: 'alert alert-success') {
                                ul(style: 'margin:0; padding-left: 20px;') {
                                    message.each { msg -> li(msg) }
                                }
                            }
                        }
                        if (error) {
                            div(class: 'alert alert-error') {
                                ul(style: 'margin:0; padding-left: 20px;') {
                                    error.each { msg -> li(msg) }
                                }
                            }
                        }
                    }
                }

                // Navigation Tab
                nav(class: 'vendor-tabs-nav') {
                    ul(class: 'vendor-tabs-list') {
                        li(class: activeTab == 'profile' ? 'vendor-tab active' : 'vendor-tab') {
                            a(href: '/vendor/portal?tab=profile', 'Profile')
                        }
                        li(class: activeTab == 'addresses' ? 'vendor-tab active' : 'vendor-tab') {
                            a(href: '/vendor/portal?tab=addresses', 'Addresses')
                        }
                        li(class: activeTab == 'inventory' ? 'vendor-tab active' : 'vendor-tab') {
                            a(href: '/vendor/portal?tab=inventory', 'Inventory')
                        }
                        li(class: activeTab == 'orders' ? 'vendor-tab active' : 'vendor-tab') {
                            a(href: '/vendor/portal?tab=orders', 'Orders')
                        }
                    }
                }

                div(class: 'vendor-tabs-panel') {

                    // ════════ PROFILE TAB ════════
                    if (activeTab == 'profile') {
                        div(class: 'vendor-tab-content') {

                            // Logo Section
                            div(class: 'vendor-logo-section') {
                                div(class: 'vendor-section-header') {
                                    h2('Update Logo')
                                }

                                img(id: 'vendorLogoPreview',
                                        src: ImageUrlUtil.resolve(vendorDetail?.logo_url, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                        alt: 'Logo Preview')

                                button(type: 'button', class: 'btn-small', id: 'changeLogoBtn', 'Change Logo')

                                form(id: 'vendorLogoForm',
                                        action: '/vendor/portal/update-logo',
                                        method: 'post',
                                        enctype: 'multipart/form-data') {
                                    input(type: 'hidden', id: 'vendorId', name: 'vendorId', value: vendorDetail?.id)
                                    input(type: 'file', id: 'vendorLogoUpload', name: 'vendorLogo', style: 'display:none;', accept: 'image/*')
                                }
                            }
                            // Settings Section
                            div(class: 'vendor-settings-section') {
                                div(class: 'vendor-section-header') {
                                    h2('Settings')
                                }
                                div(class: 'padding-container') {

                                    form(id: 'vendorSettingsForm', action: '/vendor/portal/update-settings', method: 'post') {
                                        input(type: 'hidden', name: 'vendorId', value: vendorDetail?.id)

                                        div(class: 'vendor-settings', 'data-setting': 'lead_time') {
                                            label('Order Processing Time (Days)')
                                            span(class: 'readonly-box vendor-setting-display', 'data-setting-display': 'lead_time', vendorDetail?.lead_time)
                                            input(type: 'number', name: 'leadTime', min: '0', step: '1', value: vendorDetail?.lead_time, class: 'vendor-setting-input', 'data-setting-input': 'lead_time', style: 'display:none;', disabled: 'disabled')
                                        }

                                        div(class: 'vendor-settings', 'data-setting': 'active') {
                                            label('Active')
                                            span(class: 'readonly-checkbox vendor-setting-display', 'data-setting-display': 'active', vendorDetail?.active)
                                            input(type: 'checkbox', name: 'active', value: vendorDetail?.active, class: 'vendor-setting-input', 'data-setting-input': 'active', style: 'display:none;', disabled: 'disabled')
                                        }
                                    }

                                    div(class: 'vendor-settings-actions') {
                                        button(type: 'button', class: 'btn-small', id: 'editVendorSettingsBtn', 'Edit')
                                        button(type: 'button', class: 'btn-small', id: 'cancelVendorSettingsBtn', style: 'display:none;', 'Cancel')
                                        button(type: 'submit', class: 'btn-small', id: 'saveVendorSettingsBtn', form: 'vendorSettingsForm', style: 'display:none;', 'Save')
                                    }
                                }
                            }
                        }
                    }

                    // ════════ ADDRESSES TAB ════════
                    if (activeTab == 'addresses') {
                        div(class: 'vendor-tab-content') {

                            div(class: 'vendor-address-section') {

                                div(class: 'vendor-section-header') {
                                    h2('Vendor Addresses')
                                }

                                div(class: 'padding-container') {
                                    if (vendorDetail?.addresses && !vendorDetail.addresses.isEmpty()) {
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
                                    } else {
                                        p('No addresses saved yet.')
                                    }

                                    div(style: 'display:flex; gap: 12px; align-items:center; margin-top: 14px;') {
                                        button(type: 'button', class: 'btn-small', id: 'openUpdateAddress', 'Edit Addresses')
                                    }
                                }
                            }

                            include template: 'partials/vendor-address-modal.tpl'
                        }
                    }

                    // ════════ INVENTORY TAB ════════
                    if (activeTab == 'inventory') {
                        div(class: 'vendor-tab-content') {
                            div(class: 'vendor-section-header') {
                                h2('Inventory')
                            }

                            div(class: 'padding-container') {

                                if (products && !products.isEmpty()) {

                                    products.each { prod ->
                                        def formId = "productForm_${prod.id}"

                                        div(class: 'inventory-item-card', id: "item_${prod.id}") {

                                            // ── Read-Only View ──
                                            div(class: 'inventory-item-view', id: "view_${prod.id}") {
                                                div(class: 'inventory-item-header') {
                                                    div(class: 'inventory-item-title') {
                                                        strong(prod.name ?: 'Unnamed Product')
                                                        if (prod.active) {
                                                            span(class: 'badge badge-active', 'Active')
                                                        } else {
                                                            span(class: 'badge badge-inactive', 'Inactive')
                                                        }
                                                    }
                                                    button(type: 'button', class: 'btn-small',
                                                            onclick: "toggleEditProduct('${prod.id}')", 'Edit')
                                                }

                                                div(class: 'inventory-item-details') {
                                                    div(class: 'inventory-detail-group') {
                                                        label('SKU')
                                                        span(class: 'readonly-box', prod.sku ?: 'N/A')
                                                    }
                                                    div(class: 'inventory-detail-group') {
                                                        label('Price')
                                                        span(class: 'readonly-box', prod.price != null ? "\$${prod.price}" : 'N/A')
                                                    }
                                                    div(class: 'inventory-detail-group') {
                                                        label('Sale Price')
                                                        span(class: 'readonly-box', prod.salePrice != null ? "\$${prod.salePrice}" : '—')
                                                    }
                                                    div(class: 'inventory-detail-group') {
                                                        label('Stock')
                                                        def stockClass = (prod.stock != null && prod.lowStockThreshold != null && prod.stock <= prod.lowStockThreshold) ? 'readonly-box low-stock' : 'readonly-box'
                                                        span(class: stockClass, prod.stock != null ? "${prod.stock}" : '0')
                                                    }
                                                    div(class: 'inventory-detail-group') {
                                                        label('Low Stock Threshold')
                                                        span(class: 'readonly-box', prod.lowStockThreshold != null ? "${prod.lowStockThreshold}" : '0')
                                                    }
                                                    div(class: 'inventory-detail-group') {
                                                        label('Category')
                                                        span(class: 'readonly-box', categories?.get(prod.categoryId) ?: 'Uncategorized')
                                                    }

                                                    if (prod.description) {
                                                        div(class: 'inventory-detail-group full-width') {
                                                            label('Description')
                                                            span(class: 'readonly-box', prod.description)
                                                        }
                                                    }
                                                }
                                            }

                                            // ── Edit Form (hidden by default) ──
                                            div(class: 'inventory-item-edit', id: "edit_${prod.id}", style: 'display:none;') {
                                                form(id: formId, action: '/vendor/portal/update-product', method: 'post') {
                                                    input(type: 'hidden', name: 'productId', value: prod.id)

                                                    div(class: 'inventory-edit-grid') {
                                                        div(class: 'inventory-edit-group full-width') {
                                                            label(for: "name_${prod.id}", 'Product Name')
                                                            input(type: 'text', id: "name_${prod.id}", name: 'name', value: prod.name ?: '', required: 'required')
                                                        }

                                                        div(class: 'inventory-edit-group') {
                                                            label(for: "price_${prod.id}", 'Price')
                                                            input(type: 'number', id: "price_${prod.id}", name: 'price', value: prod.price ?: '', step: '0.01', min: '0', required: 'required')
                                                        }

                                                        div(class: 'inventory-edit-group') {
                                                            label(for: "salePrice_${prod.id}", 'Sale Price')
                                                            input(type: 'number', id: "salePrice_${prod.id}", name: 'salePrice', value: prod.salePrice ?: '', step: '0.01', min: '0')
                                                        }

                                                        div(class: 'inventory-edit-group') {
                                                            label(for: "stock_${prod.id}", 'Stock')
                                                            input(type: 'number', id: "stock_${prod.id}", name: 'stock', value: prod.stock ?: 0, min: '0', required: 'required')
                                                        }

                                                        div(class: 'inventory-edit-group') {
                                                            label(for: "lowStock_${prod.id}", 'Low Stock Threshold')
                                                            input(type: 'number', id: "lowStock_${prod.id}", name: 'lowStockThreshold', value: prod.lowStockThreshold ?: 0, min: '0')
                                                        }

                                                        div(class: 'inventory-edit-group full-width') {
                                                            label(for: "desc_${prod.id}", 'Description')
                                                            textarea(id: "desc_${prod.id}", name: 'description', rows: '3', prod.description ?: '')
                                                        }

                                                        div(class: 'inventory-edit-group') {
                                                            label('Active')
                                                            div(class: 'checkbox-wrapper') {
                                                                input(type: 'checkbox', id: "active_${prod.id}", name: 'active', value: 'true', checked: prod.active ? 'checked' : null)
                                                                label(for: "active_${prod.id}", 'Listed on store')
                                                            }
                                                        }
                                                    }

                                                    div(class: 'inventory-edit-actions') {
                                                        button(type: 'button', class: 'btn-small',
                                                                onclick: "cancelEditProduct('${prod.id}')", 'Cancel')
                                                        button(type: 'submit', class: 'btn-small btn-save', 'Save Changes')
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    p('No products found. Add products to get started.')
                                }
                            }
                        }
                    }

                    // ════════ ORDERS TAB ════════
                    if (activeTab == 'orders') {
                        div(class: 'vendor-tab-content') {
                            h2('Orders')
                            p('Order management coming soon.')
                        }
                    }
                }
            }
            script(src: '/scripts/address/address-update.js') {}
            script(src: '/scripts/vendor/vendor.js') {}
        }








