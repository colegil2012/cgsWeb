package templates

layout 'layout.tpl',
        title: 'CGS Web | Vendor',
        username: username,
        role: role,
        cartItems: cartItems,
        content: {
                div(class: 'container') {
                        h1('Admin Dashboard')
                        h2('Add New Products')
                }
        }

