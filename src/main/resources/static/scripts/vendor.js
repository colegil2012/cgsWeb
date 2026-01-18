document.addEventListener('DOMContentLoaded', () => {
    const addRowBtn = document.getElementById('addRowBtn');
    const itemsContainer = document.getElementById('itemsContainer');

    if (addRowBtn && itemsContainer) {
        addRowBtn.addEventListener('click', () => {
            const index = itemsContainer.children.length;
            // Clone the first row as a template
            const newRow = itemsContainer.children[0].cloneNode(true);

            // Update Header and Index
            newRow.setAttribute('data-index', index);
            const headerSpan = newRow.querySelector('.row-number span');
            if (headerSpan) headerSpan.textContent = `Product #${index + 1}`;

            // Add Remove Button if it doesn't exist (it won't on the first clone)
            if (!newRow.querySelector('.btn-remove-row')) {
                const removeBtn = document.createElement('span');
                removeBtn.className = 'btn-remove-row';
                removeBtn.textContent = '-';
                removeBtn.title = 'Remove Product';
                removeBtn.onclick = function() {
                    this.closest('.vendor-item-row').remove();
                    reindexRows();
                };
                newRow.querySelector('.row-number').appendChild(removeBtn);
            }

            // Update Input Names (e.g., products[0].name -> products[1].name)
            newRow.querySelectorAll('input, select, textarea').forEach(el => {
                const name = el.getAttribute('name');
                if (name) {
                    // This regex will now catch both products[0] and productImages[0]
                    el.setAttribute('name', name.replace(/\[\d+\]/, `[${index}]`));

                    if (el.type === 'file') {
                        el.value = ''; // Clear file selection for new row
                    } else if (el.tagName !== 'SELECT') {
                        el.value = '';
                    }
                }
            });

            itemsContainer.appendChild(newRow);
        });
    }

    // Helper to keep numbers consistent if one in the middle is removed
    function reindexRows() {
        Array.from(itemsContainer.children).forEach((row, idx) => {
            row.querySelector('.row-number span').textContent = `Product #${idx + 1}`;
            row.querySelectorAll('input, select, textarea').forEach(el => {
                const name = el.getAttribute('name');
                if (name) {
                    el.setAttribute('name', name.replace(/\[\d+\]/, `[${idx}]`));
                }
            });
        });
    }
});