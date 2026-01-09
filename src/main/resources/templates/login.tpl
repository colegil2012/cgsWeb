package templates

layout 'layout.tpl',
        title: 'CGS Web | Login',
        content: {
            div(class: 'hero') {
                h1('Welcome Back')
                p('Please sign in to access the general store.')
            }
            div(class: 'container', style: 'max-width: 400px; margin: 0 auto;') {
                if (error) {
                    div(style: 'background: #ffcdd2; color: #c62828; padding: 1rem; border-radius: 5px; margin-bottom: 1rem;', error)
                }
                if (message) {
                    div(style: 'background: #c8e6c9; color: #2e7d32; padding: 1rem; border-radius: 5px; margin-bottom: 1rem;', message)
                }
                form(action: '/login', method: 'post', style: 'display: flex; flex-direction: column; gap: 1rem;') {
                    div {
                        label(for: 'username', style: 'display: block; margin-bottom: 0.5rem;', 'Username')
                        input(type: 'text', name: 'username', id: 'username', required: 'required',
                                style: 'width: 100%; padding: 0.8rem; border: 1px solid var(--soft-brown); border-radius: 5px;')
                    }
                    div {
                        label(for: 'password', style: 'display: block; margin-bottom: 0.5rem;', 'Password')
                        input(type: 'password', name: 'password', id: 'password', required: 'required',
                                style: 'width: 100%; padding: 0.8rem; border: 1px solid var(--soft-brown); border-radius: 5px;')
                    }
                    button(type: 'submit', class: 'btn', style: 'width: 100%; cursor: pointer; border: none;', 'Sign In')
                }
            }
        }