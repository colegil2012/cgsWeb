// src/main/resources/static/scripts/signup.js
(function () {
  function updateFeedback(input, feedbackEl, message) {
    // Optional fields: don't show errors when empty
    if (!input.value) {
      feedbackEl.textContent = '';
      input.classList.remove('invalid');
      return;
    }

    if (input.checkValidity()) {
      feedbackEl.textContent = '';
      input.classList.remove('invalid');
    } else {
      feedbackEl.textContent = message || input.validationMessage;
      input.classList.add('invalid');
    }
  }

  function setUsernameStatus(statusEl, feedbackEl, state, text) {
    // state: 'idle' | 'loading' | 'available' | 'unavailable' | 'error'
    if (!statusEl || !feedbackEl) return;

    statusEl.textContent = '';
    feedbackEl.textContent = '';

    if (state === 'idle') return;

    if (state === 'loading') {
      statusEl.textContent = '…';
      statusEl.style.color = '#666';
      feedbackEl.textContent = 'Checking username...';
      feedbackEl.style.color = '#666';
      return;
    }

    if (state === 'available') {
      statusEl.textContent = '✓';
      statusEl.style.color = '#1a7f37';
      feedbackEl.textContent = text || 'Username is available.';
      feedbackEl.style.color = '#1a7f37';
      return;
    }

    if (state === 'unavailable') {
      statusEl.textContent = '✕';
      statusEl.style.color = '#d1242f';
      feedbackEl.textContent = text || 'Username is already taken.';
      feedbackEl.style.color = '#d1242f';
      return;
    }

    statusEl.textContent = '✕';
    statusEl.style.color = '#d1242f';
    feedbackEl.textContent = text || 'Could not validate username right now.';
  }

  function setPasswordStatus(statusEl, feedbackEl, state, text) {
    // state: 'idle' | 'match' | 'mismatch'
    if (!feedbackEl) return;

    if (statusEl) statusEl.textContent = '';
    feedbackEl.textContent = '';

    if (state === 'idle') return;

    if (state === 'match') {
      if (statusEl) {
        statusEl.textContent = '✓';
        statusEl.style.color = '#1a7f37';
      }
      feedbackEl.textContent = text || 'Passwords match.';
      feedbackEl.style.color = '#1a7f37';
      return;
    }

    if (statusEl) {
      statusEl.textContent = '✕';
      statusEl.style.color = '#d1242f';
    }
    feedbackEl.textContent = text || 'Passwords do not match.';
    feedbackEl.style.color = '#d1242f';
  }

  document.addEventListener('DOMContentLoaded', function () {
    // Optional email/phone validation wiring
    const email = document.getElementById('email');
    const phone = document.getElementById('phone');
    const emailFeedback = document.getElementById('emailFeedback');
    const phoneFeedback = document.getElementById('phoneFeedback');

    if (email && emailFeedback) {
      email.addEventListener('input', () =>
        updateFeedback(email, emailFeedback, 'Please enter a valid email (e.g., name@example.com).')
      );
    }

    function formatPhoneToParenDash(value) {
      const digits = String(value || '').replace(/\D/g, '').slice(0, 10);

      if (digits.length === 0) return '';

      if (digits.length <= 3) {
        return `(${digits}`;
      }

      if (digits.length <= 6) {
        return `(${digits.slice(0, 3)})-${digits.slice(3)}`;
      }

      return `(${digits.slice(0, 3)})-${digits.slice(3, 6)}-${digits.slice(6)}`;
    }

    function validatePhoneField() {
      if (!phone) return;

      // Keep phone optional: if empty, it's valid and no error shown
      if (!phone.value) {
        phone.setCustomValidity('');
        return;
      }

      // If they haven't typed a complete 10 digits yet, mark invalid (so feedback shows)
      const digits = phone.value.replace(/\D/g, '');
      if (digits.length < 10) {
        phone.setCustomValidity('Please enter a 10-digit phone number.');
        return;
      }

      // Let the pattern/checkValidity handle the final strict format
      phone.setCustomValidity('');
    }

    if (phone && phoneFeedback) {
      phone.addEventListener('input', () => {
        const formatted = formatPhoneToParenDash(phone.value);

        // Preserve caret reasonably well by adjusting relative to length change
        const oldPos = phone.selectionStart;
        const oldLen = phone.value.length;

        phone.value = formatted;

        const newLen = phone.value.length;
        const newPos = Math.max(0, (oldPos || 0) + (newLen - oldLen));
        phone.setSelectionRange(newPos, newPos);

        validatePhoneField();
        updateFeedback(phone, phoneFeedback, 'Please enter a valid phone (e.g., (555)-555-5555).');
      });

      phone.addEventListener('blur', () => {
        validatePhoneField();
        updateFeedback(phone, phoneFeedback, 'Please enter a valid phone (e.g., (555)-555-5555).');
      });
    }

    // Password match wiring
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const passwordStatus = document.getElementById('passwordStatus');
    const passwordFeedback = document.getElementById('passwordFeedback');

    let passwordsMatch = false;

    function validatePasswords() {
      if (!password || !confirmPassword || !passwordFeedback) return false;

      const p = password.value;
      const c = confirmPassword.value;

      // Don't show anything until user has started interacting
      if (!p && !c) {
        confirmPassword.setCustomValidity('');
        confirmPassword.classList.remove('invalid');
        setPasswordStatus(passwordStatus, passwordFeedback, 'idle');
        passwordsMatch = false;
        return passwordsMatch;
      }

      // Only start showing "mismatch" once confirm has something typed (avoids yelling while they type password)
      if (!c) {
        confirmPassword.setCustomValidity('');
        confirmPassword.classList.remove('invalid');
        setPasswordStatus(passwordStatus, passwordFeedback, 'idle');
        passwordsMatch = false;
        return passwordsMatch;
      }

      if (p === c) {
        confirmPassword.setCustomValidity('');
        confirmPassword.classList.remove('invalid');
        setPasswordStatus(passwordStatus, passwordFeedback, 'match');
        passwordsMatch = true;
        return passwordsMatch;
      }

      // Make the browser consider the field invalid, so form submit is blocked too
      confirmPassword.setCustomValidity('Passwords must match.');
      confirmPassword.classList.add('invalid');
      setPasswordStatus(passwordStatus, passwordFeedback, 'mismatch', 'Passwords must match.');
      passwordsMatch = false;
      return passwordsMatch;
    }

    if (password && confirmPassword) {
      password.addEventListener('input', () => {
        validatePasswords();
        recomputeSubmitEnabled();
      });
      confirmPassword.addEventListener('input', () => {
        validatePasswords();
        recomputeSubmitEnabled();
      });
      confirmPassword.addEventListener('blur', () => {
        validatePasswords();
        recomputeSubmitEnabled();
      });
    }


    // Username availability check wiring + disable submit until confirmed available
    const username = document.getElementById('username');
    const usernameStatus = document.getElementById('usernameStatus');
    const usernameFeedback = document.getElementById('usernameFeedback');
    const signupSubmit = document.getElementById('signupSubmit');

    // If this script is included on a page that doesn't have these fields, do nothing
    if (!username || !usernameStatus || !usernameFeedback || !signupSubmit) return;

    function setSubmitEnabled(enabled) {
      signupSubmit.disabled = !enabled;
      signupSubmit.style.opacity = enabled ? '1' : '0.6';
      signupSubmit.style.cursor = enabled ? 'pointer' : 'not-allowed';
    }

    let lastConfirmedAvailableValue = '';

    function recomputeSubmitEnabled() {
      // If password fields exist on the page, require a match. Otherwise only gate on username.
      const passwordOk = (password && confirmPassword) ? validatePasswords() : true;
      const usernameOk = username.value.trim() && username.value.trim() === lastConfirmedAvailableValue;
      setSubmitEnabled(Boolean(usernameOk && passwordOk));
    }

    // Start disabled until username is confirmed available
    setSubmitEnabled(false);

    let debounceTimer = null;
    let inFlightController = null;

    let lastValueChecked = '';

    async function checkUsernameAvailability(value) {
      const trimmed = value.trim();

      if (!trimmed) {
        lastValueChecked = '';
        lastConfirmedAvailableValue = '';
        if (inFlightController) inFlightController.abort();
        setUsernameStatus(usernameStatus, usernameFeedback, 'idle');
        setSubmitEnabled(false);
        return;
      }

      // Avoid repeating the same request
      if (trimmed === lastValueChecked) return;
      lastValueChecked = trimmed;

      // Cancel previous request (if any)
      if (inFlightController) inFlightController.abort();
      inFlightController = new AbortController();

      setUsernameStatus(usernameStatus, usernameFeedback, 'loading');
      setSubmitEnabled(false);

      try {
        const res = await fetch('/signup/checkUsername', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
          body: new URLSearchParams({ username: trimmed }).toString(),
          signal: inFlightController.signal,
        });

        if (!res.ok) throw new Error('Non-OK response');

        // Endpoint returns text: "true" (exists/taken) or "false" (not taken)
        const existsText = (await res.text()).trim().toLowerCase();
        if (existsText !== 'true' && existsText !== 'false') {
          throw new Error('Unexpected response body: ' + existsText);
        }
        const exists = (existsText === 'true');
        const available = !exists;

        // Only apply result if the input still matches what we checked
        if (username.value.trim() !== trimmed) return;

        if (available) {
          lastConfirmedAvailableValue = trimmed;
          setUsernameStatus(usernameStatus, usernameFeedback, 'available');
          setSubmitEnabled(true);
        } else {
          lastConfirmedAvailableValue = '';
          setUsernameStatus(usernameStatus, usernameFeedback, 'unavailable');
          username.classList.add('invalid');
          setSubmitEnabled(false);
        }
      } catch (e) {
        // Ignore aborts (they happen during normal typing)
        if (e && e.name === 'AbortError') return;
        lastConfirmedAvailableValue = '';
        setUsernameStatus(usernameStatus, usernameFeedback, 'error');
        setSubmitEnabled(false);
      }
    }

    username.addEventListener('input', () => {
      // As soon as the user types, require re-confirmation (unless they typed back to confirmed value)
      username.classList.remove('invalid'); //Clear any previous invalid state
      const current = username.value.trim();
      if (current !== lastConfirmedAvailableValue) setSubmitEnabled(false);

      window.clearTimeout(debounceTimer);
      debounceTimer = window.setTimeout(() => {
        checkUsernameAvailability(username.value);
      }, 300);
    });

    username.addEventListener('blur', () => {
      // On blur, validate immediately (no debounce)
      window.clearTimeout(debounceTimer);
      checkUsernameAvailability(username.value);
    });

    validatePasswords();
    recomputeSubmitEnabled();
  });
})();