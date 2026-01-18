package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        user: user,
        cartItems: cartItems,
        totalPrice: totalPrice,
        content: {
            //Calculate total, subtotal and tax
            def subtotal = cartItems.collect { it.price * it.quantity }.sum() ?: 0.00
            def tax = subtotal * 0.07 // Example 7% tax
            def total = subtotal + tax

            if(cartItems == null || cartItems.isEmpty()) {
                div(class: 'empty-cart-message') {
                    h3('Your cart is empty!')
                    p('It looks like you haven\'t added any products to your cart yet.')
                    a(href: '/shop', class: 'btn-search', 'Start Shopping')
                }
            } else {

                div(class: 'cart-page-container') {

                    div(class: 'cart-main') {
                        div(class: 'cart-header') {
                            span(class: 'cart-header-text', "${user.username.toUpperCase()}'s Cart:")
                        }
                        div(class: 'cart-items') {
                            div(class: 'cart-grid') {
                                cartItems.each { item ->
                                    div(class: 'cart-card') {
                                        div(class: "cart-card-section") {
                                            img(src: item.imageUrl ?: '/images/placeholder.jpg', alt: item.name)
                                        }
                                        div(class: "cart-card-section") {
                                            p(class: 'cart-product-name', item.name)
                                            p(class: 'cart-product-vendor', "By: ${item.vendorName}")
                                        }
                                        div(class: "cart-card-section") {
                                            p(class: 'cart-product-price', item.price)
                                        }
                                        div(class: "cart-card-section quantity-controls") {
                                            a(href: "/cart/remove/${item.id}", class: 'btn-qty minus', '-')
                                            span(class: 'cart-product-qy', item.quantity)
                                            a(href: "/cart/add/${item.id}", class: 'btn-qty plus', '+')
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(class: 'cart-summary') {
                        h2('Order Summary')
                        div(class: 'summary-details') {
                            div(class: 'summary-row') {
                                span('Subtotal')
                                span(class: 'summary-value', "\$${String.format('%.2f', subtotal)}")
                            }
                            div(class: 'summary-row') {
                                span('Estimated Tax')
                                span(class: 'summary-value', "\$${String.format('%.2f', tax)}")
                            }
                            hr()
                            div(class: 'summary-row total') {
                                span('Total')
                                span(class: 'summary-value', "\$${String.format('%.2f', total)}")
                            }
                        }
                        div(class: 'payment-methods') {
                            p('Accepted Payments:')
                            div(class: 'payment-icons') {
                                span(class: 'payment-badge', 'Visa')
                                span(class: 'payment-badge', 'MasterCard')
                                span(class: 'payment-badge', 'PayPal')
                            }
                        }
                        button(class: 'btn-checkout', 'Proceed to Checkout')
                    }
                }
            }
            script(src: '/scripts/cart-update.js') {}
        }

