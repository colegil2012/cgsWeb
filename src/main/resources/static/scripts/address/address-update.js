(() => {
  const ADDRESS_TYPES = ["SHIPPING", "BILLING", "ALTERNATE"];

  if (!window.CGS || typeof window.CGS.Modal !== 'function') {
    console.warn('[address-update] window.CGS.Modal not loaded — is /scripts/_modal.js included in <head>?');
    return;
  }

  // Keep in sync with com.ua.estore.cgsWeb.util.UsStates.
  // The server still validates with /^[A-Za-z]{2}$/, so the dropdown is just UX.
  const US_STATES = [
    ["IN", "IN – Indiana"], ["KY", "KY – Kentucky"], ["OH", "OH – Ohio"],
  ];

  /* =============================================================================
   * Generic UI helpers
   * ============================================================================= */

  const renderStateOptions = (selectedCode) => {
    const sel = (selectedCode || "").toString().trim().toUpperCase();
    const head = `<option value="">— Select state —</option>`;
    const rest = US_STATES.map(([code, label]) =>
        `<option value="${code}"${code === sel ? " selected" : ""}>${label}</option>`
    ).join("");
    return head + rest;
  };

  const renderTypeOptions = (selectedValue) => {
    const sel = (selectedValue || "SHIPPING").toString().trim().toUpperCase();
    return ADDRESS_TYPES.map((t) =>
        `<option value="${t}"${t === sel ? " selected" : ""}>${t}</option>`
    ).join("");
  };

  const escapeHtml = (s) =>
      String(s ?? "")
          .replaceAll("&", "&amp;")
          .replaceAll("<", "&lt;")
          .replaceAll(">", "&gt;");

  const setByName = (root, name, value) => {
    const el = root.querySelector(`[name="${CSS.escape(name)}"]`);
    if (el) el.value = value ?? "";
  };

  /* =============================================================================
   * Boot the address modal (the only modal this script manages).
   * ============================================================================= */

  const addrModal = window.CGS.Modal({
    openBtnId: "openUpdateAddress",
    overlayId: "updateAddressOverlay",
    closeBtnId: "closeUpdateAddress",
    cancelBtnId: "cancelUpdateAddress",
    focusInputId: null,
    dialogLabel: "Edit your saved Addresses",
  });

  if (!addrModal) return;

  const API_SUGGEST = "/api/address/suggest";
  const API_RESOLVE = "/api/address/resolve";

  /* =============================================================================
   * Default-checkbox: only one address can be default at a time.
   * ============================================================================= */

  const isDefaultCheckbox = (el) =>
      el && el.tagName === "INPUT" && el.type === "checkbox" &&
      typeof el.name === "string" && el.name.endsWith(".default");

  addrModal.overlay.addEventListener("change", (e) => {
    const el = e.target;
    if (!isDefaultCheckbox(el) || !el.checked) return;

    addrModal.overlay
        .querySelectorAll('input[type="checkbox"][name$=".default"]')
        .forEach((cb) => { if (cb !== el) cb.checked = false; });
  });

  /* =============================================================================
   * Dynamic "new address" blocks.
   * ============================================================================= */

  const addBtn    = document.getElementById("addAddressBlockBtn");
  const container = document.getElementById("newAddressesContainer");

  const buildAddressBlock = (index) => {
    const wrapper = document.createElement("div");
    wrapper.className = "address-edit-card";
    wrapper.dataset.index = String(index);

    wrapper.innerHTML = `
      <div class="address-edit-card-header">
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
        <input type="text" name="newAddresses[${index}].street1" autocomplete="off" />
      </div>

      <div class="form-control">
        <label>Apt / Suite (optional)</label>
        <input type="text" name="newAddresses[${index}].street2" autocomplete="off" />
      </div>

      <div class="form-control">
        <label>City</label>
        <input type="text" name="newAddresses[${index}].city" />
      </div>

      <div class="form-control">
        <label>State</label>
        <select name="newAddresses[${index}].state" required>
          ${renderStateOptions("")}
        </select>
      </div>

      <div class="form-control">
        <label>Zip</label>
        <input type="text" name="newAddresses[${index}].zip" required="required"
               pattern="^\\d{5}(?:-\\d{4})?$" inputmode="numeric" />
      </div>

      <div class="update-address-footer">
        <div class="form-control">
          <input type="checkbox" name="newAddresses[${index}].default" value="true" />
          <label style="margin:0;">Make default</label>
        </div>
      </div>
    `;

    const removeBtn = wrapper.querySelector('[data-remove="1"]');
    if (removeBtn) removeBtn.addEventListener("click", () => wrapper.remove());

    return wrapper;
  };

  const nextIndex = () => {
    if (!container) return 0;
    const indices = [...container.querySelectorAll(".address-edit-card")]
        .map((el) => Number(el.dataset.index))
        .filter((n) => Number.isFinite(n));
    return indices.length ? Math.max(...indices) + 1 : 0;
  };

  if (addBtn && container) {
    addBtn.addEventListener("click", () => {
      container.appendChild(buildAddressBlock(nextIndex()));
    });
  }

  /* =============================================================================
   * Autocomplete: dropdown + autofill.
   * ============================================================================= */

  const isStreetInput = (el) =>
      el && el.tagName === "INPUT" && el.type === "text" &&
      typeof el.name === "string" && el.name.endsWith(".street1");

  /** "addresses[0].street1" -> "addresses[0]." (used to build sibling field names) */
  const prefixFromStreetName = (streetName) => {
    if (!streetName || !streetName.endsWith(".street1")) return null;
    return streetName.slice(0, -".street1".length) + ".";
  };

  const ensureSuggestBox = (streetInputEl) => {
    const parent = streetInputEl.closest(".form-control") || streetInputEl.parentElement;
    if (!parent) return null;

    parent.style.position = parent.style.position || "relative";

    let box = parent.querySelector(".addr-suggest-box");
    if (box) return box;

    box = document.createElement("div");
    box.className = "addr-suggest-box";
    Object.assign(box.style, {
      position: "absolute", left: "0", right: "0", top: "100%",
      marginTop: "6px", background: "#fff",
      border: "1px solid rgba(0,0,0,0.15)", borderRadius: "8px",
      boxShadow: "0 10px 30px rgba(0,0,0,0.12)",
      zIndex: "9999", display: "none",
      maxHeight: "220px", overflowY: "auto",
    });

    parent.appendChild(box);
    return box;
  };

  const hideSuggestBox = (streetInputEl) => {
    const parent = streetInputEl.closest(".form-control") || streetInputEl.parentElement;
    const box = parent ? parent.querySelector(".addr-suggest-box") : null;
    if (box) {
      box.style.display = "none";
      box.innerHTML = "";
    }
  };

  const fetchSuggestions = async (q) => {
    const res = await fetch(`${API_SUGGEST}?q=${encodeURIComponent(q)}`, {
      headers: { Accept: "application/json" },
    });
    if (!res.ok) return [];
    const data = await res.json();
    return Array.isArray(data) ? data : [];
  };

  /** Google-flow fallback: /suggest only carries label+placeId, so we ask /resolve. */
  const resolvePlaceId = async (placeId) => {
    const res = await fetch(`${API_RESOLVE}?placeId=${encodeURIComponent(placeId)}`, {
      headers: { Accept: "application/json" },
    });
    if (!res.ok) return null;
    return await res.json(); // AddressDTO {street1, street2, city, state, zip}
  };

  /**
   * Write a resolved AddressDTO into the corresponding form fields.
   *
   * Always overwrites street1 (the click puts the full label in there momentarily;
   * we want it replaced with just the parsed street). Other fields are only written
   * when present so we don't blank a user-edited city/zip the resolver doesn't return.
   */
  const applyResolvedAddress = (scope, prefix, resolved, streetInputEl) => {
    streetInputEl.value = resolved.street1 || "";
    setByName(scope, `${prefix}street1`, resolved.street1 || "");
    if (resolved.street2) setByName(scope, `${prefix}street2`, resolved.street2);
    if (resolved.city)    setByName(scope, `${prefix}city`,    resolved.city);
    if (resolved.state)   setByName(scope, `${prefix}state`,   resolved.state);
    if (resolved.zip)     setByName(scope, `${prefix}zip`,     resolved.zip);
  };

  const renderSuggestions = (streetInputEl, suggestions) => {
    const box = ensureSuggestBox(streetInputEl);
    if (!box) return;

    if (!suggestions || !suggestions.length) {
      box.style.display = "none";
      box.innerHTML = "";
      return;
    }

    // Snapshot the suggestions array so a later request can't mutate what we render.
    const items = suggestions.slice();

    box.innerHTML = items
        .map((s, i) => {
          const label   = escapeHtml(s.label);
          const placeId = escapeHtml(s.placeId);
          return `<div class="addr-suggest-item" data-place-id="${placeId}" data-suggestion-index="${i}" style="padding:10px 12px; cursor:pointer;">${label}</div>`;
        })
        .join("");

    box.style.display = "block";

    box.querySelectorAll(".addr-suggest-item").forEach((item) => {
      item.addEventListener("mousedown", async (e) => {
        // mousedown so the click registers before blur tears the box down.
        e.preventDefault();

        const placeId = item.getAttribute("data-place-id");
        const idx     = Number(item.getAttribute("data-suggestion-index"));
        const picked  = Number.isFinite(idx) ? items[idx] : null;

        hideSuggestBox(streetInputEl);

        const prefix = prefixFromStreetName(streetInputEl.name);
        if (!prefix) return;

        // Fast path: the /suggest response already had components attached.
        let resolved = null;
        if (picked && (picked.street1 || picked.city || picked.state || picked.zip)) {
          resolved = {
            street1: picked.street1,
            street2: picked.street2,
            city:    picked.city,
            state:   picked.state,
            zip:     picked.zip,
          };
        }

        // Fallback (Google flow): /suggest only carried label+placeId – fetch details.
        if (!resolved && placeId) resolved = await resolvePlaceId(placeId);
        if (!resolved) return;

        applyResolvedAddress(addrModal.overlay, prefix, resolved, streetInputEl);
      });
    });
  };

  // Per-input debounce timer
  const timers = new WeakMap();

  const wireAutocomplete = (streetInputEl) => {
    if (!streetInputEl || streetInputEl.dataset.addrAutocompleteWired === "1") return;
    streetInputEl.dataset.addrAutocompleteWired = "1";
    streetInputEl.setAttribute("autocomplete", "off");

    streetInputEl.addEventListener("input", () => {
      const q = (streetInputEl.value || "").trim();
      if (q.length < 3) {
        hideSuggestBox(streetInputEl);
        return;
      }

      if (timers.has(streetInputEl)) clearTimeout(timers.get(streetInputEl));

      timers.set(streetInputEl, setTimeout(async () => {
        renderSuggestions(streetInputEl, await fetchSuggestions(q));
      }, 250));
    });

    streetInputEl.addEventListener("blur", () => {
      // Small delay so an in-flight mousedown selection wins.
      setTimeout(() => hideSuggestBox(streetInputEl), 150);
    });
  };

  // Wire street inputs that exist on initial render.
  addrModal.overlay
      .querySelectorAll('input[type="text"][name$=".street1"]')
      .forEach(wireAutocomplete);

  // Wire dynamically-added street inputs as the user focuses them.
  addrModal.overlay.addEventListener("focusin", (e) => {
    if (isStreetInput(e.target)) wireAutocomplete(e.target);
  });
})();