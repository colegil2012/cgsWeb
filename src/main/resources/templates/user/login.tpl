package templates.user

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Login',
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: 'css/pages/login.css')
        },
        content: {
            div(class: 'hero') {
                div(class: 'hero-content') {
                    img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Transparent.png', imagesBaseUrl),
                            alt: 'Celtech Logo',
                            class: 'hero-logo float-in')
                }
            }
            div(class: 'auth-container') {
                if (error) div(class: 'alert alert-error', error)
                if (message) div(class: 'alert alert-success', message)

                form(action: '/login', method: 'post', class: 'form-group') {
                    input(
                            type: 'hidden',
                            name: (csrfParamName ?: '_csrf'),
                            value: (csrfToken ?: '')
                    )
                    div(class: 'form-control') {
                        label(for: 'username', 'Username')
                        input(type: 'text', name: 'username', id: 'username', required: 'required', autocomplete: 'username')
                    }
                    div(class: 'form-control') {
                        label(for: 'password', 'Password')
                        input(type: 'password', name: 'password', id: 'password', required: 'required', autocomplete: 'current-password')
                    }
                    // Forgot-password entry point. Sits just under the password
                    // field, before the "keep me signed in" row. This is the
                    // only way into the reset flow from the UI.

                    div(class: 'login-help') {
                        div(class: 'login-forgot') {
                            a(href: '/account/forgot-password', 'Forgot your password?')
                        }
                        div(class: 'login-remember') {
                            input(type: 'checkbox', name: 'remember-me', id: 'remember-me', value: 'true')
                            label(for: 'remember-me', 'Keep me signed in')
                        }
                    }
                    div(class: 'auth-actions-grid') {
                        a(href: '/signup', class: 'btn', 'Sign Up')
                        button(type: 'submit', class: 'btn btn-brown', 'Sign In')
                    }
                }

                // Resend-verification block. UserController.login() sets
                // showResendVerification = true when a login attempt fails
                // with DisabledException (account exists, email not yet
                // verified). The bare email form below POSTs to the
                // enumeration-oracle-safe resend endpoint.
                if (showResendVerification) {
                    div(class: 'auth-resend-verification') {
                        p('Need a new confirmation link? Enter your email and we\'ll send one.')
                        form(action: '/account/resend-verification', method: 'post', class: 'form-group') {
                            input(
                                    type: 'hidden',
                                    name: (csrfParamName ?: '_csrf'),
                                    value: (csrfToken ?: '')
                            )
                            div(class: 'form-control') {
                                label(for: 'resendEmail', 'Email address')
                                input(type: 'email', name: 'email', id: 'resendEmail',
                                        required: 'required', autocomplete: 'email',
                                        placeholder: 'name@example.com')
                            }
                            div(class: 'auth-actions-grid') {
                                button(type: 'submit', class: 'btn btn-brown', 'Resend Confirmation')
                            }
                        }
                    }
                }
            }
        }
