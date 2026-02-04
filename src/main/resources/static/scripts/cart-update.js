document.addEventListener('DOMContentLoaded', function() {
    const qtyButtons = document.querySelectorAll('.btn-qty');

    qtyButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            event.preventDefault();
            const url = this.getAttribute('href');

            fetch(url, {
                method: 'GET',
                headers: {
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

                    // Reload to update subtotal/tax/totals accurately
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
        el.classList.remove('glow-green'); // reset so re-adding replays animation
        void el.offsetWidth;               // force reflow
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

     async function refreshShippingEstimates() {
         if (!shippingDropdown || !shippingEstimatesContainer) return;

         const shippingAddressId = shippingDropdown.value;
         if (!shippingAddressId) return;

         try {
             const res = await fetch(`/api/roadie/estimate?shippingAddressId=${encodeURIComponent(shippingAddressId)}`, {
                 method: 'POST',
                 headers: { 'Accept': 'application/json' }
             });

             if (!res.ok) {
                 console.error('Shipping estimate request failed:', await res.text());
                 return;
             }

             const data = await res.json();
             console.log('estimate data', data);
             const estimates = Array.isArray(data.shippingEstimates) ? data.shippingEstimates : [];

             shippingEstimatesContainer.innerHTML = estimates.map(e => {
                 const vendor = e.vendor ?? '';
                 const costNum = Number(e.cost);
                 const cost = Number.isFinite(costNum) ? costNum.toFixed(2) : '0.00';

                 return `
                     <div class="summary-row shipping-detail">
                         <span class="ship-name">Shipping (${vendor})</span>
                         <span class="summary-value ship-cost">$${cost}</span>
                     </div>
                 `;
             }).join('');

             // Glow the newly inserted shipping names + costs
             flashGlowAll(shippingEstimatesContainer.querySelectorAll('.ship-name'));
             flashGlowAll(shippingEstimatesContainer.querySelectorAll('.ship-cost'));

             updateTaxAndTotal(data.totalShipping);
         } catch (err) {
             console.error('Error fetching shipping estimates:', err);
         }
     }

     if (shippingDropdown) {
         shippingDropdown.addEventListener('change', refreshShippingEstimates);
         refreshShippingEstimates(); // initial load for default-selected address
     }
 });