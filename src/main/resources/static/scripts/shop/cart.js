document.addEventListener('DOMContentLoaded', function() {
    const qtyButtons = document.querySelectorAll('.btn-qty');

    qtyButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            event.preventDefault();
            const url = this.getAttribute('href');

            const csrfHeaders = (window.CGS && window.CGS.csrfHeaders) ? window.CGS.csrfHeaders() : {};

            fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    ...csrfHeaders,
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const cartLinkContainer = document.getElementById('cart-link-container');
                    if (cartLinkContainer) {
                        cartLinkContainer.innerHTML = `<a href="/cart">Cart(${data.cartCount})</a>`;
                    }
                    window.location.reload();
                }
            })
            .catch(error => console.error('Error updating cart:', error));
        });
    });

    // Shipping estimate refresh (address dropdown)
    const shippingDropdown = document.getElementById('shipping-dropdown');
    const shippingEstimatesContainer = document.getElementById('shipping-estimates');

    const orderSummary = document.getElementById('order-summary');
    const taxValueEl = document.getElementById('tax-value');
    const totalValueEl = document.getElementById('total-value');

    function flashGlow(el) {
        if (!el) return;
        el.classList.remove('glow-green');
        void el.offsetWidth;
        el.classList.add('glow-green');
        window.setTimeout(() => el.classList.remove('glow-green'), 950);
    }

    function flashGlowAll(nodeList) {
        if (!nodeList) return;
        nodeList.forEach(flashGlow);
    }

    function formatUsd(amount) {
        const num = Number(amount);
        const safe = Number.isFinite(num) ? num : 0;
        return `$${safe.toFixed(2)}`;
    }

    function getSubtotalAndTaxRate() {
        if (!orderSummary) return { subtotal: 0, taxRate: 0.07 };
        const subtotal = Number(orderSummary.dataset.subtotal);
        const taxRate = Number(orderSummary.dataset.taxRate);
        return {
            subtotal: Number.isFinite(subtotal) ? subtotal : 0,
            taxRate: Number.isFinite(taxRate) ? taxRate : 0.07
        };
    }

    function updateTaxAndTotal(totalShipping) {
        const { subtotal, taxRate } = getSubtotalAndTaxRate();
        const shipping = Number(totalShipping);
        const safeShipping = Number.isFinite(shipping) ? shipping : 0;

        const tax = (subtotal + safeShipping) * taxRate;
        const total = subtotal + safeShipping + tax;

        if (taxValueEl) taxValueEl.textContent = formatUsd(tax);
        if (totalValueEl) totalValueEl.textContent = formatUsd(total);

        flashGlow(taxValueEl);
        flashGlow(totalValueEl);
    }

    function renderShippingEstimates(estimates) {
        if (!shippingEstimatesContainer) return;
        shippingEstimatesContainer.innerHTML = '';

        if (!estimates || estimates.length === 0) {
            const emptyRow = document.createElement('div');
            emptyRow.className = 'summary-row shipping-detail';
            emptyRow.innerHTML = `<span>Shipping</span><span class="summary-value">—</span>`;
            shippingEstimatesContainer.appendChild(emptyRow);
            return;
        }

        estimates.forEach(est => {
            const row = document.createElement('div');
            row.className = 'summary-row shipping-detail';
            row.innerHTML =
                `<span>Shipping</span>` +
                `<span class="summary-value">${formatUsd(est.cost)}</span>`;
            shippingEstimatesContainer.appendChild(row);
        });

        flashGlowAll(shippingEstimatesContainer.querySelectorAll('.summary-value'));
    }

    async function refreshShippingEstimates() {
        if (!shippingDropdown) return;
        const addressId = shippingDropdown.value;
        if (!addressId) {
            renderShippingEstimates([]);
            updateTaxAndTotal(0);
            return;
        }

        try {
            const res = await fetch(`/api/shipping/estimate?addressId=${encodeURIComponent(addressId)}`, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!res.ok) throw new Error(`Estimate request failed: ${res.status}`);
            const data = await res.json();

            renderShippingEstimates(data.estimates || []);
            updateTaxAndTotal(data.totalShipping || 0);
        } catch (err) {
            console.error('Error refreshing shipping estimates:', err);
        }
    }

    if (shippingDropdown) {
        shippingDropdown.addEventListener('change', refreshShippingEstimates);
        refreshShippingEstimates();
    }

    window.CGS = window.CGS || {};
    window.CGS.refreshShippingEstimates = refreshShippingEstimates;
});