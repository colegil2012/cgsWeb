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
                        a(href: '/shop',    class: 'btn btn-pill', 'Shop')
                    }
                }
            }

            /* ---- Specialties -------------------------------------------- */
            section(class: 'container') {
                h2(class: 'home-section-title', 'What we\'re known for')
                p(class: 'home-section-sub',
                        'Curated picks from the small farms and crafters in our circle.')

                div(class: 'home-specialties') {
                    [
                            [title: 'Fresh Herbs',         body: 'Cut-this-morning basil, mint, chamomile, and more — grown at small Kentucky farms.'],
                            [title: 'Organic Vegetables',  body: 'Heirloom tomatoes, salad greens, root veg — sourced within 50 miles of Louisville.'],
                            [title: 'Hand-ground Spices', body: 'Dried, milled, and blended in small batches — Aleppo, ashwagandha, and house seasonings.'],
                    ].each { item ->
                        div(class: 'home-specialty-card') {
                            h3(item.title)
                            p(item.body)
                        }
                    }
                }
            }

            /* ---- How it works ------------------------------------------- */
            section(class: 'container') {
                h2(class: 'home-section-title', 'How it works')
                div(class: 'home-steps') {
                    [
                            [num: '1', title: 'Browse',  body: 'Shop products from local farms, crafters, and specialty makers.'],
                            [num: '2', title: 'Order',   body: 'Pick a delivery day and address — we batch routes to keep shipping low.'],
                            [num: '3', title: 'Receive', body: 'A driver delivers within our 50-mile zone, usually next-day.'],
                    ].each { step ->
                        div(class: 'home-step') {
                            span(class: 'home-step-number', step.num)
                            div(class: 'home-step-body') {
                                h4(step.title)
                                p(step.body)
                            }
                        }
                    }
                }
            }
        }