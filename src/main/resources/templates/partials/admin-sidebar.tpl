package templates.partials

import com.ua.estore.cgsWeb.util.ImageUrlUtil

/*
 * Admin sidebar navigation.
 *
 * Each admin page sets `activeSection` on the model to one of:
 *   dashboard | users | vendors | products | routes | orders
 *
 * Round 2: every section is functional. No coming-soon placeholders left.
 */

aside(class: 'admin-sidebar') {
    div(class: 'admin-sidebar-header') {
        img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Transparent.png', imagesBaseUrl),
                alt: 'Celtech General Store',
                class: 'admin-sidebar-logo'
        )
        span(class: 'admin-sidebar-badge', 'ADMIN')
    }
    nav(class: 'admin-sidebar-nav') {
        ul {
            li {
                a(href: '/admin',
                        class: 'admin-sidebar-link' + (activeSection == 'dashboard' ? ' active' : ''),
                        'Dashboard')
            }
            li {
                a(href: '/admin/users',
                        class: 'admin-sidebar-link' + (activeSection == 'users' ? ' active' : ''),
                        'Users')
            }
            li {
                a(href: '/admin/vendors',
                        class: 'admin-sidebar-link' + (activeSection == 'vendors' ? ' active' : ''),
                        'Vendors')
            }
            li {
                a(href: '/admin/products',
                        class: 'admin-sidebar-link' + (activeSection == 'products' ? ' active' : ''),
                        'Products')
            }
            li {
                a(href: '/admin/routes',
                        class: 'admin-sidebar-link' + (activeSection == 'routes' ? ' active' : ''),
                        'Routes')
            }
            li {
                a(href: '/admin/orders',
                        class: 'admin-sidebar-link' + (activeSection == 'orders' ? ' active' : ''),
                        'Orders')
            }
        }
    }
}
