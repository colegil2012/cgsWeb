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
                    el.setAttribute('name', name.replace(/\[\d+\]/, `[${index}]`));

                    if (el.type === 'file') {
                        el.value = '';
                    } else if (el.tagName !== 'SELECT') {
                        el.value = '';
                    }
                }
            });

            itemsContainer.appendChild(newRow);
        });
    }

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

    // Logo upload (non-AJAX): pick file -> submit form -> redirect back with flash messages
    const changeLogoBtn = document.getElementById('changeLogoBtn');
    const vendorLogoUpload = document.getElementById('vendorLogoUpload');
    const vendorLogoForm = document.getElementById('vendorLogoForm');

    if (changeLogoBtn && vendorLogoUpload) {
        changeLogoBtn.addEventListener('click', () => vendorLogoUpload.click());
    }

    if (vendorLogoUpload && vendorLogoForm) {
        vendorLogoUpload.addEventListener('change', () => {
            const file = vendorLogoUpload.files && vendorLogoUpload.files[0];
            if (!file) return;
            vendorLogoForm.submit();
        });
    }

    // Vendor Settings edit toggle
    const editBtn = document.getElementById('editVendorSettingsBtn');
    const cancelBtn = document.getElementById('cancelVendorSettingsBtn');
    const saveBtn = document.getElementById('saveVendorSettingsBtn');
    const settingsGrid = document.getElementById('vendorSettingsGrid');

    if (editBtn && cancelBtn && saveBtn && settingsGrid) {
        const setEditing = (editing) => {
            const displays = settingsGrid.querySelectorAll('[data-setting-display]');
            const inputs = settingsGrid.querySelectorAll('[data-setting-input]');

            displays.forEach(el => el.style.display = editing ? 'none' : 'block');

            inputs.forEach(input => {
                input.style.display = editing ? 'block' : 'none';
                if (editing) {
                    input.removeAttribute('disabled');
                } else {
                    input.setAttribute('disabled', 'disabled');
                }
            });

            editBtn.style.display = editing ? 'none' : 'inline-block';
            cancelBtn.style.display = editing ? 'inline-block' : 'none';
            saveBtn.style.display = editing ? 'inline-block' : 'none';
        };

        editBtn.addEventListener('click', () => setEditing(true));

        cancelBtn.addEventListener('click', () => {
            // revert inputs back to their original values from the readonly displays
            const displays = settingsGrid.querySelectorAll('[data-setting-display]');
            displays.forEach(display => {
                const key = display.getAttribute('data-setting-display');
                const input = settingsGrid.querySelector(`[data-setting-input="${key}"]`);
                if (input) input.value = (display.textContent || '').trim();
            });

            setEditing(false);
        });
    }
});