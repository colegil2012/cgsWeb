package templates.account

/*
 * Shown when an email-verification token is invalid, expired, or already
 * used. Includes an inline resend form so the user can get a fresh link
 * without hunting for it.
 *
 * Returned as "account/verify-failed" → templates/account/verify-failed.tpl.
 *
 * Bindings:
 *   reason         — short human-readable explanation (set by the controller)
 *   imagesBaseUrl  — passed through to layout
 *   csrf*          — passed through to layout
 *
 * The resend form POSTs to /account/resend-verification, which is
 * enumeration-oracle-safe — it always responds the same way.
 *
 * NOTE on the "already used" case: if a user clicks their verification link
 * twice, the second click lands here ("invalid or expired") even though
 * their account IS verified and fine. That's a small UX wrinkle of the
 * opaque-failure contract. Documented in ROUND-2-NOTES.md; revisit only if
 * it generates real confusion.
 */

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | Verification Link Problem',
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
                        (reason ?: 'This verification link is invalid or has expired.'))

                h2('That link didn\'t work')
                p('Verification links expire 48 hours after they\'re sent, and each one can only be used once. Enter your email below and we\'ll send a fresh one.')

                form(action: '/account/resend-verification', method: 'post', class: 'form-group') {
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
                        button(type: 'submit', class: 'btn btn-brown', 'Send New Link')
                    }
                }
            }
        }
