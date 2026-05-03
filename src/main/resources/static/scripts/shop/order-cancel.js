/**
 * order-cancel.js
 *
 * Wires the "Cancel this order" button on the receipt page (Phase 6C).
 *
 * Behaviour:
 *   1. On load, compute the cancellation deadline from the button's
 *      data-placed-at + data-window-seconds. If we're already past it, hide
 *      the button immediately.
 *   2. Otherwise show a live countdown ("Cancel this order (4:32 left)") so
 *      the user knows when their window closes.
 *   3. On click, POST /checkout/cancel/{orderId}. On success, swap the
 *      success banner for a "cancelled" notice; on failure, show the error
 *      inline.
 *
 * Defensive: the *server* is the source of truth about whether cancellation
 * is allowed. The client timer is purely UX — if it's wrong, the server
 * still rejects.
 */
(() => {
    const btn = document.getElementById('cancelOrderBtn');
    const help = document.getElementById('cancelOrderHelp');
    if (!btn) return;

    const orderId       = btn.dataset.orderId;
    const placedAtRaw   = btn.dataset.placedAt;
    const windowSeconds = Number(btn.dataset.windowSeconds);

    if (!orderId || !placedAtRaw || !Number.isFinite(windowSeconds) || windowSeconds <= 0) {
        btn.remove();
        return;
    }

    // The server emits LocalDateTime in ISO-8601 form (2026-04-30T17:25:23.597),
    // which Date can parse if we treat it as local. Adding 'Z' would make it
    // UTC, which is wrong — we want the customer's local time vs the server's
    // local time. Slight skew ok; we have a 5-minute window for a reason.
    const placedAt = new Date(placedAtRaw);
    if (Number.isNaN(placedAt.getTime())) {
        btn.remove();
        return;
    }

    const deadline = placedAt.getTime() + (windowSeconds * 1000);

    function remainingSec() {
        return Math.max(0, Math.floor((deadline - Date.now()) / 1000));
    }

    function formatRemaining(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return `${m}:${String(s).padStart(2, '0')}`;
    }

    function paint() {
        const left = remainingSec();
        if (left <= 0) {
            btn.disabled = true;
            btn.textContent = 'Cancellation window expired';
            btn.classList.add('is-expired');
            if (help) help.textContent = 'To cancel after this window, please contact support.';
            return false;   // stop the timer loop
        }
        btn.textContent = `Cancel this order (${formatRemaining(left)} left)`;
        return true;
    }

    // Initial paint + 1-second tick. Skipping requestAnimationFrame because the
    // user's eyes don't need 60fps for an mm:ss countdown.
    if (!paint()) return;
    const timerId = window.setInterval(() => {
        if (!paint()) window.clearInterval(timerId);
    }, 1000);

    btn.addEventListener('click', async () => {
        if (btn.disabled) return;
        if (!window.confirm('Cancel this order? This cannot be undone.')) return;

        btn.disabled = true;
        btn.textContent = 'Cancelling…';

        const headers = (window.CGS && typeof window.CGS.csrfHeaders === 'function')
            ? window.CGS.csrfHeaders()
            : {};
        headers['X-Requested-With'] = 'XMLHttpRequest';
        headers['Accept'] = 'application/json';

        try {
            const res = await fetch(`/checkout/cancel/${encodeURIComponent(orderId)}`, {
                method: 'POST',
                headers,
            });
            const payload = await res.json().catch(() => null);

            if (res.ok && payload && payload.cancelled) {
                renderCancelledState(payload.orderNumber || '');
                window.clearInterval(timerId);
            } else {
                const msg = (payload && payload.error)
                    ? payload.error
                    : `Cancellation failed (HTTP ${res.status}).`;
                showInlineError(msg);
            }
        } catch (networkErr) {
            console.error('[order-cancel] network error', networkErr);
            showInlineError('We could not reach the server. Please try again.');
        }
    });

    function renderCancelledState(orderNumber) {
        // Swap the success banner for a cancelled notice. Mutating the existing
        // DOM (rather than reloading) keeps the user's scroll position so they
        // can still see what they cancelled.
        const banner = document.querySelector('.confirmation-banner');
        if (banner) {
            banner.innerHTML = `
        <div class="confirmation-check" style="background:#888;">↶</div>
        <div class="confirmation-banner-text">
          <h1>Order cancelled</h1>
          <p>Order ${orderNumber ? `#${orderNumber}` : ''} has been cancelled. Nothing was charged.</p>
        </div>
      `;
        }
        btn.remove();
        if (help) help.remove();
    }

    function showInlineError(msg) {
        btn.disabled = false;
        btn.classList.add('has-error');
        btn.textContent = 'Cancel this order';
        if (help) {
            help.textContent = msg;
            help.classList.add('is-error');
        }
    }
})();