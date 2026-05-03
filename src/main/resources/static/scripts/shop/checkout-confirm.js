/*
 * checkout-confirm.js
 *
 * Owns the open/close lifecycle of the #confirmOrderOverlay modal on /checkout.
 * Does NOT submit — the actual fetch is wired in Phase 3 (checkout-submit.js).
 * This file populates the modal's readonly recap from the live order summary,
 * mirrors the selected delivery address, and tracks the optional instructions
 * textarea char count.
 *
 * Public surface (window.CGS.confirmModal):
 *   open()   – open the modal, refreshing the snapshot from current page state
 *   close()  – close the modal, leaving the form alone
 */
(() => {

    if (!window.CGS || typeof window.CGS.Modal !== 'function') {
        console.warn('[checkout-confirm] window.CGS.Modal not loaded — is /scripts/_modal.js included in <head>?');
        return;
    }

    const overlayId   = 'confirmOrderOverlay';
    const checkoutBtn = document.getElementById('card-button');
    const overlay     = document.getElementById(overlayId);
    if (!overlay || !checkoutBtn) return;

    const subtotalEl = document.getElementById('confirm-subtotal');
    const shippingEl = document.getElementById('confirm-shipping');
    const taxEl      = document.getElementById('confirm-tax');
    const totalEl    = document.getElementById('confirm-total');

    const nameEl       = document.getElementById('confirm-deliver-name');
    const street1El    = document.getElementById('confirm-deliver-street1');
    const street2El    = document.getElementById('confirm-deliver-street2');
    const cityZipEl    = document.getElementById('confirm-deliver-citystatezip');

    const instructionsEl = document.getElementById('confirm-delivery-instructions');
    const counterEl      = document.getElementById('confirm-instruction-counter');

    // Hidden inputs on the payment form that Phase 3 reads.
    const formInstructions = document.getElementById('delivery-instructions-input');
    const formIdempotency  = document.getElementById('idempotency-key-input');

    /* -------------------------------------------------------------------------
     * Snapshot helpers – pull live values from the page each time we open the
     * modal (the order summary is reactive to address changes via checkout.js).
     * ----------------------------------------------------------------------- */

    function readOrderSummary() {
        const txt = (id) => (document.getElementById(id)?.textContent ?? '').trim();
        const subtotal = txt('subtotal-value');
        const tax      = txt('tax-value');
        const total    = txt('total-value');

        // Shipping rows are dynamic (one per leg). Sum the .summary-value cells under
        // #shipping-estimates so the modal reads the same number the user is staring at.
        const shippingCells = document.querySelectorAll('#shipping-estimates .summary-value');
        let shippingDisplay = '—';
        if (shippingCells.length === 1) {
            shippingDisplay = shippingCells[0].textContent.trim();
        } else if (shippingCells.length > 1) {
            // Multi-leg (future); add their numeric values up.
            let sum = 0;
            shippingCells.forEach(c => {
                const n = Number((c.textContent || '').replace(/[^\d.]/g, ''));
                if (Number.isFinite(n)) sum += n;
            });
            shippingDisplay = `$${sum.toFixed(2)}`;
        }

        return { subtotal, shipping: shippingDisplay, tax, total };
    }

    function readDeliverTo() {
        const txt = (id) => (document.getElementById(id)?.textContent ?? '').trim();
        return {
            // The "name" line in #selected-address-preview is wrapped in <strong>.
            name:    document.querySelector('#selected-address-preview strong')?.textContent?.trim() ?? '',
            street1: txt('selected-address-street-1'),
            street2: txt('selected-address-street-2'),
            cityZip: txt('selected-address-city-state-zip'),
        };
    }

    function paintModal() {
        const o = readOrderSummary();
        if (subtotalEl) subtotalEl.textContent = o.subtotal || '—';
        if (shippingEl) shippingEl.textContent = o.shipping || '—';
        if (taxEl)      taxEl.textContent      = o.tax      || '—';
        if (totalEl)    totalEl.textContent    = o.total    || '—';

        const d = readDeliverTo();
        if (nameEl)    nameEl.textContent    = d.name    || '—';
        if (street1El) street1El.textContent = d.street1 || '';
        if (street2El) {
            if (d.street2) {
                street2El.textContent = d.street2;
                street2El.style.display = '';
            } else {
                street2El.textContent = '';
                street2El.style.display = 'none';
            }
        }
        if (cityZipEl) cityZipEl.textContent = d.cityZip || '';

        // Sync the textarea with whatever's already on the hidden form input
        // (rare, but supports a back-button-into-checkout flow).
        if (instructionsEl && formInstructions) {
            instructionsEl.value = formInstructions.value || '';
            updateCounter();
        }
    }

    function updateCounter() {
        if (!counterEl || !instructionsEl) return;
        const len = instructionsEl.value.length;
        counterEl.textContent = `${len} / 500`;
    }

    /* -------------------------------------------------------------------------
     * Open / close
     * ----------------------------------------------------------------------- */

    const modal = window.CGS.Modal({
        openBtnId:        null,                   // we open imperatively from #card-button
        overlayId:        'confirmOrderOverlay',
        closeBtnId:       'closeConfirmOrder',
        cancelBtnId:      'cancelConfirmOrder',
        focusInputId:     'confirm-delivery-instructions',
        resetFormOnClose: false,
        onOpen:           paintModal,
        dialogLabel: 'Confirm your order',
    });

    if (!modal) return;

    function open() {
        modal.open();
        // Generate a fresh idempotency key so a back-button + re-confirm doesn't
        // collide with the first submit's key.
        if (formIdempotency) {
            formIdempotency.value = (crypto.randomUUID && crypto.randomUUID()) || '';
        }
    }

    function persistInstructions() {
        if (instructionsEl && formInstructions) {
            formInstructions.value = instructionsEl.value.trim();
        }
    }

    /* -------------------------------------------------------------------------
     * Event wiring
     * ----------------------------------------------------------------------- */

    checkoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        open();
    });

    instructionsEl?.addEventListener('input', () => {
        updateCounter();
        persistInstructions();
    });

    // Expose for other scripts (Phase 3 will call open() if ever needed).
    window.CGS = window.CGS || {};
    window.CGS.confirmModal = { open, close: modal.close };
})();