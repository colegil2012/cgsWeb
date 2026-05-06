package templates

import com.ua.estore.cgsWeb.util.ImageUrlUtil

yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(title ?: 'CGS Web | Louisville\'s Online Farmers Market')

        // CSRF meta tags — JavaScript reads these to send X-XSRF-TOKEN on fetch POSTs
        meta(name: '_csrf', content: (csrfToken ?: ''))
        meta(name: '_csrf_header', content: (csrfHeaderName ?: 'X-CSRF-TOKEN'))
        meta(name: '_csrf_parameter', content: (csrfParamName ?: '_csrf'))

        //Global Styles
        link(rel: 'stylesheet', href: '/css/main.css')

        //Global Scripts
        script(src: '/scripts/_modal.js') {}
        script(src: '/scripts/_csrf.js') {}

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
        link(rel: 'stylesheet', href: '/css/components/spinning-leaves.css')

        if(headContent != null) {
            headContent()
        }
    }
    body {
        header {
            div(class: 'logo') {
                img(src: ImageUrlUtil.resolve('/images/site-images/CGS Logo.png', imagesBaseUrl),
                        alt: 'Celtech Logo',
                        class: 'header-image')
            }
            nav {
                ul {
                    li { a(href: '/', 'Home') }

                    // Shop and Vendors are now public (guests can browse)
                    li(class: 'nav-dropdown') {
                        a(href: '#', class: 'nav-dropdown-toggle', 'Shop')
                        ul(class: 'nav-dropdown-menu') {
                            li { a(href: '/shop', 'Shop') }
                            li { a(href: '/vendors', 'Vendors') }
                        }
                    }

                    if (user) {
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

                    // Cart is visible for EVERYONE (guest or auth'd)
                    li(id: 'cart-link-container') {
                        a(href: '/cart', class: 'cart-link') {
                            yield "Cart("
                            span(class: 'cart-count', cartCount ?: 0)
                            yield ")"
                        }
                    }

                    if (user) {
                        // Logout is a POST in Spring Security — wrap in a tiny form
                        li {
                            form(action: '/logout', method: 'post', style: 'display:inline;') {
                                input(
                                        type: 'hidden',
                                        name: (csrfParamName ?: '_csrf'),
                                        value: (csrfToken ?: '')
                                )
                                button(type: 'submit', class: 'logout-link',
                                        style: 'background:none;border:none;padding:0;font:inherit;cursor:pointer;color:#c62828;font-weight:bold;',
                                        'Logout')
                            }
                        }
                    } else {
                        li { a(href: '/login', 'Sign In') }
                    }
                }
            }
        }
        main {
            content()
        }
    }
}