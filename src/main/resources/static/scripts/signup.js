// src/main/resources/static/scripts/signup-validation.js
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

  document.addEventListener('DOMContentLoaded', function () {
    const email = document.getElementById('email');
    const phone = document.getElementById('phone');
    const emailFeedback = document.getElementById('emailFeedback');
    const phoneFeedback = document.getElementById('phoneFeedback');

    // If this script is included on a page that doesn't have these fields, do nothing
    if (!email || !phone || !emailFeedback || !phoneFeedback) return;

    email.addEventListener('input', () =>
      updateFeedback(email, emailFeedback, 'Please enter a valid email (e.g., name@example.com).')
    );

    phone.addEventListener('input', () =>
      updateFeedback(phone, phoneFeedback, 'Please enter a valid phone (e.g., 555-555-5555).')
    );
  });
})();