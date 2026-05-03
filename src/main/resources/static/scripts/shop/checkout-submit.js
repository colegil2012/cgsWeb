/*
 * checkout-submit.js
 *
 * Owns the submit half of the checkout flow:
 *   1. Click on #confirmOrderBtn flips the modal into its status view.
 *   2. POST #payment-form to /checkout/submit via fetch.
 *   3. On 200  → show success, brief pause, redirect.
 *   4. On 4xx/5xx or network failure → show error + Try again button.
 *
 * Companion to checkout-confirm.js, which handles the open/close/snapshot
 * lifecycle. The two scripts communicate only via the DOM:
 *   - #confirmOrderBtn    (we listen)
 *   - #payment-form       (we read)
 *   - #confirmStatus + #confirmStatusTitle + #confirmStatusMessage  (we write)
 *   - .modal-confirm-order.is-status  (we toggle)
 *   - #retryConfirmOrder  (we listen)
 */
(() => {
    const form        = document.getElementById('payment-form');
    const overlay     = document.getElementById('confirmOrderOverlay');
    const modal       = document.querySelector('#confirmOrderOverlay .modal');
    const confirmBtn  = document.getElementById('confirmOrderBtn');
    const cancelBtn   = document.getElementById('cancelConfirmOrder');
    const closeBtn    = document.getElementById('closeConfirmOrder');
    const retryBtn    = document.getElementById('retryConfirmOrder');

    const status      = document.getElementById('confirmStatus');
    const statusTitle = document.getElementById('confirmStatusTitle');
    const statusMsg   = document.getElementById('confirmStatusMessage');

    // Bail if any required handle is missing — means we're on a page that
    // doesn't have the modal partial wired in.
    if (!form || !overlay || !modal || !confirmBtn || !status ||
        !statusTitle || !statusMsg) {
        return;
    }

    /* -----------------------------------------------------------------------
     * State helpers
     * --------------------------------------------------------------------- */

    /** Move modal into the status view, in the given sub-state. */
    function showStatus(subState, title, message) {
        status.classList.remove('is-loading', 'is-success', 'is-error');
        status.classList.add(subState);
        statusTitle.textContent = title;
        statusMsg.textContent   = message;
        modal.classList.add('is-status');
    }

    /** Move modal back to the form view (used by Try again). */
    function showForm() {
        modal.classList.remove('is-status');
        status.classList.remove('is-loading', 'is-success', 'is-error');
    }

    /** Whether we're mid-submit. Used to lock close paths. */
    function isSubmitting() {
        return status.classList.contains('is-loading');
    }

    /* -----------------------------------------------------------------------
     * Submit
     * --------------------------------------------------------------------- */

    async function submitOrder() {
        showStatus('is-loading',
            'Placing your order…',
            'Hold tight while we tidy up your delivery.');

        // Build a FormData from the live form so all hidden inputs (csrf,
        // idempotencyKey, selectedAddress, deliveryInstructions, ...) come along
        // automatically.
        const body = new FormData(form);

        // Defense-in-depth: also send the CSRF header. _csrf.js exposes a helper.
        const headers = (window.CGS && typeof window.CGS.csrfHeaders === 'function')
            ? window.CGS.csrfHeaders()
            : {};
        headers['X-Requested-With'] = 'XMLHttpRequest';
        headers['Accept']           = 'application/json';

        let response;
        try {
            response = await fetch('/checkout/submit', { method: 'POST', headers, body });
        } catch (networkErr) {
            console.error('[checkout-submit] network error', networkErr);
            showStatus('is-error',
                'We couldn\'t reach the server',
                'Check your connection and try again. Your cart is safe.');
            return;
        }

        // Try to parse JSON regardless of status — controller emits JSON for both
        // success and error envelopes.
        let payload = null;
        try {
            payload = await response.json();
        } catch (_) {
            // Non-JSON response (HTML error page, e.g. 502 from upstream proxy).
        }

        if (response.ok && payload && payload.redirect) {
            const orderNumber = payload.orderNumber
                ? `Order #${payload.orderNumber}`
                : 'Your order is on its way';
            showStatus('is-success',
                'Order received!',
                orderNumber + ' — taking you to your receipt…');

            window.setTimeout(() => {
                window.location.assign(payload.redirect);
            }, 600);
            return;
        }

        // Failure path: surface the message and let them retry. Same idempotency
        // key is on the form, so a successful retry produces exactly one order.
        const errorText = (payload && payload.error)
            ? payload.error
            : `Something went wrong (HTTP ${response.status}). Please try again.`;
        showStatus('is-error', 'We couldn\'t place your order', errorText);
    }

    /* -----------------------------------------------------------------------
     * Wiring
     * --------------------------------------------------------------------- */

    confirmBtn.addEventListener('click', (e) => {
        e.preventDefault();
        submitOrder();
    });

    // Try Again: flip back to the form, let checkout-confirm.js's existing
    // handlers take it from here. (Idempotency key stays the same.)
    retryBtn?.addEventListener('click', (e) => {
        e.preventDefault();
        showForm();
    });

    // Lock close paths while submitting. We use capture-phase listeners so we
    // run BEFORE checkout-confirm.js's bubble-phase close listeners, and we
    // call stopImmediatePropagation() to prevent them from running.
    function blockWhenSubmitting(e) {
        if (isSubmitting()) {
            e.preventDefault();
            e.stopImmediatePropagation();
        }
    }
    cancelBtn?.addEventListener('click', blockWhenSubmitting, true);
    closeBtn ?.addEventListener('click', blockWhenSubmitting, true);
    overlay  ?.addEventListener('click', blockWhenSubmitting, true);
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isSubmitting()) {
            e.preventDefault();
            e.stopImmediatePropagation();
        }
    }, true);

    // If the user closes the modal entirely (after success/error/cancel), reset
    // the status state so the next open starts in the form view.
    cancelBtn?.addEventListener('click', () => { if (!isSubmitting()) showForm(); });
    closeBtn ?.addEventListener('click', () => { if (!isSubmitting()) showForm(); });
})();