package templates.main

layout 'layout.tpl',
        title: 'CGS Web | Login',
        content: {
            div(class: 'hero') {
                h1('Welcome Back')
                p('Please sign in to access the general store.')
            }
            div(class: 'auth-container') {
                if (error) div(class: 'alert alert-error', error)
                if (message) div(class: 'alert alert-success', message)

                form(action: '/login', method: 'post', class: 'form-group') {
                    div(class: 'form-control') {
                        label(for: 'username', 'Username')
                        input(type: 'text', name: 'username', id: 'username', required: 'required')
                    }
                    div(class: 'form-control') {
                        label(for: 'password', 'Password')
                        input(type: 'password', name: 'password', id: 'password', required: 'required')
                    }
                    button(type: 'submit', class: 'btn', style: 'border:none; cursor:pointer;', 'Sign In')
                }
            }
        }