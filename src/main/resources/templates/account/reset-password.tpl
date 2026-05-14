package templates.account

/*
 * "Choose a new password" form. Reached from the link in the reset email.
 *
 * AccountController.resetPasswordForm() has already peek-validated the token
 * before this renders — an invalid/expired token shows account/reset-failed
 * instead, so by the time we're here the token is known-good.
 *
 * Returned as "account/reset-password" → templates/account/reset-password.tpl.
 *
 * Bindings:
 *   token          — the validated reset token, carried as a hidden field
 *   error          — flash error (rare — see note below)
 *   imagesBaseUrl  — passed through to layout
 *   csrf*          — passed through to layout
 *
 * POSTs to /account/reset-password. The token is CONSUMED (single-use) on
 * that POST. Note: if the password fails server-side validation, the token
 * is already spent — performReset() bounces the user to forgot-password with
 * the reason, NOT back here. So `error` rendering here is just defensive; in
 * practice a validation failure lands on the forgot-password page.
 *
 * Password rules (minlength: 10) mirror CredentialService.resetPassword().
 * The server is authoritative; this is the client-side hint. Matches the
 * minlength used in partials/change-password-modal.tpl.
 */

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Choose a New Password',
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

                h2('Choose a new password')
                p('Pick a new password for your account. It needs to be at least 10 characters.')

                form(action: '/account/reset-password', method: 'post', class: 'form-group') {
                    input(
                            type: 'hidden',
                            name: (csrfParamName ?: '_csrf'),
                            value: (csrfToken ?: '')
                    )
                    // The validated reset token, carried through from the GET.
                    // performReset() consumes it.
                    input(type: 'hidden', name: 'token', value: (token ?: ''))

                    div(class: 'form-control') {
                        label(for: 'newPassword', 'New Password')
                        input(type: 'password', name: 'newPassword', id: 'newPassword',
                                required: 'required', minlength: '10',
                                autocomplete: 'new-password')
                    }
                    div(class: 'form-control') {
                        label(for: 'confirmNewPassword', 'Confirm New Password')
                        input(type: 'password', name: 'confirmNewPassword', id: 'confirmNewPassword',
                                required: 'required', minlength: '10',
                                autocomplete: 'new-password')
                    }
                    div(class: 'auth-actions-grid') {
                        a(href: '/login', class: 'btn', 'Cancel')
                        button(type: 'submit', class: 'btn btn-brown', 'Reset Password')
                    }
                }
            }
        }
