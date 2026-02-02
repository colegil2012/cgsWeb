package templates.shop;

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Shop',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        content: {
            div(class: 'hero') {
                h1('Shop')
                p('\'Fresh organic produce, herbs, and seasonings delivered from our soil to your kitchen.\'')
            }
            div(class: 'filter-container') {
                form(action: '/shop/filter', method: 'get', class: 'filter-form') {

                    div(class: 'filter-group') {
                        label(for: 'searchInput', 'Search')
                        input(type: 'text', id: 'searchInput', name: 'search', placeholder: 'Find a product...')
                    }

                    div(class: 'filter-group') {
                        label(for: 'categoryFilter', 'Filter by Category: ')
                        select(id: 'categoryFilter', name: 'category') {
                            option(value: '', 'All Categories')
                            categories.each { id, name ->
                                option(value: id, name)
                            }
                        }
                    }

                    div(class: 'filter-group') {
                        label(for: 'vendorFilter', 'Filter by Vendor: ')
                        select(id: 'vendorFilter', name: 'vendor') {
                            option(value: '', 'All Vendors')
                            vendors.each { vendor ->
                                option(value: vendor.id, vendor.name)
                            }
                        }
                    }

                    div(class: 'filter-group checkbox-group') {
                        label(for: 'lowStock', 'Low Stock!')
                        input(type: 'checkbox', id: 'lowStock', name: 'lowStock', value: 'true')
                    }

                    button(type: 'submit', class: 'btn-search') {
                        span('Apply')
                    }
                }
            }
            div(class: 'container') {
                div(class: 'product-grid') {
                    products.each { product ->
                        div(class: 'product-card') {
                            a(href: "/shop/view/${product.id}", 'class: product-image-link') {
                                img(src: ImageUrlUtil.resolve(product.imageUrl, imagesBaseUrl), alt: product.name)
                            }
                            div(class: 'product-info') {
                                div(class: 'product-title') {
                                    span(class: 'title-name', product.name)
                                    span(class: 'category', product.categoryName)
                                }
                                div(class: 'product-meta') {
                                    a(href: "/vendor/${product.vendorId}", class: 'vendor-tag', "By: ${product.vendorName}")
                                }
                                p(class: 'description', product.description)

                                if(product.stock < product.lowStockThreshold) {
                                    div(class: 'warning-wrapper') {
                                        span(class: 'low-stock-warn', "Low Stock! Only ${product.stock} left!")
                                    }
                                } else {
                                    div(class: 'spacing-wrapper') {

                                    }
                                }
                            }

                            div(class: 'product-footer') {
                                span(class: 'price', "\$${product.price}")
                                button(class: 'btn-small', onclick: "addToCart('${product.id}')", 'Add to Cart')
                            }
                        }
                    }
                }
                if (totalPages > 1) {
                    div(class: 'pagination-container') {
                        (0..<totalPages).each { p ->
                            a(href: "/shop/filter?page=${p}&search=${search ?: ''}&category=${category ?: ''}&vendor=${vendor ?: ''}&lowStock=${lowStock}",
                                    class: "page-link ${p == currentPage ? 'active' : ''}", p + 1)
                        }
                    }
                }
            }
            script(src: '/scripts/cart.js') {}
        }