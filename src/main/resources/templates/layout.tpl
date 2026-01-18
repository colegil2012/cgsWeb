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
            if (user) {
                span(class: 'welcome-message', "Welcome, ${user.username}!")
            }
            nav {
                ul {
                    li { a(href: '/', 'Home') }
                    if (user) {
                        li { a(href: '/shop', 'Shop') }
                        li { a(href: '/account', 'My Account')}
                    }
                    if (user?.role == 'ADMIN') {
                        li { a(href: '/admin', 'Admin Portal')}
                    }
                    if (user?.role == 'VENDOR') {
                        li { a(href: '/vendor', 'Vendor Portal')}
                    }
                    if (user) {
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
