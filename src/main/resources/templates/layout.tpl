package templates

yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(title ?: 'CGS Web | Louisville\'s Online Farmers Market')

        //Global Styles
        link(rel: 'stylesheet', href: '/css/main.css')

        //Component Styles
        link(rel: 'stylesheet', href: '/css/components/nav.css')
        link(rel: 'stylesheet', href: '/css/components/buttons.css')
        link(rel: 'stylesheet', href: '/css/components/forms.css')
        link(rel: 'stylesheet', href: '/css/components/cards.css')
        link(rel: 'stylesheet', href: '/css/components/modal.css')
        link(rel: 'stylesheet', href: '/css/components/alerts.css')
        link(rel: 'stylesheet', href: '/css/components/animations.css')
        link(rel: 'stylesheet', href: '/css/components/pagination.css')
        link(rel: 'stylesheet', href: '/css/components/mobile.css')

        if(headContent != null) {
            headContent()
        }
    }
    body {
        header {
            div(class: 'logo', 'Celtech GS')
            if (user) {
                span(class: 'welcome-message', "Welcome, ${user.profile.firstName}!")
            }
            nav {
                ul {
                    li { a(href: '/', 'Home') }

                    if (user) {
                        li(class: 'nav-dropdown') {
                            a(href: '#', class: 'nav-dropdown-toggle', 'Shop')
                            ul(class: 'nav-dropdown-menu') {
                                li { a(href: '/shop', 'Shop') }
                                li { a(href: '/vendors', 'Vendors') }
                            }
                        }

                        li(class: 'nav-dropdown') {
                            a(href: '#', class: 'nav-dropdown-toggle', 'Manage')
                            ul(class: 'nav-dropdown-menu') {
                                li { a(href: '/account', 'My Account') }
                                if ( user?.roles?.contains('ADMIN')) {
                                    li { a(href: '/admin', 'Admin Portal') }
                                }
                                if ( user?.roles?.contains('VENDOR')) {
                                    li { a(href: '/vendor/portal', 'Vendor Portal') }
                                }
                            }
                        }
                    }

                    li { a(href: '/about', 'About') }
                    if (user) {
                        li(id: 'cart-link-container') {
                            a(href: '/cart', class: 'cart-link') {
                                yield "Cart("
                                span(class: 'cart-count', cartCount ?: 0)
                                yield ")"
                            }
                        }
                        li { a(href: '/logout', class: 'logout-link', 'Logout') }
                    }
                }
            }
        }
        main {
            content()
        }
    }
}
