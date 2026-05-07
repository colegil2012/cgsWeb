document.addEventListener('DOMContentLoaded', async () => {
    const addresses = window.USER_ADDRESSES;

    const addressSelect = document.getElementById('checkout-address-select');
    const shippingEstimatesContainer = document.getElementById('shipping-estimates');
    const addressInput = document.getElementById('selected-address-input');
    const addressStreet1 = document.getElementById('selected-address-street-1');
    const addressStreet2 = document.getElementById('selected-address-street-2');
    const addressCityStateZip = document.getElementById('selected-address-city-state-zip');

    const orderSummary = document.getElementById('order-summary');
    const taxValueEl = document.getElementById('tax-value');
    const totalValueEl = document.getElementById('total-value');
    const totalCentsInput = document.getElementById('total-cents');

    function formatUsd(amount) {
        const num = Number(amount);
        const safe = Number.isFinite(num) ? num : 0;
        return `$${safe.toFixed(2)}`;
    }

    function getSubtotalAndTaxRate() {
        if (!orderSummary) return { subtotal: 0, taxRate: 0 };

        const subtotal = Number(orderSummary.dataset.subtotal);
        const taxRate = Number(orderSummary.dataset.taxRate);

        return {
            subtotal: Number.isFinite(subtotal) ? subtotal : 0,
            taxRate: Number.isFinite(taxRate) ? taxRate : 0
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

        // Keep the hidden field Square submits in sync with the visible total
        //if (totalCentsInput) totalCentsInput.value = Math.round(total * 100);

        flashGlow(taxValueEl);
        flashGlow(totalValueEl);
    }

    function flashGlow(el) {
        if (!el) return;
        el.classList.remove('glow-green'); // reset so re-adding replays animation
        void el.offsetWidth;               // force reflow
        el.classList.add('glow-green');
        window.setTimeout(() => el.classList.remove('glow-green'), 950);
    }

    function flashGlowAll(nodeList) {
        if (!nodeList) return;
        nodeList.forEach(flashGlow);
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
        if (!addressSelect) return;
        const addressId = addressSelect.value;
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

    // Expose so address-update.js can trigger a refresh after save
    window.CGS = window.CGS || {};
    window.CGS.refreshShippingEstimates = refreshShippingEstimates;


    // Sync Address Select with hidden input field and address preview
    function syncAddress() {
        if (!addressSelect) {
            return;
        }
        const selectedId = addressSelect.value;

        if(addressInput) { addressInput.value = selectedId }
        const address = addresses.find( a => a.addressId === selectedId);

        if (!address) {
            return;
        }

        if (addressStreet1) { addressStreet1.textContent = address.street1; }
        if (addressStreet2) {
            if (address.street2) {
                addressStreet2.textContent = address.street2;
                addressStreet2.style.display = '';
            } else {
                addressStreet2.textContent = '';
                addressStreet2.style.display = 'none';
            }
        }

        if (addressCityStateZip) {
            addressCityStateZip.textContent = `${address.city}, ${address.state} ${address.zip}`;
        }
    }

    if (addressSelect) {
        addressSelect.addEventListener('change', () => {
            syncAddress();
            refreshShippingEstimates();
        });
        syncAddress();
        refreshShippingEstimates();
    }
});