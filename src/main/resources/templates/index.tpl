package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        username: username,
        cart: cartItems,
        content: {
            div(class: 'hero') {
                h1('Cole\'s General Store')
                p('Fresh organic produce, herbs, and seasonings delivered from our soil to your kitchen.')
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