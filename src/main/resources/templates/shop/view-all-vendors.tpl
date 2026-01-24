package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
    title: 'Our Local Vendors',
    user: user,
    cartItems: cartItems,
    vendors: vendors,
    content: {
        div(class: 'vendor-page-container') {
            div(class: 'hero') {
                h1 'Our Trusted Partners'
                p 'Discover the local farms and producers bringing fresh organic produce to your door.'
            }
            div(class: 'vendor-grid') {
                vendors?.each { vendor ->
                    div(class: 'vendor-card') {
                        // Clickable Logo
                        a(href: "/vendor/${vendor.id}") {
                            div(class: 'vendor-logo-container') {
                                img(src: ImageUrlUtil.resolve(vendor.logo_url, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                        alt: "${vendor.name} logo",
                                        class: 'vendor-logo-img')
                            }
                        }

                        div(class: 'vendor-info') {
                            h2 vendor.name

                            if (vendor.description) {
                                p(class: 'vendor-description', vendor.description)
                            }

                            // Address Block
                            div(class: 'vendor-address') {
                                // The fix: Use [0] instead of .first() to avoid exceptions on empty lists
                                def defaultAddress = vendor.addresses?.find { it.isDefault } ?: vendor.addresses?.getAt(0)

                                if (defaultAddress) {
                                    strong "${defaultAddress.type ?: 'Location'}:"
                                    p {
                                        yield "${defaultAddress.street ?: ''}"
                                        br()
                                        yield "${defaultAddress.city ?: ''}, ${defaultAddress.state ?: ''} ${defaultAddress.zip ?: ''}"
                                    }
                                } else {
                                    p 'No address listed.'
                                }
                            }
                            div(class: 'vendor-footer') {
                                a(href: "/vendor/${vendor.id}", class: 'btn-view-vendor', 'View Store Page')
                            }
                        }
                    }
                }
                if(!vendors) {
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