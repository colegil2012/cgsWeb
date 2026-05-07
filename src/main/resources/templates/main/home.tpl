package templates.main

import com.ua.estore.cgsWeb.util.ImageUrlUtil

/*
 * Home page.
 *
 * Hero band with the floating logo + tagline + CTA, then a "specialties"
 * grid, then a "how it works" three-step strip. All styles live in either
 * the global system (main.css + components/) or pages/home.css. Nothing
 * inline.
 */

layout 'layout.tpl',
        title: 'CGS Web | Home',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/home.css') },
        content: {

            /* ---- Hero --------------------------------------------------- */
            section(class: 'hero') {
                div(class: 'hero-content') {
                    img(
                            src: ImageUrlUtil.resolve('/images/site-images/Celtech Transparent.png', imagesBaseUrl),
                            alt: 'Celtech General Store',
                            class: 'hero-logo float-in'
                    )
                    p(class: 'hero-tagline float-in is-delayed',
                            'Louisville\'s online farmers market — local, hand made, delivered.')
                    div(class: 'hero-actions float-in is-delayed-2') {
                        a(href: '/shop',    class: 'btn btn-brown btn-pill', 'Shop Now')
                    }
                }
            }

            /* ---- Specialties -------------------------------------------- */
            section(class: 'container') {
                div(class: 'home-specialties') {
                    [
                            [title: 'Locally Grown',         body: 'Sourced from Kentucky Farmers, Transparently Labeled. You know who grew or raised everything in-store and online.'],
                            [title: 'Reliably Sourced',  body: 'Celtech partners with Kentucky Farmers we trust, and if you don\'t trust us you can meet them for yourself.'],
                            [title: 'Affordable & Convenient', body: 'Available in store all week, or delivered to your door.'],
                    ].each { item ->
                        div(class: 'home-specialty-card') {
                            h3(item.title)
                            p(item.body)
                        }
                    }
                }
            }
        }