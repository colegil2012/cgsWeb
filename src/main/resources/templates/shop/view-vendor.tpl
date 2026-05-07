package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: vendor?.name ?: 'Vendor Profile',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/vendor.css') },
        content: {

            // Vendor banner (uses shared .hero with .hero-vendor-* helpers)
            div(class: 'hero') {
                div(class: 'hero-content') {
                    div(class: 'hero-vendor-logo') {
                        img(src: ImageUrlUtil.resolve(vendor?.logoUrl, imagesBaseUrl) ?: '/images/site-images/default-vendor.png',
                                alt: vendor?.name)
                    }
                    h1(class: 'hero-vendor-name', vendor?.name)
                    if (vendor?.description) {
                        p(class: 'hero-vendor-bio', vendor.description)
                    }
                    div(class: 'hero-vendor-location') {
                        span(class: 'hero-vendor-location-label', 'Find us at:')
                        def displayAddr = vendor?.addresses?.find { it.isDefault } ?: vendor?.addresses?.getAt(0)
                        if (displayAddr) {
                            p(class: 'hero-vendor-address-line', "${displayAddr.street1 ?: ''}")
                            p(class: 'hero-vendor-address-line', "${displayAddr.city ?: ''}, ${displayAddr.state ?: ''} ${displayAddr.zip ?: ''}")
                        } else {
                            p(class: 'hero-vendor-address-line', 'No address listed.')
                        }
                    }
                }
            }

            // Products grid
            div(class: 'vendor-products-section') {
                div(class: 'container-wide') {
                    div(class: 'product-grid') {
                        if (products) {
                            products.each { product ->
                                div(class: 'product-card') {
                                    a(href: "/shop/view/${product.id}", class: 'product-image-link') {
                                        img(src: ImageUrlUtil.resolve(product.imageUrl, imagesBaseUrl) ?: '/images/products/default.png', alt: product.name)
                                    }
                                    div(class: 'product-info') {
                                        div(class: 'product-title') {
                                            span(class: 'title-name', product.name)
                                            span(class: 'category-tag', product.categoryName)
                                        }
                                        p(class: 'product-card-description', product.description)

                                        if (product.stock != null && product.stock < (product.lowStockThreshold ?: 0)) {
                                            div(class: 'warning-wrapper') {
                                                span(class: 'low-stock-warn', "Low Stock! Only ${product.stock} left!")
                                            }
                                        } else {
                                            div(class: 'spacing-wrapper') {}
                                        }
                                    }

                                    div(class: 'product-card-footer') {
                                        span(class: 'price', "\$${product.price}")
                                        button(class: 'btn btn-small', onclick: "addToCart('${product.id}')", 'Add to Cart')
                                    }
                                }
                            }
                        } else {
                            p(class: 'no-products', 'This vendor has no products listed yet.')
                        }
                    }

                    if (totalPages > 1) {
                        div(class: 'pagination-container') {
                            (0..<totalPages).each { i ->
                                a(href: "/vendor/${vendor.id}?page=${i}",
                                        class: "page-link ${i == currentPage ? 'active' : ''}", i + 1)
                            }
                        }
                    }
                }
            }

            script(src: '/scripts/shop/cart-update.js') {}
        }
