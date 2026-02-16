package templates.shop

import com.ua.estore.cgsWeb.models.dto.product.ProductDTO
import com.ua.estore.cgsWeb.util.ImageUrlUtil;

modelTypes = [
        selected_product: ProductDTO
]

layout 'layout.tpl',
        title: 'CGS Web | View Product',
        user: user,
        cartItems: cartItems,
        cartCount: cartCount,
        selected_product: selected_product,
        content: {
            div(class: 'container') {
                a(
                        href: (backLinkHref ?: '/shop'),
                        class: 'btn-small',
                        (backLinkText ?: '← Back to Shop')
                )

                div(class: 'product-view-container') {
                    div(class: 'product-view-image') {
                        img(src: ImageUrlUtil.resolve(selected_product.imageUrl, imagesBaseUrl) ?: '/images/placeholder.jpg', alt: selected_product.name, style: 'width: 100%; border-radius: 8px;')
                    }

                    div(class: 'product-view-details') {
                        span(class: 'category-tag', selected_product.categoryName)
                        span(class: 'name-tag', selected_product.name)
                        p(class: 'vendor-text') {
                            yield "Supplied by "
                            a(href: "/vendor/${selected_product.vendorId}") { strong(selected_product.vendorName) }
                        }

                        hr()

                        p(class: 'product-description', selected_product.description)

                        div(class: 'product-footer') {
                            span(class: 'price-large', "\$${selected_product.price}")
                            if(selected_product.stock != null && selected_product.stock < selected_product.lowStockThreshold) {
                                div(class: 'warning-wrapper') {
                                    span(class: 'low-stock-warn', "Low Stock! Only ${selected_product.stock} left!")
                                }
                            }

                            button(class: 'btn-small', onclick: "addToCart('${selected_product.id}')", 'Add to Cart')
                        }
                    }
                }
            }
            // Include the same cart script used in shop.tpl
            script(src: '/scripts/cart/cart.js') {}
        }