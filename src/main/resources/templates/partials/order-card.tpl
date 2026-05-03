package templates.partials

import com.ua.estore.cgsWeb.util.ImageUrlUtil
import java.time.format.DateTimeFormatter

/*
 * Renders ONE row in the user's order history. Used by the Orders tab in
 * account.tpl. Bindings:
 *   order         – Order document (with totals + items snapshotted)
 *   imagesBaseUrl – DO Spaces base, or local /images
 *
 * Uses the same status pill class names as a future driver-dash admin view,
 * so the styling lives in pages/account.css and can be lifted out later.
 */

def DATE_FMT = DateTimeFormatter.ofPattern('MMM d, yyyy')

def statusClass = (order?.status?.name() ?: 'PENDING').toLowerCase()
def statusLabel = (order?.status?.name() ?: 'PENDING')
        .toLowerCase()
        .replace('_', ' ')

def itemCount = order?.items?.size() ?: 0
def totalDisplay = order?.totals?.total != null
        ? String.format('%.2f', order.totals.total)
        : '0.00'

def thumbnail = order?.items && !order.items.isEmpty() && order.items[0]?.imageUrl
        ? ImageUrlUtil.resolve(order.items[0].imageUrl, imagesBaseUrl)
        : '/images/placeholder.jpg'

div(class: 'order-card') {

    div(class: 'order-card-thumb') {
        img(src: thumbnail, alt: '')
        if (itemCount > 1) {
            span(class: 'order-card-extra-count', "+${itemCount - 1}")
        }
    }

    div(class: 'order-card-body') {
        div(class: 'order-card-header') {
            strong(class: 'order-card-number', "Order #${order?.orderNumber ?: ''}")
            span(class: "order-status-pill order-status-${statusClass}",
                    statusLabel.toUpperCase())
        }

        div(class: 'order-card-meta') {
            span(class: 'order-card-date',
                    "Placed ${order?.placedAt?.format(DATE_FMT) ?: '—'}")
            span(class: 'order-card-itemcount',
                    itemCount == 1 ? '1 item' : "${itemCount} items")
        }
    }

    div(class: 'order-card-side') {
        span(class: 'order-card-total', "\$${totalDisplay}")
        a(href: "/checkout/confirmation/${order?.id}",
                class: 'btn btn-small order-card-link',
                'View receipt')
    }
}