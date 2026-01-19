package templates.shop

layout 'layout.tpl',
        title: vendor?.name ?: 'Vendor Profile',
        user: user,
        cartItems: cartItems,
        content: {
            div(class: 'vendor-profile-container') {
                // Vendor Header Section
                div(class: 'vendor-header-card') {
                    div(class: 'vendor-header-logo') {
                        img(src: vendor?.logo_url ?: '/images/site-images/default-vendor.png', alt: vendor?.name)
                    }
                    div(class: 'vendor-header-info') {
                        h1(class: 'vendor-name', vendor?.name)
                        p(class: 'vendor-bio', vendor?.description)
                        div(class: 'vendor-location') {
                            span(class: 'location-label', 'Find us at:')
                            p(class: 'address-line', "${vendor?.address_1}${vendor?.address_2 ? ', ' + vendor.address_2 : ''}")
                            p(class: 'address-line', "${vendor?.city}, ${vendor?.state} ${vendor?.zip}")
                        }
                    }
                }

                // Products Section
                div(class: 'vendor-products-section') {
                    div(class: 'product-header-container') {
                        h2 "Products from ${vendor?.name}"
                    }
                    div(class: 'product-grid') {
                        if (products) {
                            products.each { product ->
                                div(class: 'product-card') {
                                    a(href: "/shop/view/${product.id}", class: 'product-image-link') {
                                        img(src: product.imageUrl ?: '/images/products/default.png', alt: product.name)
                                    }
                                    div(class: 'product-info') {
                                        div(class: 'product-title') {
                                            p(class: 'title-name', product.name)
                                            span(class: 'category', product.category)
                                        }
                                        p(class: 'description', product.description)

                                    }

                                    div(class: 'product-footer') {
                                        span(class: 'price', "\$${product.price}")
                                        button(class: 'btn-small', onclick: "addToCart('${product.id}')", 'Add to Cart')
                                    }
                                }
                            }
                        } else {
                            p(class: 'no-products', 'This vendor has no products listed yet.')
                        }
                    }
                    div(class: 'pagination-container') {
                        if (totalPages > 1) {
                            (0..<totalPages).each { i ->
                                a(href: "/vendor/${vendor.id}?page=${i}",
                                        class: "page-link ${i == currentPage ? 'active' : ''}", i + 1)
                            }
                        }
                    }
                }
            }
            script(src: '/scripts/cart.js') {}
        }