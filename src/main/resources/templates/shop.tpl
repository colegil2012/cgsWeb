package templates;

layout 'layout.tpl',
        title: 'CGS Web | Shop',
        username: username,
        cartItems: cartItems,
        role: role,
        content: {
            div(class: 'hero') {
                h1('Shop')
                p('\'Fresh organic produce, herbs, and seasonings delivered from our soil to your kitchen.\'')
            }
            div(class: 'filter-container') {
                form(action: '/shop/filter', method: 'get', class: 'filter-form') {
                    div(class: 'filter-group') {
                        label(for: 'categoryFilter', 'Filter by Category: ')
                        select(id: 'categoryFilter', name: 'category') {
                            option(value: '', selected: (lastCategory == null || lastCategory == ''), 'All Categories')
                            categories.each { cat ->
                                option(value: cat, cat)
                            }
                        }
                    }

                    div(class: 'filter-group') {
                        label(for: 'searchInput', 'Search')
                        input(type: 'text', id: 'searchInput', name: 'search', value: lastSearch ?: '', placeholder: 'Find a product...')
                    }

                    div(class: 'filter-group checkbox-group') {
                        label(for: 'lowStock', 'Low Stock!')
                        input(type: 'checkbox', id: 'lowStock', name: 'lowStock', value: 'true')
                    }

                    a(href: '/shop', class: 'btn-clear') {
                        span('Clear Filters')
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
                            img(src: product.imageUrl ?: '/images/placeholder.jpg', alt: product.name)
                            div(class: 'product-info') {
                                div (class: 'product-title') {
                                    span(class: 'title-name', product.name)
                                    span(class: 'category', product.category)
                                }
                                div (class: 'product-meta') {
                                    span(class: 'vendor-tag', "By: ${product.vendorName}")
                                }
                                p(class: 'description', product.description)
                                div(class: 'product-footer') {
                                    span(class: 'price', "\$${product.price}")
                                    a(href: "/cart/add/${product.id}", class: 'btn-small', 'Add to Cart')
                                }
                            }
                        }
                    }
                }
            }
            script(src: '/scripts/cart.js') {}
        }