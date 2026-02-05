package templates.shop

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Secure Checkout',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        headContent: {
            script(src: 'https://sandbox.web.squarecdn.com/v1/square.js') {}
        }
        content: {
            div(id: 'payment-form') {
                div(id: 'card')
                button(id: 'pay', 'Pay')
            }
        }