document.addEventListener('DOMContentLoaded', function() {
    const addButtons = document.querySelectorAll('.btn-small');
    
    addButtons.forEach(button => {
        if (button.textContent.trim() === 'Add to Cart') {
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
                    }
                })
                .catch(error => console.error('Error adding to cart:', error));
            });
        }
    });
});