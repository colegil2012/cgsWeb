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
                    // For a cart page, it's often easiest to reload to update all totals/taxes
                    // but you can also update the header badge manually:
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
});