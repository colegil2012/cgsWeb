yield "CELTECH GENERAL STORE\n"
yield "===========================================\n\n"

yield "Thank you, ${order.customer?.firstName ?: 'friend'}!\n"
yield "Your order #${order.orderNumber} was placed on ${placedAt}.\n\n"

yield "ITEMS\n"
yield "-------------------------------------------\n"
order.items.each { item ->
    yield "${item.name}"
    if (item.vendorName) { yield " (${item.vendorName})" }
    yield " x${item.quantity}"
    yield "  \$${fmt.apply(item.lineTotal)}\n"
}

yield "\nTOTALS\n"
yield "-------------------------------------------\n"
yield "Subtotal:        \$${fmt.apply(order.totals?.subtotal)}\n"
yield "Shipping:        \$${fmt.apply(order.totals?.shipping)}\n"
yield "Estimated Tax:   \$${fmt.apply(order.totals?.tax)}\n"
yield "Total:           \$${fmt.apply(order.totals?.total)}\n\n"

if (order.shipTo) {
    yield "DELIVERING TO\n"
    yield "-------------------------------------------\n"
    yield "${order.customer?.firstName ?: ''} ${order.customer?.lastName ?: ''}\n".trim() + "\n"
    yield "${order.shipTo.street1 ?: ''}\n"
    if (order.shipTo.street2) { yield "${order.shipTo.street2}\n" }
    yield "${order.shipTo.city ?: ''}, ${order.shipTo.state ?: ''} ${order.shipTo.zip ?: ''}\n\n"
}

if (order.deliveryInstructions) {
    yield "Instructions: ${order.deliveryInstructions}\n\n"
}

yield "View your receipt: ${orderUrl}\n\n"
yield "Questions? Reply to this email or write to cole@celtechgs.com.\n"
yield "Celtech General Store · celtechgs.com\n"