package templates.admin.products

/*
 * Admin product create/edit form.
 *
 * Path: templates/admin/products/edit.tpl
 * URLs: /admin/products/new        (product == null -> create)
 *       /admin/products/{id}       (product != null -> edit)
 *
 * Bindings:
 *   product             — Product or null
 *   allVendors          — List<Vendor> (id+name) for the vendor dropdown
 *   preselectedVendorId — create mode: vendor to pre-select (from ?vendor=)
 *   activeSection       — "products"
 *   error               — flash
 *
 * Note: Product.ProductAttributes (dimensions/weight/type) is intentionally
 * NOT in this form for Round 1B — see ROUND-1B-NOTES.md. Add later if needed.
 */

boolean isEdit = (product != null)
String formAction = isEdit ? "/admin/products/${product.id}" : '/admin/products'
String pageTitle = isEdit ? "Edit Product: ${product.name}" : 'Add Product'

layout 'layout.tpl',
        title: 'CGS Web | Admin · ' + (isEdit ? 'Edit Product' : 'Add Product'),
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
                            a(href: '/admin/products', '← Back to Products')
                        }
                        h1(class: 'admin-page-title', pageTitle)
                    }

                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    section(class: 'admin-card admin-card-form') {
                        form(action: formAction, method: 'post', class: 'admin-form') {
                            input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                    value: (csrfToken ?: ''))

                            fieldset(class: 'admin-fieldset') {
                                legend('Basics')

                                div(class: 'admin-form-field') {
                                    label(for: 'name', 'Product name *')
                                    input(type: 'text', id: 'name', name: 'name', required: 'required',
                                            value: isEdit ? (product.name ?: '') : '')
                                }

                                div(class: 'admin-form-row admin-form-row-2') {
                                    div(class: 'admin-form-field') {
                                        label(for: 'sku', 'SKU')
                                        input(type: 'text', id: 'sku', name: 'sku',
                                                value: isEdit ? (product.sku ?: '') : '')
                                    }
                                    div(class: 'admin-form-field') {
                                        label(for: 'slug', 'Slug')
                                        input(type: 'text', id: 'slug', name: 'slug',
                                                value: isEdit ? (product.slug ?: '') : '',
                                                placeholder: 'auto from name')
                                    }
                                }

                                div(class: 'admin-form-field') {
                                    label(for: 'vendorId', 'Vendor *')
                                    select(id: 'vendorId', name: 'vendorId', required: 'required') {
                                        String currentVendor = isEdit
                                                ? product.vendorId
                                                : preselectedVendorId
                                        if (currentVendor == null) {
                                            option(value: '', 'Select a vendor…')
                                        }
                                        allVendors.each { v ->
                                            Map vAttrs = [value: v.id]
                                            if (currentVendor == v.id) vAttrs['selected'] = 'selected'
                                            option(vAttrs, v.name ?: v.id)
                                        }
                                    }
                                }

                                div(class: 'admin-form-field') {
                                    label(for: 'description', 'Description')
                                    textarea(id: 'description', name: 'description', rows: '4',
                                            isEdit ? (product.description ?: '') : '')
                                }

                                div(class: 'admin-form-field') {
                                    label(for: 'imageUrl', 'Image URL')
                                    input(type: 'text', id: 'imageUrl', name: 'imageUrl',
                                            value: isEdit ? (product.imageUrl ?: '') : '')
                                }

                                div(class: 'admin-form-field') {
                                    label(for: 'categoryId', 'Category ID')
                                    input(type: 'text', id: 'categoryId', name: 'categoryId',
                                            value: isEdit ? (product.categoryId ?: '') : '')
                                    p(class: 'admin-form-hint',
                                            'Optional. The category management UI is not part of this round.')
                                }
                            }

                            fieldset(class: 'admin-fieldset') {
                                legend('Pricing')
                                div(class: 'admin-form-row admin-form-row-2') {
                                    div(class: 'admin-form-field') {
                                        label(for: 'price', 'Price')
                                        input(type: 'number', id: 'price', name: 'price',
                                                step: '0.01', min: '0',
                                                value: isEdit && product.price != null ? product.price : '')
                                    }
                                    div(class: 'admin-form-field') {
                                        label(for: 'salePrice', 'Sale price')
                                        input(type: 'number', id: 'salePrice', name: 'salePrice',
                                                step: '0.01', min: '0',
                                                value: isEdit && product.salePrice != null ? product.salePrice : '')
                                    }
                                }
                            }

                            fieldset(class: 'admin-fieldset') {
                                legend('Inventory')
                                div(class: 'admin-form-row admin-form-row-2') {
                                    div(class: 'admin-form-field') {
                                        label(for: 'stock', 'Stock quantity')
                                        input(type: 'number', id: 'stock', name: 'stock', min: '0',
                                                value: isEdit && product.stock != null ? product.stock : '0')
                                        p(class: 'admin-form-hint',
                                                'At 0, the product is hidden from the storefront but stays here.')
                                    }
                                    div(class: 'admin-form-field') {
                                        label(for: 'lowStockThreshold', 'Low-stock threshold')
                                        input(type: 'number', id: 'lowStockThreshold',
                                                name: 'lowStockThreshold', min: '0',
                                                value: isEdit && product.lowStockThreshold != null
                                                        ? product.lowStockThreshold : '')
                                        p(class: 'admin-form-hint',
                                                'At or below this, the product list flags it as low.')
                                    }
                                }

                                div(class: 'admin-form-field admin-form-field-check') {
                                    label(class: 'admin-checkbox-label') {
                                        Map activeAttrs = [type: 'checkbox', name: 'active', value: 'true']
                                        if (isEdit && product.active) activeAttrs['checked'] = 'checked'
                                        input(activeAttrs)
                                        span('Active')
                                    }
                                    p(class: 'admin-form-hint',
                                            'Manual on/off switch, separate from stock. A product must be BOTH active AND in stock to show on the storefront.')
                                }
                            }

                            div(class: 'admin-form-actions') {
                                button(type: 'submit', class: 'admin-btn admin-btn-primary',
                                        isEdit ? 'Save Changes' : 'Create Product')
                                a(href: isEdit ? "/admin/products/${product.id}" : '/admin/products',
                                        class: 'admin-btn admin-btn-secondary', 'Cancel')
                            }
                        }
                    }
                }
            }
        }
