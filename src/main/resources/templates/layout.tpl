package templates

yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(title ?: 'CGS Web - Organic Produce')
        link(rel: 'stylesheet', href: '/css/style.css')
    }
    body {
        header {
            div(class: 'logo', 'CGS Organic')
            if (username) {
                span(class: 'welcome-message', "Welcome, ${username}")
            }
            nav {
                ul {
                    li { a(href: '/', 'Home') }
                    li { a(href: '/shop', 'Shop') }
                    if (username) {
                        li { a(href: '/account', 'My Account')}
                    }
                    if (role == 'ADMIN') {
                        li { a(href: '/admin', 'Admin Portal')}
                    }
                    if (role == 'VENDOR') {
                        li { a(href: '/vendor', 'Vendor Portal')}
                    }
                    if (username) {
                        li(id: 'cart-link-container') { a(href: '/cart', "Cart(${cartItems?.sum { it.quantity } ?: 0})") }
                        li { a(href: '/logout', class: 'logout-link', 'Logout') }
                    }
                    li { a(href: '/about', 'About') }
                }
            }
        }
        main {
            content()
        }
    }
}
