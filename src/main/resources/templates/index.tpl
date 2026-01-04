package templates

layout 'layout.tpl',
        title: 'CGS Web | Home',
        content: {
            div(class: 'hero') {
                h1('Cole\'s General Store')
                p('Fresh organic produce, herbs, and seasonings delivered from our soil to your kitchen.')
                a(href: '/shop', class: 'btn', 'Explore Shop')
            }
            div(class: 'container') {
                section {
                    h2('Our Specialties')
                    div(style: 'display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 2rem;') {
                        ['Fresh Herbs', 'Organic Vegetables', 'Hand-ground Spices'].each { item ->
                            div(style: "background: var(--primary-green); padding: 2rem; border-radius: 10px; border-left: 5px solid var(--earth-brown);") {
                                h3(item)
                                p("Quality ${item.toLowerCase()} grown with sustainable practices.")
                            }
                        }
                    }
                }
            }
        }