package templates.partials

/*
 * Confirm-order modal opened by the Checkout button on /checkout.
 *
 * The modal has TWO mutually-exclusive views:
 *
 *   1. The form view ("confirm-form-view") — order recap, deliver-to, instructions,
 *      Cancel + Confirm Order buttons. Default visible.
 *   2. The status view ("confirm-status") — takes over the entire modal body during
 *      submit. Has three sub-states (loading / success / error) toggled via classes
 *      added by checkout-submit.js.
 *
 * Switching between them is one class on the modal: .modal-confirm-order.is-status
 * (controlled by JS) hides the form view and shows the status view.
 *
 * Bindings expected from the host template:
 *   csrfParamName, csrfToken     – Spring Security CSRF
 */
div(class: 'modal-overlay', id: 'confirmOrderOverlay', style: 'display:none;') {
    div(class: 'modal modal-confirm-order') {

        /* ============================================================
         * Form view  (default)
         * ============================================================ */
        div(class: 'confirm-form-view') {

            div(class: 'modal-header') {
                h3('Confirm Your Order')
                button(type: 'button', class: 'modal-close', id: 'closeConfirmOrder', '×')
            }

            // ---- Readonly recap ---------------------------------------------------------
            div(class: 'confirm-section') {
                h4('Order Summary')
                div(class: 'confirm-summary', id: 'confirm-summary') {
                    div(class: 'confirm-row') {
                        span('Subtotal')
                        span(class: 'confirm-value', id: 'confirm-subtotal', '—')
                    }
                    div(class: 'confirm-row') {
                        span('Shipping')
                        span(class: 'confirm-value', id: 'confirm-shipping', '—')
                    }
                    div(class: 'confirm-row') {
                        span('Estimated Tax')
                        span(class: 'confirm-value', id: 'confirm-tax', '—')
                    }
                    hr()
                    div(class: 'confirm-row total') {
                        span('Total')
                        span(class: 'confirm-value', id: 'confirm-total', '—')
                    }
                }
            }

            // ---- Delivery-to preview ----------------------------------------------------
            div(class: 'confirm-section') {
                h4('Deliver To')
                div(class: 'confirm-deliver-to', id: 'confirm-deliver-to') {
                    p(class: 'confirm-line', id: 'confirm-deliver-name', '—')
                    p(class: 'confirm-line', id: 'confirm-deliver-street1', '')
                    // Inline display:none preserved here — checkout-submit.js
                    // toggles this via el.style.display directly. Migrating to
                    // a class would require updating the JS.
                    p(class: 'confirm-line', id: 'confirm-deliver-street2', style: 'display:none;', '')
                    p(class: 'confirm-line', id: 'confirm-deliver-citystatezip', '')
                }
            }

            // ---- Optional delivery instructions ----------------------------------------
            div(class: 'confirm-section') {
                h4('Delivery Instructions (optional)')
                div(class: 'form-control') {
                    textarea(
                            id: 'confirm-delivery-instructions',
                            name: 'deliveryInstructions',
                            rows: '3',
                            maxlength: '500',
                            placeholder: 'e.g. "Leave on side porch, gate code 1234"',
                            ''
                    )
                    small(class: 'confirm-instruction-counter', id: 'confirm-instruction-counter',
                            '0 / 500')
                }
            }

            // ---- Action buttons --------------------------------------------------------
            div(class: 'confirm-actions') {
                button(type: 'button', class: 'btn btn-secondary', id: 'cancelConfirmOrder', 'Cancel')
                button(type: 'button', class: 'btn btn-confirm-order', id: 'confirmOrderBtn',
                        'Confirm Order')
            }
        }

        /* ============================================================
         * Status view  (toggled on by JS during submit)
         * Inner sub-states are toggled by classes on this element:
         *   .is-loading  – default once shown
         *   .is-success  – successful submit, awaiting redirect
         *   .is-error    – submit failed, show retry
         * ============================================================ */
        div(class: 'confirm-status', id: 'confirmStatus',
                role: 'status', 'aria-live': 'polite') {

            // Loading icon: three bouncing leaves, in place.
            div(class: 'confirm-status-icon confirm-status-icon-loading') {
                div(class: 'leaf-spinner-bouncing') {
                    span(class: 'leaf-spinner-bouncing__leaf') {}
                    span(class: 'leaf-spinner-bouncing__leaf') {}
                    span(class: 'leaf-spinner-bouncing__leaf') {}
                }
            }

            // Error icon: warning glyph; CSS handles its red color.
            div(class: 'confirm-status-icon confirm-status-icon-error', '!')

            h3(class: 'confirm-status-title', id: 'confirmStatusTitle', 'Placing your order…')
            p(class: 'confirm-status-message', id: 'confirmStatusMessage',
                    'Hold tight while we tidy up your delivery.')

            // Retry button — only visible in error state. JS calls .click() on cancel
            // when "Try again" is hit, which restores the form view.
            div(class: 'confirm-status-actions') {
                button(type: 'button', class: 'btn btn-small', id: 'retryConfirmOrder',
                        'Try again')
            }
        }
    }
}
