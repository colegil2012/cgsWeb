/**
 * Shared open/close/escape mechanics for every modal in the project.
 *
 * Replaces three near-identical local copies (address-update.js,
 * account-security.js, checkout-confirm.js) so that close-on-overlay-click,
 * close-on-Escape, and form-reset on close are implemented exactly once.
 *
 * Phase 6A additions:
 *   - Focus trap (Tab / Shift-Tab cycle within the modal).
 *   - Return focus to the element that opened the modal on close.
 *   - role="dialog" / aria-modal="true" while shown.
 *
 * Usage:
 *   const modal = window.CGS.Modal({
 *     openBtnId:    'openUpdateAddress',
 *     overlayId:    'updateAddressOverlay',
 *     closeBtnId:   'closeUpdateAddress',
 *     cancelBtnId:  'cancelUpdateAddress',
 *     focusInputId: null,
 *     resetFormOnClose: true,
 *     onOpen:  () => { ... },
 *     onClose: () => { ... },
 *     dialogLabel: 'Edit your saved addresses',
 *   });
 *
 * Returns null when overlay is missing (page doesn't host that modal).
 */
(function () {
    window.CGS = window.CGS || {};

    function noop() {}

    // Selector for the elements that should be focusable inside a trapped modal.
    // Mirrors the WAI-ARIA "tabbable" set; covers the common cases without
    // pulling in a focus-trap library.
    const FOCUSABLE_SELECTOR = [
        'a[href]',
        'button:not([disabled])',
        'input:not([disabled]):not([type="hidden"])',
        'select:not([disabled])',
        'textarea:not([disabled])',
        '[tabindex]:not([tabindex="-1"])',
    ].join(',');

    function listFocusable(root) {
        return Array.from(root.querySelectorAll(FOCUSABLE_SELECTOR))
            .filter(el => !el.hasAttribute('data-modal-trap-skip')
                && el.offsetParent !== null);   // visible-ish
    }

    window.CGS.Modal = function ({
                                     openBtnId,
                                     overlayId,
                                     closeBtnId,
                                     cancelBtnId,
                                     focusInputId = null,
                                     resetFormOnClose = true,
                                     onOpen = noop,
                                     onClose = noop,
                                     dialogLabel = null,
                                 }) {
        const overlay = document.getElementById(overlayId);
        if (!overlay) return null;

        const openBtn   = openBtnId   ? document.getElementById(openBtnId)   : null;
        const closeBtn  = closeBtnId  ? document.getElementById(closeBtnId)  : null;
        const cancelBtn = cancelBtnId ? document.getElementById(cancelBtnId) : null;

        // Static ARIA attributes that don't depend on open/close state.
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');
        if (dialogLabel) overlay.setAttribute('aria-label', dialogLabel);

        // Track who opened the modal so we can return focus to them on close.
        let lastTrigger = null;

        function trapTabKey(e) {
            if (e.key !== 'Tab') return;
            const focusables = listFocusable(overlay);
            if (focusables.length === 0) {
                e.preventDefault();
                return;
            }
            const first = focusables[0];
            const last  = focusables[focusables.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }

        function open() {
            // Remember who opened us (Escape / Cancel will restore focus here).
            lastTrigger = document.activeElement instanceof HTMLElement
                ? document.activeElement
                : null;

            overlay.style.display = 'flex';
            onOpen();

            // Focus priority: explicit focusInputId > first focusable in modal.
            const explicit = focusInputId ? document.getElementById(focusInputId) : null;
            if (explicit) {
                explicit.focus();
            } else {
                const focusables = listFocusable(overlay);
                if (focusables.length > 0) focusables[0].focus();
            }

            overlay.addEventListener('keydown', trapTabKey);
        }

        function close() {
            overlay.removeEventListener('keydown', trapTabKey);
            overlay.style.display = 'none';

            if (resetFormOnClose) {
                const form = overlay.querySelector('form');
                if (form) form.reset();
            }
            onClose();

            // Return focus to whoever opened us so keyboard users don't get stranded.
            if (lastTrigger && typeof lastTrigger.focus === 'function') {
                lastTrigger.focus();
                lastTrigger = null;
            }
        }

        if (openBtn)   openBtn.addEventListener('click', open);
        if (closeBtn)  closeBtn.addEventListener('click', close);
        if (cancelBtn) cancelBtn.addEventListener('click', close);

        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) close();
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && overlay.style.display !== 'none') close();
        });

        return { open, close, overlay };
    };
})();