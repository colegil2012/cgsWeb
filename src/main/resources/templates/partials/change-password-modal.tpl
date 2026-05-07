package templates.partials

/*
 * Change-password modal opened from the Security tab in account.tpl.
 * JS hooks (account-security.js):
 *   #changePasswordOverlay — open/close target
 *   #closeChangePassword   — close button (header)
 *   #cancelChangePassword  — cancel button (footer)
 *   #oldPassword, #newPassword, #confirmNewPassword — fields
 *
 * Bindings expected from the host template:
 *   csrfParamName, csrfToken — Spring Security CSRF
 */

div(class: 'modal-overlay', id: 'changePasswordOverlay', style: 'display:none;') {
    div(class: 'modal') {
        div(class: 'modal-header') {
            h3('Change Password')
            button(type: 'button', class: 'modal-close', id: 'closeChangePassword', '×')
        }

        form(action: '/account/password', method: 'post', class: 'form-group') {

            input(
                    type: 'hidden',
                    name: (csrfParamName ?: '_csrf'),
                    value: (csrfToken ?: '')
            )

            div(class: 'form-control') {
                label(for: 'oldPassword', 'Old Password')
                input(type: 'password', name: 'oldPassword', id: 'oldPassword',
                        required: 'required')
            }

            div(class: 'form-control') {
                label(for: 'newPassword', 'New Password')
                input(type: 'password', name: 'newPassword', id: 'newPassword',
                        required: 'required', minlength: '10')
            }

            div(class: 'form-control') {
                label(for: 'confirmNewPassword', 'Confirm New Password')
                input(type: 'password', name: 'confirmNewPassword', id: 'confirmNewPassword',
                        required: 'required', minlength: '10')
            }

            div(class: 'modal-actions') {
                button(type: 'button', class: 'btn btn-secondary', id: 'cancelChangePassword', 'Cancel')
                button(type: 'submit', class: 'btn', 'Update Password')
            }
        }
    }
}
