package templates.admin

/*
 * Generic "coming soon" page for admin sections not yet built.
 *
 * Bindings:
 *   activeSection      — drives sidebar highlight
 *   sectionTitle       — h1 text (e.g. "Vendors")
 *   sectionDescription — body paragraph
 *
 * Used by AdminController for vendors/routes/orders until Round 1B/2 ships.
 */

layout 'layout.tpl',
        title: 'CGS Web | Admin · ' + (sectionTitle ?: 'Coming Soon'),
        user: user,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/admin.css')
        },
        content: {
            div(class: 'admin-shell') {
                include template: 'partials/admin-sidebar.tpl'

                section(class: 'admin-content') {
                    header(class: 'admin-page-header') {
                        h1(class: 'admin-page-title', sectionTitle ?: 'Coming Soon')
                    }

                    div(class: 'admin-coming-soon') {
                        div(class: 'admin-coming-soon-badge', 'Coming Soon')
                        p(class: 'admin-coming-soon-desc',
                                sectionDescription ?: 'This section is being built and will land in an upcoming round.')
                        a(href: '/admin', class: 'admin-btn admin-btn-secondary', '← Back to Dashboard')
                    }
                }
            }
        }
