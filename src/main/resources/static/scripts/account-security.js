(() => {
  const ADDRESS_TYPES = ["SHIPPING", "BILLING", "ALTERNATE"];

  const initModal = ({ openBtnId, overlayId, closeBtnId, cancelBtnId, focusInputId }) => {
    const openBtn = document.getElementById(openBtnId);
    const overlay = document.getElementById(overlayId);
    const closeBtn = document.getElementById(closeBtnId);
    const cancelBtn = document.getElementById(cancelBtnId);

    if (!openBtn || !overlay) return null;

    const open = () => {
      overlay.style.display = 'flex';
      const focusEl = focusInputId ? document.getElementById(focusInputId) : null;
      if (focusEl) focusEl.focus();
    };

    const close = () => {
      overlay.style.display = 'none';
      const form = overlay.querySelector('form');
      if (form) form.reset();
    };

    openBtn.addEventListener('click', open);
    if (closeBtn) closeBtn.addEventListener('click', close);
    if (cancelBtn) cancelBtn.addEventListener('click', close);

    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) close();
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && overlay.style.display !== 'none') close();
    });

    return { open, close, overlay };
  };

  // Password modal (Security tab)
  initModal({
    openBtnId: 'openChangePassword',
    overlayId: 'changePasswordOverlay',
    closeBtnId: 'closeChangePassword',
    cancelBtnId: 'cancelChangePassword',
    focusInputId: 'oldPassword',
  });

  // Address modal (Addresses tab)
  const addrModal = initModal({
    openBtnId: 'openUpdateAddress',
    overlayId: 'updateAddressOverlay',
    closeBtnId: 'closeUpdateAddress',
    cancelBtnId: 'cancelUpdateAddress',
    focusInputId: null,
  });

  // Dynamic address blocks inside address modal
  if (addrModal) {
    const addBtn = document.getElementById('addAddressBlockBtn');
    const container = document.getElementById('newAddressesContainer');

    const renderTypeOptions = (selectedValue) => {
      const normalized = (selectedValue || "SHIPPING").toString().trim().toUpperCase();
      return ADDRESS_TYPES.map((t) => {
        const selectedAttr = t === normalized ? " selected" : "";
        return `<option value="${t}"${selectedAttr}>${t}</option>`;
      }).join("");
    };

    const buildAddressBlock = (index) => {
      const wrapper = document.createElement('div');
      wrapper.className = 'address-edit-card';
      wrapper.dataset.index = String(index);

      wrapper.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; gap:10px;">
          <strong>New Address #${index + 1}</strong>
          <button type="button" class="btn-small" data-remove="1">Remove</button>
        </div>

        <div class="form-control">
          <label>Type</label>
          <select name="newAddresses[${index}].type" required>
            ${renderTypeOptions("SHIPPING")}
          </select>
        </div>

        <div class="form-control">
          <label>Street</label>
          <input type="text" name="newAddresses[${index}].street" />
        </div>

        <div class="form-control">
          <label>City</label>
          <input type="text" name="newAddresses[${index}].city" />
        </div>

        <div class="form-control">
          <label>State</label>
          <input type="text" name="newAddresses[${index}].state" required="required" pattern="^[A-Za-z]{2}$" maxlength="2" title="Use 2-letter state code (e.g., KY)" />
        </div>

        <div class="form-control">
          <label>Zip</label>
          <input type="text" name="newAddresses[${index}].zip" required="required" pattern="^\\d{5}(?:-\\d{4})?$" inputmode="numeric" />
        </div>

        <div class="form-control" style="display:flex; align-items:center; gap:10px;">
          <input type="checkbox" name="newAddresses[${index}].default" value="true" />
          <label style="margin:0;">Make default</label>
        </div>
      `;

      const removeBtn = wrapper.querySelector('[data-remove="1"]');
      if (removeBtn) {
        removeBtn.addEventListener('click', () => {
          wrapper.remove();
        });
      }

      return wrapper;
    };

    const nextIndex = () => {
      if (!container) return 0;
      const indices = [...container.querySelectorAll('.address-edit-card')]
        .map((el) => Number(el.dataset.index))
        .filter((n) => Number.isFinite(n));
      return indices.length ? Math.max(...indices) + 1 : 0;
    };

    if (addBtn && container) {
      addBtn.addEventListener('click', () => {
        const index = nextIndex();
        container.appendChild(buildAddressBlock(index));
      });
    }
  }
})();