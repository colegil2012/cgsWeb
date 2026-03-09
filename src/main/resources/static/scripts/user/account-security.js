(() => {
  const ADDRESS_TYPES = ["SHIPPING", "BILLING", "ALTERNATE"];

  const initModal = ({ openBtnId, overlayId, closeBtnId, cancelBtnId, focusInputId }) => {
    const openBtn = document.getElementById(openBtnId);
    const overlay = document.getElementById(overlayId);
    const closeBtn = document.getElementById(closeBtnId);
    const cancelBtn = document.getElementById(cancelBtnId);

    if (!openBtn || !overlay) return null;

    const open = () => {
      overlay.style.display = "flex";
      const focusEl = focusInputId ? document.getElementById(focusInputId) : null;
      if (focusEl) focusEl.focus();
    };

    const close = () => {
      overlay.style.display = "none";
      const form = overlay.querySelector("form");
      if (form) form.reset();
    };

    openBtn.addEventListener("click", open);
    if (closeBtn) closeBtn.addEventListener("click", close);
    if (cancelBtn) cancelBtn.addEventListener("click", close);

    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) close();
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && overlay.style.display !== "none") close();
    });

    return { open, close, overlay };
  };

  // Password modal (Security tab)
  initModal({
    openBtnId: "openChangePassword",
    overlayId: "changePasswordOverlay",
    closeBtnId: "closeChangePassword",
    cancelBtnId: "cancelChangePassword",
    focusInputId: "oldPassword",
  });

})();