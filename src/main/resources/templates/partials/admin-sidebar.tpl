package templates.partials

/*
 * Admin sidebar navigation.
 *
 * Each admin page sets `activeSection` on the model to one of:
 *   dashboard | users | vendors | products | routes | orders
 *
 * Round 1B: Users, Vendors, Products are functional. Routes/Orders link
 * to the coming-soon placeholder served by AdminController.
 */

aside(class: 'admin-sidebar') {
    div(class: 'admin-sidebar-header') {
        span(class: 'admin-sidebar-badge', 'ADMIN')
        h2(class: 'admin-sidebar-title', 'Portal')
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
