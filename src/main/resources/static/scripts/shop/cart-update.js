/**
 * Handles adding items to the shopping cart with a flying animation effect.
 * Works for both authenticated users AND guests (server creates a guest cookie lazily).
 *
 * @param {string} productId - The ID of the product to add.
 */
function addToCart(productId) {
    const productCard = event.target.closest('.product-card') || event.target.closest('.product-view-container');
    const imgToClone = productCard ? productCard.querySelector('img') : null;
    const cartIcon = document.querySelector('.cart-link');

    if (imgToClone && cartIcon) {
        const clone = imgToClone.cloneNode();
        const rect = imgToClone.getBoundingClientRect();
        const cartRect = cartIcon.getBoundingClientRect();

        clone.classList.add('flying-item');
        clone.style.width = rect.width + 'px';
        clone.style.height = rect.height + 'px';
        clone.style.top = rect.top + 'px';
        clone.style.left = rect.left + 'px';

        document.body.appendChild(clone);

        setTimeout(() => {
            clone.style.top = (cartRect.top + 10) + 'px';
            clone.style.left = (cartRect.left + 10) + 'px';
            clone.style.width = '20px';
            clone.style.height = '20px';
            clone.style.opacity = '0.2';
            clone.style.transform = 'rotate(360deg)';
        }, 10);

        setTimeout(() => clone.remove(), 800);
    }

    const csrfHeaders = (window.CGS && window.CGS.csrfHeaders) ? window.CGS.csrfHeaders() : {};

    fetch('/cart/add/' + encodeURIComponent(productId), {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            ...csrfHeaders,
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            if (data.success) {
                const cartBadge = document.querySelector('.cart-count');
                if (cartBadge) {
                    cartBadge.textContent = data.cartCount;
                    cartBadge.style.transition = 'transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
                    cartBadge.style.transform = 'scale(1.5)';
                    setTimeout(() => {
                        cartBadge.style.transform = 'scale(1)';
                    }, 200);
                }
            }
        })
        .catch(error => {
            console.error('Error adding to cart:', error);
        });
}