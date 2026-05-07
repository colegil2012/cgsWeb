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
        title: 'CGS Web | Admin',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: { link(rel: 'stylesheet', href: '/css/pages/admin.css') },
        content: {

        }