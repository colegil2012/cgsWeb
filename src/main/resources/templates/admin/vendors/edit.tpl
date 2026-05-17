package templates.admin.vendors

/*
 * Admin vendor create/edit form.
 *
 * Path: templates/admin/vendors/edit.tpl
 * URLs: /admin/vendors/new        (vendor == null  -> create mode)
 *       /admin/vendors/{id}/edit  (vendor != null  -> edit mode)
 *
 * Create mode POSTs to /admin/vendors; edit mode POSTs to /admin/vendors/{id}.
 */

boolean isEdit = (vendor != null)
String formAction = isEdit ? "/admin/vendors/${vendor.id}" : '/admin/vendors'
String pageTitle = isEdit ? "Edit Vendor: ${vendor.name}" : 'Add Vendor'

layout 'layout.tpl',
        title: 'CGS Web | Admin · ' + (isEdit ? 'Edit Vendor' : 'Add Vendor'),
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
                            a(href: '/admin/vendors', '← Back to Vendors')
                        }
                        h1(class: 'admin-page-title', pageTitle)
                    }

                    if (error) div(class: 'admin-alert admin-alert-error', error)

                    section(class: 'admin-card admin-card-form') {
                        form(action: formAction, method: 'post', class: 'admin-form') {
                            input(type: 'hidden', name: (csrfParamName ?: '_csrf'),
                                    value: (csrfToken ?: ''))

                            div(class: 'admin-form-field') {
                                label(for: 'name', 'Vendor name *')
                                input(type: 'text', id: 'name', name: 'name', required: 'required',
                                        value: isEdit ? (vendor.name ?: '') : '')
                            }

                            div(class: 'admin-form-field') {
                                label(for: 'slug', 'Slug')
                                input(type: 'text', id: 'slug', name: 'slug',
                                        value: isEdit ? (vendor.slug ?: '') : '',
                                        placeholder: 'auto-generated from name if left blank')
                                p(class: 'admin-form-hint',
                                        'URL-friendly identifier. Leave blank to auto-generate. Collisions get a numeric suffix.')
                            }

                            div(class: 'admin-form-field') {
                                label(for: 'description', 'Description')
                                textarea(id: 'description', name: 'description', rows: '4',
                                        isEdit ? (vendor.description ?: '') : '')
                            }

                            div(class: 'admin-form-field') {
                                label(for: 'logoUrl', 'Logo URL')
                                input(type: 'text', id: 'logoUrl', name: 'logoUrl',
                                        value: isEdit ? (vendor.logoUrl ?: '') : '')
                            }

                            div(class: 'admin-form-field admin-form-field-check') {
                                label(class: 'admin-checkbox-label') {
                                    Map activeAttrs = [type: 'checkbox', name: 'active', value: 'true']
                                    if (isEdit && vendor.active) activeAttrs['checked'] = 'checked'
                                    input(activeAttrs)
                                    span('Active')
                                }
                                p(class: 'admin-form-hint',
                                        'Inactive vendors are hidden from the storefront vendor list.')
                            }

                            div(class: 'admin-form-actions') {
                                button(type: 'submit', class: 'admin-btn admin-btn-primary',
                                        isEdit ? 'Save Changes' : 'Create Vendor')
                                a(href: isEdit ? "/admin/vendors/${vendor.id}" : '/admin/vendors',
                                        class: 'admin-btn admin-btn-secondary', 'Cancel')
                            }
                        }
                    }
                }
            }
        }
