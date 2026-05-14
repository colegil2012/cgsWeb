package templates.account

/*
 * "Enter your email to reset your password" form.
 *
 * Sibling of templates/user/login.tpl — same layout call, same auth-container
 * shell, same alert + form-control vocabulary.
 *
 * Returned by AccountController.forgotPasswordForm() as "account/forgot-password",
 * so this file must live at templates/account/forgot-password.tpl.
 *
 * Bindings:
 *   message        — flash success (set by requestReset on every submit)
 *   error          — flash error (empty-email submit bounces back here)
 *   imagesBaseUrl  — passed through to layout
 *   csrf*          — passed through to layout
 *
 * POSTs to /account/request-reset, which is enumeration-oracle-safe: it
 * returns the same generic message whether or not the email maps to an
 * account.
 */

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Reset Password',
        imagesBaseUrl: imagesBaseUrl,
        csrfToken: (csrfToken ?: ''),
        csrfParamName: (csrfParamName ?: '_csrf'),
        csrfHeaderName: (csrfHeaderName ?: 'X-CSRF-TOKEN'),
        headContent: {
            link(rel: 'stylesheet', href: '/css/pages/login.css')
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

                h2('Reset your password')
                p('Enter the email address on your account and we\'ll send you a link to choose a new password. The link expires 30 minutes after it\'s sent.')

                form(action: '/account/request-reset', method: 'post', class: 'form-group') {
                    input(
                            type: 'hidden',
                            name: (csrfParamName ?: '_csrf'),
                            value: (csrfToken ?: '')
                    )
                    div(class: 'form-control') {
                        label(for: 'email', 'Email address')
                        input(type: 'email', name: 'email', id: 'email',
                                required: 'required', autocomplete: 'email',
                                placeholder: 'name@example.com')
                    }
                    div(class: 'auth-actions-grid') {
                        a(href: '/login', class: 'btn', 'Back to Login')
                        button(type: 'submit', class: 'btn btn-brown', 'Send Reset Link')
                    }
                }
            }
        }
