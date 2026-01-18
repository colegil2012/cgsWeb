package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        user: user,
        cartItems: cartItems,
        content: {
            div(class: 'hero') {
                h1('Celtech General Store')
                p('Helping to foster better relationships between Kentucy Citizens, Farmers and Crafters.')
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