document.addEventListener('DOMContentLoaded', () => {

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

    function toggleEditProduct(productId) {
        const viewEl = document.getElementById('view_' + productId);
        const editEl = document.getElementById('edit_' + productId);
        if (viewEl) viewEl.style.display = 'none';
        if (editEl) editEl.style.display = 'block';
    }

    function cancelEditProduct(productId) {
        const viewEl = document.getElementById('view_' + productId);
        const editEl = document.getElementById('edit_' + productId);
        const form = editEl ? editEl.querySelector('form') : null;

        if (form) form.reset();
        if (editEl) editEl.style.display = 'none';
        if (viewEl) viewEl.style.display = 'block';
    }
});