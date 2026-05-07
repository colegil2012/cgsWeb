package templates.partials

/*
 * Site footer. Included from layout.tpl on every page.
 *
 * Three-column structure:
 *   - Brand column:   text logo + tagline + small contact bit
 *   - Shop column:    primary navigation links (Shop, Vendors, About)
 *   - Account column: account-related links + a small mailto link
 *
 * Copyright bar sits below the columns with a thin top divider.
 *
 * Notes on accessibility / SEO:
 *   - Wrapped in <footer> with role="contentinfo" so screen readers
 *     identify the region cleanly.
 *   - Heading levels (h4) chosen to sit below an h1 hero / h2 page
 *     headers without collision.
 *   - Mailto and external links use rel="noopener" where applicable.
 *
 * Bindings expected from the host layout:
 *   imagesBaseUrl  — DO Spaces base url for prod, or local /images
 */

import com.ua.estore.cgsWeb.util.ImageUrlUtil

def thisYear = java.time.LocalDate.now().getYear()

footer(class: 'site-footer', role: 'contentinfo') {

    div(class: 'site-footer-inner') {

        /* ---- Brand column ----------------------------------------------- */
        div(class: 'site-footer-column site-footer-brand') {
            div(class: 'site-footer-logo') {
                img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Text Logo Middle.png', imagesBaseUrl),
                        alt: 'Celtech General Store')
            }
            p(class: 'site-footer-tagline',
                    'Locally-sourced goods from Kentucky farms and crafters, delivered.')
            p(class: 'site-footer-contact') {
                yield 'Questions? '
                a(href: 'mailto:cole@celtechgs.com', 'cole@celtechgs.com')
            }
        }

        /* ---- Shop column ------------------------------------------------ */
        div(class: 'site-footer-column') {
            h4('Shop')
            ul(class: 'site-footer-links') {
                li { a(href: '/shop', 'All Products') }
                li { a(href: '/vendors', 'Our Vendors') }
                li { a(href: '/about', 'About Celtech') }
            }
        }

        /* ---- Account column --------------------------------------------- */
        div(class: 'site-footer-column') {
            h4('Account')
            ul(class: 'site-footer-links') {
                li { a(href: '/account', 'My Account') }
                li { a(href: '/account?tab=orders', 'Order History') }
                li { a(href: '/login', 'Log In') }
                li { a(href: '/signup', 'Create Account') }
            }
        }
    }

    /* ---- Copyright bar -------------------------------------------------- */
    div(class: 'site-footer-bottom') {
        p {
            yield "\u00a9 ${thisYear} Celtech General Store. "
            span(class: 'site-footer-bottom-divider', '\u00b7')
            yield ' Kentucky-grown, Kentucky-served.'
        }
    }
}
