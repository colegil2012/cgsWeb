package templates.main

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Home',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        imagesBaseUrl: imagesBaseUrl,
        headContent: { link(rel: 'stylesheet', href: '/css/pages/home.css') },
        content: {
            div(class: 'hero') {
                img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Transparent.png', imagesBaseUrl), alt: 'Celtech Logo', class: 'logo-image')
            }
            div(class: 'shop-btn-container') {
                a(href: '/shop', class: 'btn', 'Explore Shop')
            }
            div(class: 'container') {
                section {
                    h2('Our Specialties')
                    div(class: 'specialties-grid') {
                        ['Fresh Herbs', 'Organic Vegetables', 'Hand-ground Spices'].each { item ->
                            div(class: 'specialty-card') {
                                h3(item)
                                p("Quality ${item.toLowerCase()} grown with sustainable practices.")
                            }
                        }
                    }
                }
            }
        }