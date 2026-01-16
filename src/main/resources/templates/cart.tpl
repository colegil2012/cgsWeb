package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        username: username,
        cartItems: cartItems,
        content: {
            div(class: 'cart-page-container') {

                div(class: 'cart-main') {
                    div(class: 'cart-header') {
                        span(class: 'cart-header-text', "${username.toUpperCase()}'s Cart:")
                    }

                    div(class: 'cart-items') {
                        div(class: 'cart-grid') {
                            cartItems.each { item ->
                                div(class: 'cart-card') {
                                    img(src: item.product.imageUrl ?: '/images/placeholder.jpg', alt: item.product.name)
                                    p(class: 'cart-product-name', item.product.name)
                                    p(class: 'cart-product-price', item.product.price)
                                    p(class: 'cart-product-qy', item.quantity)
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
                            span(class: 'summary-value', '$0.00') // Logic for calculation can be added later
                        }
                        div(class: 'summary-row') {
                            span('Estimated Tax')
                            span(class: 'summary-value', '$0.00')
                        }
                        hr()
                        div(class: 'summary-row total') {
                            span('Total')
                            span(class: 'summary-value', '$0.00')
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

