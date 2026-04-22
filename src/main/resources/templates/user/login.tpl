package templates.user

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Login',
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        content: {
            div(class: 'hero') {
                img(src: ImageUrlUtil.resolve('/images/site-images/Celtech Transparent.png', imagesBaseUrl), alt: 'Celtech Logo', class: 'logo-image')
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
                    div(class: 'form-control', style: 'display:flex; align-items:center; gap:8px;') {
                        input(type: 'checkbox', name: 'remember-me', id: 'remember-me', value: 'true')
                        label(for: 'remember-me', style: 'margin:0; font-weight:500;', 'Keep me signed in')
                    }
                    div(class: 'auth-actions-grid') {
                        a(href: '/signup', class: 'signup-btn', 'Sign Up')
                        button(type: 'submit', class: 'btn', 'Sign In')
                    }
                }
            }
        }