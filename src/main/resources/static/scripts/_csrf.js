/**
 * Shared CSRF helpers for fetch-based POST/PUT/DELETE calls.
 * Reads token + header name from the <meta> tags rendered in layout.tpl.
 *
 * Usage:
 *   fetch('/cart/add/123', {
 *     method: 'POST',
 *     headers: { ...window.CGS.csrfHeaders(), 'X-Requested-With': 'XMLHttpRequest' }
 *   });
 */
(function () {
  window.CGS = window.CGS || {};

  function meta(name) {
    const el = document.querySelector(`meta[name="${name}"]`);
    return el ? el.getAttribute('content') : '';
  }

  window.CGS.csrfToken = function () {
    return meta('_csrf');
  };

  window.CGS.csrfHeaderName = function () {
    return meta('_csrf_header') || 'X-CSRF-TOKEN';
  };

  window.CGS.csrfHeaders = function () {
    const h = {};
    const token = window.CGS.csrfToken();
    if (token) h[window.CGS.csrfHeaderName()] = token;
    return h;
  };
})();