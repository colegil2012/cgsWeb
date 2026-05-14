package templates.account

/*
 * Shown when a password-reset token is invalid, expired, or already used.
 *
 * Reached two ways from AccountController:
 *   - resetPasswordForm() — the GET link was bad (peek failed)
 *   - performReset()      — the POST token was bad (consume returned false)
 *
 * Deliberately does NOT say which failure mode — consistent with the
 * opaque-failure contract in TokenService.
 *
 * Returned as "account/reset-failed" → templates/account/reset-failed.tpl.
 *
 * Bindings:
 *   reason         — short human-readable explanation (set by the controller)
 *   imagesBaseUrl  — passed through to layout
 *   csrf*          — passed through to layout
 */

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Reset Link Problem',
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
                div(class: 'alert alert-error',
                        (reason ?: 'This password-reset link is invalid or has expired.'))

                h2('That link didn\'t work')
                p('Reset links expire 30 minutes after they\'re sent, and each one can only be used once. Request a fresh link to continue.')

                div(class: 'auth-actions-grid') {
                    a(href: '/login', class: 'btn', 'Back to Login')
                    a(href: '/account/forgot-password', class: 'btn btn-brown', 'Request a New Link')
                }
            }
        }
