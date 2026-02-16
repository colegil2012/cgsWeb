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

                //FORM for Signup new user
                form(action: '/signup/submit', method: 'post', class: 'form-group') {
                    div(class: 'form-control') {
                        label(for: 'username', 'Username')

                        div(class: 'input-row') {
                            input(type: 'text', name: 'username', id: 'username', required: 'required')
                            span(id: 'usernameStatus', '')
                        }

                        div(class: 'field-feedback', id: 'usernameFeedback', '')
                    }

                    div(class: 'form-control') {
                        label(for: 'password', 'Password')
                        input(type: 'password', name: 'password', id: 'password', required: 'required')
                    }

                    div(class: 'form-control') {
                        label(for: 'confirmPassword', 'Confirm Password')

                        div(class: 'input-row') {
                            input(type: 'password', name: 'confirmPassword', id: 'confirmPassword', required: 'required')
                            span(id: 'passwordStatus', '')
                        }

                        div(class: 'field-feedback', id: 'passwordFeedback', '')
                    }

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
                        label(for: 'phone', 'Phone Number')
                        input(
                                type: 'tel',
                                name: 'phone',
                                id: 'phone',
                                placeholder: '555-555-5555',
                                pattern: '^\\(\\d{3}\\)-\\d{3}-\\d{4}$',
                                maxlength: '14',
                                inputmode: 'numeric',
                                title: 'Enter a valid phone number (e.g., 555-555-5555)'
                        )
                        div(class: 'field-feedback', id: 'phoneFeedback', '')
                    }

                    div(class: 'form-control') {
                        label(for: 'email', 'Email')
                        input(
                                type: 'email',
                                name: 'email',
                                id: 'email',
                                placeholder: 'name@example.com'
                        )
                        div(class: 'field-feedback', id: 'emailFeedback', '')
                    }

                    button(id: 'signupSubmit', type: 'submit', class: 'btn', style: 'border:none; cursor:pointer;', disabled: 'disabled', 'Sign Up')
                }
            }
            script(src: '/scripts/signup.js') {}
        }