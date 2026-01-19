/**
 * Handles adding items to the shopping cart with a flying animation effect.
 * This updates the server-side session and refreshes the UI badge without a page reload.
 *
 * @param {string} productId - The ID of the product to add.
 */
function addToCart(productId) {
    // 1. Setup elements for the "Sucked to Cart" animation
    const productCard = event.target.closest('.product-card') || event.target.closest('.product-view-container');
    const imgToClone = productCard ? productCard.querySelector('img') : null;
    const cartIcon = document.querySelector('.cart-link'); // Targets the header cart link

    if (imgToClone && cartIcon) {
        // 2. Create a temporary clone for the animation
        const clone = imgToClone.cloneNode();
        const rect = imgToClone.getBoundingClientRect();
        const cartRect = cartIcon.getBoundingClientRect();

        // 3. Initial placement of the clone directly over the original image
        clone.classList.add('flying-item');
        clone.style.width = rect.width + 'px';
        clone.style.height = rect.height + 'px';
        clone.style.top = rect.top + 'px';
        clone.style.left = rect.left + 'px';

        document.body.appendChild(clone);

        // 4. Trigger the flight to the cart icon
        // A small timeout ensures the browser registers the initial position before animating
        setTimeout(() => {
            clone.style.top = (cartRect.top + 10) + 'px';
            clone.style.left = (cartRect.left + 10) + 'px';
            clone.style.width = '20px';
            clone.style.height = '20px';
            clone.style.opacity = '0.2';
            clone.style.transform = 'rotate(360deg)';
        }, 10);

        // 5. Remove clone after animation completes (matches CSS transition time)
        setTimeout(() => {
            clone.remove();
        }, 800);
    }

    // 6. Perform the background request to update the session cart
    fetch('/cart/add/' + productId)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // Update the cart badge in the header
                const cartBadge = document.querySelector('.cart-count');
                if (cartBadge) {
                    cartBadge.textContent = data.cartCount;

                    // Satisfaction feedback: A tiny "pop" animation on the badge
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