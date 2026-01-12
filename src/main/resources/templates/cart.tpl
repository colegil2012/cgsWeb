package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        username: username,
        cart: cartItems,
        content: {
            div(class: 'cart-header') {
                span(class: 'cart-header', "${username}'s Cart:")
            }
            div(class: 'cart') {
                div(class: 'cart-grid')
                cartItems.each { product ->
                    div(class: 'cart-card') {
                        img(src: product.imageUrl ?: '/images/placeholder.jpg', alt: product.name)
                        p(class: 'cart-product-name', product.name)
                        p(class: 'cart-product-id', product.id)
                        p(class: 'cart-product-price', product.price)
                    }
                }
            }
        }
