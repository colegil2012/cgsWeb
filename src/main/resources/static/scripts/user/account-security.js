/**
 * account-security.js
 *
 * Wires the "Change Password" modal on the Account → Security tab.
 * Modal mechanics (open/close/Escape/overlay-click/form-reset) come from
 * the shared {@code window.CGS.Modal} helper in /scripts/_modal.js, so this
 * file only carries the page-specific configuration.
 */
(() => {
  if (!window.CGS || typeof window.CGS.Modal !== 'function') {
    console.warn('[account-security] window.CGS.Modal not loaded — is /scripts/_modal.js included before this file?');
    return;
  }

  // Password modal (Security tab)
  window.CGS.Modal({
    openBtnId:   'openChangePassword',
    overlayId:   'changePasswordOverlay',
    closeBtnId:  'closeChangePassword',
    cancelBtnId: 'cancelChangePassword',
    focusInputId: 'oldPassword',
    dialogLabel: 'Change your password',
  });
})();