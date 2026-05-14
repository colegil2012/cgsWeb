/*
 * HTML email-verification message, sent at signup.
 *
 * Same rules of engagement as order-confirmation.html.tpl:
 *   - Inline styles only; tables for layout; hardcoded hex matching design tokens.
 *
 * Bindings expected from SignupConfirmationMailer:
 *   firstName    — user's first name, or "there" as a fallback
 *   verifyUrl    — absolute verification link (publicBaseUrl + /account/verify?token=...)
 *   expiryHours  — integer, how long the link stays valid (48)
 *
 * NOTE: Celtech text-logo URL hardcoded on LINE 24, same asset as the
 * order-confirmation template. If the asset moves, update both.
 */

yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
    head {
        meta('http-equiv': 'Content-Type', content: 'text/html; charset=UTF-8')
        title('Confirm your Celtech General Store account')
    }
    /* Body — soft cream base (#faf6f3), brand brown text (#5a3a1e). */
    body(style: 'margin:0; padding:24px; background:#faf6f3; ' +
            'font-family: Arial, sans-serif; color:#5a3a1e;') {

        table(width: '100%', cellpadding: '0', cellspacing: '0', border: '0',
                style: 'max-width:640px; margin:0 auto; background:#ffffff; ' +
                        'border:1px solid #d7ccc8; border-radius:14px; ' +
                        'overflow:hidden;') {

            /* ---- Logo header strip ---- */
            tr {
                td(align: 'center',
                        style: 'padding: 28px 28px 18px; background:#33691e; ' +
                                'border-bottom: 1px solid #d7ccc8;') {
                    img(src: 'https://cgsweb-img.nyc3.cdn.digitaloceanspaces.com/site-images/Celtech%20Text%20Logo%20Middle.png',
                            alt: 'Celtech General Store',
                            width: '240',
                            style: 'display:block; max-width:240px; width:100%; ' +
                                    'height:auto; margin:0 auto;')
                }
            }

            /* ---- Welcome + instruction ---- */
            tr {
                td(align: 'center',
                        style: 'padding: 22px 28px 14px; background:#ffffff;') {
                    h1(style: 'margin:0; color:#33691e; font-size:22px; ' +
                            'font-weight:bold;',
                            "Welcome, ${firstName}!")
                    p(style: 'margin:8px 0 0; color:#5a3a1e; font-size:15px; ' +
                            'line-height:1.5;') {
                        yield 'Thanks for creating an account. Confirm your email '
                        yield 'address to activate your account and start shopping.'
                    }
                }
            }

            /* ---- CTA button ---- */
            tr {
                td(style: 'padding: 12px 28px 8px;') {
                    p(style: 'margin: 16px 0; text-align: center;') {
                        a(href: verifyUrl,
                                style: 'display:inline-block; padding: 12px 28px; ' +
                                        'background:#33691e; color:#ffffff; ' +
                                        'text-decoration:none; border-radius:8px; ' +
                                        'font-weight:bold; font-size:15px;',
                                'Confirm my email')
                    }
                }
            }

            /* ---- Fallback link + expiry note ---- */
            tr {
                td(style: 'padding: 4px 28px 24px;') {
                    p(style: 'margin:0 0 12px; color:#7a6757; font-size:13px; ' +
                            'line-height:1.5;') {
                        yield "If the button doesn't work, copy and paste this "
                        yield 'link into your browser:'
                    }
                    p(style: 'margin:0 0 16px; padding:10px 12px; background:#faf6f3; ' +
                            'border:1px solid #d7ccc8; border-radius:8px; ' +
                            'color:#33691e; font-size:12px; word-break:break-all;') {
                        yield verifyUrl
                    }
                    p(style: 'margin:0; padding:12px 14px; background:#e8f5e9; ' +
                            'border-left:3px solid #558b2f; border-radius:8px; ' +
                            'color:#5a3a1e; font-size:13px; line-height:1.5;') {
                        yield "This link expires in ${expiryHours} hours. "
                        yield "If it does, you can request a new one from the login page."
                    }
                }
            }

            /* ---- "Didn't sign up?" reassurance ---- */
            tr {
                td(style: 'padding: 0 28px 24px;') {
                    p(style: 'margin:0; color:#7a6757; font-size:13px; ' +
                            'line-height:1.5;') {
                        yield "Didn't create this account? You can safely ignore "
                        yield 'this email \u2014 no account is activated until the '
                        yield 'link above is clicked.'
                    }
                }
            }

            /* ---- Footer ---- */
            tr {
                td(align: 'center',
                        style: 'padding: 18px 28px; background:#faf6f3; ' +
                                'border-top:1px solid #d7ccc8; ' +
                                'color:#7a6757; font-size:12px;') {
                    yield 'Questions? Reply to this email \u2014 it goes straight to '
                    a(href: 'mailto:cole@celtechgs.com',
                            style: 'color:#33691e; text-decoration:none;',
                            'cole@celtechgs.com')
                    yield '.'
                    br()
                    span(style: 'color:#a99c8d;', 'Celtech General Store \u00b7 celtechgs.com')
                }
            }
        }
    }
}
