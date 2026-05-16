/*
 * Assign-user modal for the vendor detail page.
 *
 * Flow:
 *   1. "+ Assign a User" opens the modal.
 *   2. Typing in the search box (debounced) calls
 *      GET /admin/vendors/users/search?q=... which returns JSON:
 *        [{ id, username, email, displayName, alreadyAssigned }, ...]
 *   3. Each result renders as a row. Rows for users already assigned to a
 *      vendor are shown disabled (the server would block the assign anyway —
 *      this just makes it visible before the click).
 *   4. Clicking an enabled result fills the hidden userId on #assign-user-form
 *      and submits it (a normal POST, so CSRF + redirect/flash all work).
 *
 * No framework — plain DOM. Loaded only on the vendor detail page.
 */
(function () {
    'use strict';

    var modal = document.getElementById('assign-modal');
    var openBtn = document.getElementById('open-assign-modal');
    var closeBtn = document.getElementById('close-assign-modal');
    var input = document.getElementById('user-search-input');
    var results = document.getElementById('user-search-results');
    var form = document.getElementById('assign-user-form');
    var userIdField = document.getElementById('assign-user-id');

    // If any piece is missing, this isn't the vendor detail page — bail quietly.
    if (!modal || !openBtn || !closeBtn || !input || !results || !form || !userIdField) {
        return;
    }

    var debounceTimer = null;
    var DEBOUNCE_MS = 250;
    var MIN_QUERY = 2;

    /* ---- Modal open/close ---- */

    function openModal() {
        modal.hidden = false;
        input.value = '';
        setHint('Start typing to search.');
        // Focus after the browser has painted the now-visible modal.
        window.setTimeout(function () { input.focus(); }, 0);
    }

    function closeModal() {
        modal.hidden = true;
        if (debounceTimer) {
            window.clearTimeout(debounceTimer);
            debounceTimer = null;
        }
    }

    openBtn.addEventListener('click', openModal);
    closeBtn.addEventListener('click', closeModal);

    // Backdrop click (outside the modal box) closes.
    modal.addEventListener('click', function (e) {
        if (e.target === modal) closeModal();
    });

    // Escape closes.
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !modal.hidden) closeModal();
    });

    /* ---- Search ---- */

    input.addEventListener('input', function () {
        var query = input.value.trim();
        if (debounceTimer) window.clearTimeout(debounceTimer);

        if (query.length < MIN_QUERY) {
            setHint('Type at least ' + MIN_QUERY + ' characters.');
            return;
        }

        setHint('Searching…');
        debounceTimer = window.setTimeout(function () {
            runSearch(query);
        }, DEBOUNCE_MS);
    });

    function runSearch(query) {
        fetch('/admin/vendors/users/search?q=' + encodeURIComponent(query), {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        })
            .then(function (res) {
                if (!res.ok) throw new Error('Search failed (' + res.status + ')');
                return res.json();
            })
            .then(function (data) {
                renderResults(Array.isArray(data) ? data : []);
            })
            .catch(function (err) {
                setHint('Search error. Try again.');
                console.error(err);
            });
    }

    /* ---- Render ---- */

    function renderResults(users) {
        results.innerHTML = '';

        if (users.length === 0) {
            setHint('No users match that search.');
            return;
        }

        users.forEach(function (u) {
            var row = document.createElement('div');
            row.className = 'admin-modal-result'
                + (u.alreadyAssigned ? ' admin-modal-result-disabled' : '');

            var info = document.createElement('div');
            info.className = 'admin-modal-result-info';

            var name = document.createElement('span');
            name.className = 'admin-modal-result-name';
            name.textContent = u.displayName || u.username || '(unknown)';
            info.appendChild(name);

            var meta = document.createElement('span');
            meta.className = 'admin-modal-result-meta';
            meta.textContent = [u.username, u.email].filter(Boolean).join(' · ');
            info.appendChild(meta);

            row.appendChild(info);

            if (u.alreadyAssigned) {
                var note = document.createElement('span');
                note.className = 'admin-modal-result-note';
                note.textContent = 'Already assigned to a vendor';
                row.appendChild(note);
            } else {
                var pick = document.createElement('button');
                pick.type = 'button';
                pick.className = 'admin-btn admin-btn-primary admin-btn-sm';
                pick.textContent = 'Assign';
                pick.addEventListener('click', function () {
                    selectUser(u.id);
                });
                row.appendChild(pick);
            }

            results.appendChild(row);
        });
    }

    function selectUser(userId) {
        if (!userId) return;
        userIdField.value = userId;
        form.submit();
    }

    function setHint(text) {
        results.innerHTML = '';
        var hint = document.createElement('p');
        hint.className = 'admin-modal-hint';
        hint.textContent = text;
        results.appendChild(hint);
    }
})();