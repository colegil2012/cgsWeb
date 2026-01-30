package templates.main

layout 'layout.tpl',
        title: 'CGS Web | Sign Up',
        content: {
            div(class: 'hero') {
                h1('New User')
                p('Register now to start purchasing fresh local produce.')
            }
            div(class: 'signup-container') {
                if (error) div(class: 'alert alert-error', error)
                if (message) div(class: 'alert alert-success', message)

                form(action: '/signup/submit', method: 'post', class: 'form-group') {
                    div(class: 'form-control') {
                        label(for: 'firstName', 'First Name')
                        input(type: 'text', name: 'firstName', id: 'firstName', required: 'required')
                    }
                    div(class: 'form-control') {
                        label(for: 'middleInit', 'MI')
                        input(type: 'text', name: 'middleInit', id: 'middleInit')
                    }
                    div(class: 'form-control') {
                        label(for: 'lastName', 'Last Name')
                        input(type: 'text', name: 'lastName', id: 'lastName', required: 'required')
                    }

                    div(class: 'form-control') {
                        label(for: 'phone', 'Phone Number (Opt.)')
                        input(
                                type: 'tel',
                                name: 'phone',
                                id: 'phone',
                                placeholder: '555-555-5555',
                                pattern: '^\\+?1?[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$',
                                title: 'Enter a valid phone number (e.g., 555-555-5555)'
                        )
                        div(class: 'field-feedback', id: 'phoneFeedback', '')
                    }

                    div(class: 'form-control') {
                        label(for: 'email', 'Email (Opt.)')
                        input(
                                type: 'email',
                                name: 'email',
                                id: 'email',
                                placeholder: 'name@example.com'
                        )
                        div(class: 'field-feedback', id: 'emailFeedback', '')
                    }

                    div(class: 'form-control') {
                        label(for: 'username', 'Username')
                        input(type: 'text', name: 'username', id: 'username', required: 'required')
                    }
                    div(class: 'form-control') {
                        label(for: 'password', 'Password')
                        input(type: 'password', name: 'password', id: 'password', required: 'required')
                    }
                    div(class: 'form-control') {
                        label(for: 'confirmPassword', 'Confirm Password')
                        input(type: 'password', name: 'confirmPassword', id: 'confirmPassword', required: 'required')
                    }
                    button(type: 'submit', class: 'btn', style: 'border:none; cursor:pointer;', 'Sign Up')
                }

                script(src: '/scripts/signup.js')
            }
        }