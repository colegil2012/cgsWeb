package templates;

layout 'layout.tpl',
        title: 'CGS Web | Shop',
        content: {
            div(class: 'hero') {
                h1('Shop')
                p('\'Fresh organic produce, herbs, and seasonings delivered from our soil to your kitchen.\'')
            }
            div(class: 'container') {
                div(class: 'product-grid') {
                    products.each { product ->
                        div(class: 'product-card') {
                            img(src: product.imageUrl ?: '/images/placeholder.jpg', alt: product.name)
                            div(class: 'product-info') {
                                h3(product.name)
                                p(class: 'category', product.category)
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
        }