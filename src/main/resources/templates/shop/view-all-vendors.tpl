package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Our Vendors',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        vendors: vendors,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/vendors.css') },
        content: {
            div(class: 'vendor-page-container') {
                div(class: 'hero') {
                    div(class: 'hero-content') {
                        img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Text Logo Middle.png', imagesBaseUrl),
                                alt: 'Celtech Logo',
                                class: 'hero-logo float-in')
                        h3("Our Trusted Vendors")
                    }
                }
                div(class: 'container-wide') {
                    div(class: 'vendor-grid') {
                        vendors?.each { vendor ->
                            div(class: 'vendor-card') {
                                // Clickable Logo
                                a(href: "/vendor/${vendor.id}") {
                                    div(class: 'vendor-logo-container') {
                                        img(src: ImageUrlUtil.resolve(vendor.logoUrl, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                                alt: "${vendor.name} logo",
                                                class: 'vendor-logo-img')
                                    }
                                }

                                div(class: 'vendor-info') {
                                    span(class: 'vendor-card-name', vendor.name)

                                    if (vendor.description) {
                                        p(class: 'vendor-description', vendor.description)
                                    }

                                    // Address Block
                                    div(class: 'vendor-address') {
                                        def defaultAddress = vendor.addresses?.find { it.isDefault } ?: vendor.addresses?.getAt(0)

                                        if (defaultAddress) {
                                            strong "${defaultAddress.type ?: 'Find us at'}:"
                                            p {
                                                yield "${defaultAddress.street1 ?: ''}"
                                                br()
                                                yield "${defaultAddress.city ?: ''}, ${defaultAddress.state ?: ''} ${defaultAddress.zip ?: ''}"
                                            }
                                        } else {
                                            p 'No address listed.'
                                        }
                                    }
                                    div(class: 'vendor-card-footer') {
                                        a(href: "/vendor/${vendor.id}", class: 'btn btn-small', 'View Store Page')
                                    }
                                }
                            }
                        }
                        if (!vendors) {
                            p('No vendors found at this time.')
                        }
                    }

                    div(class: 'pagination-container') {
                        if (totalPages > 1) {
                            (0..<totalPages).each { i ->
                                a(href: "/vendors?page=${i}",
                                        class: "page-link ${i == currentPage ? 'active' : ''}", i + 1)
                            }
                        }
                    }
                }
            }
        }
