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

  // Address modal
  const addrModal = initModal({
    openBtnId: "openUpdateAddress",
    overlayId: "updateAddressOverlay",
    closeBtnId: "closeUpdateAddress",
    cancelBtnId: "cancelUpdateAddress",
    focusInputId: null,
  });

  // Dynamic address blocks + autocomplete inside address modal
  if (addrModal) {
    const API_SUGGEST = "/api/address/suggest";
    const API_RESOLVE = "/api/address/resolve";

    const addBtn = document.getElementById("addAddressBlockBtn");
    const container = document.getElementById("newAddressesContainer");

    const isDefaultCheckbox = (el) =>
      el &&
      el.tagName === "INPUT" &&
      el.type === "checkbox" &&
      typeof el.name === "string" &&
      el.name.endsWith(".default");

    addrModal.overlay.addEventListener("change", (e) => {
      const el = e.target;
      if (!isDefaultCheckbox(el)) return;

      // If user unchecked it, don't auto-select anything else.
      if (!el.checked) return;

      // Uncheck all other ".default" checkboxes within this same modal overlay
      const allDefaultChecks = addrModal.overlay.querySelectorAll('input[type="checkbox"][name$=".default"]');
      allDefaultChecks.forEach((cb) => {
        if (cb !== el) cb.checked = false;
      });
    });

    const renderTypeOptions = (selectedValue) => {
      const normalized = (selectedValue || "SHIPPING").toString().trim().toUpperCase();
      return ADDRESS_TYPES.map((t) => {
        const selectedAttr = t === normalized ? " selected" : "";
        return `<option value="${t}"${selectedAttr}>${t}</option>`;
      }).join("");
    };

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

        <div class="update-address-footer">
            <div class="form-control">
              <input type="checkbox" name="newAddresses[${index}].default" value="true" />
              <label style="margin:0;">Make default</label>
            </div>
        </div>
      `;

      const removeBtn = wrapper.querySelector('[data-remove="1"]');
      if (removeBtn) {
        removeBtn.addEventListener("click", () => {
          wrapper.remove();
        });
      }

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
        const index = nextIndex();
        container.appendChild(buildAddressBlock(index));
      });
    }

    // -----------------------------
    // Autocomplete: UI helpers
    // -----------------------------
    const isStreetInput = (el) =>
      el &&
      el.tagName === "INPUT" &&
      el.type === "text" &&
      typeof el.name === "string" &&
      el.name.endsWith(".street1");

    const prefixFromStreetName = (streetName) => {
      // "addresses[0].street" -> "addresses[0]."
      // "newAddresses[2].street" -> "newAddresses[2]."
      if (!streetName || !streetName.endsWith(".street1")) return null;
      return streetName.slice(0, -".street1".length) + ".";
    };

    const setByName = (root, name, value) => {
      const sel = `[name="${CSS.escape(name)}"]`;
      const el = root.querySelector(sel);
      if (el) el.value = value ?? "";
    };

    const ensureSuggestBox = (streetInputEl) => {
      const parent = streetInputEl.closest(".form-control") || streetInputEl.parentElement;
      if (!parent) return null;

      parent.style.position = parent.style.position || "relative";

      let box = parent.querySelector(".addr-suggest-box");
      if (box) return box;

      box = document.createElement("div");
      box.className = "addr-suggest-box";
      box.style.position = "absolute";
      box.style.left = "0";
      box.style.right = "0";
      box.style.top = "100%";
      box.style.marginTop = "6px";
      box.style.background = "#fff";
      box.style.border = "1px solid rgba(0,0,0,0.15)";
      box.style.borderRadius = "8px";
      box.style.boxShadow = "0 10px 30px rgba(0,0,0,0.12)";
      box.style.zIndex = "9999";
      box.style.display = "none";
      box.style.maxHeight = "220px";
      box.style.overflowY = "auto";

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

    const escapeHtml = (s) =>
      String(s ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");

    const fetchSuggestions = async (q) => {
      const res = await fetch(`${API_SUGGEST}?q=${encodeURIComponent(q)}`, {
        headers: { Accept: "application/json" },
      });
      if (!res.ok) return [];
      const data = await res.json();
      return Array.isArray(data) ? data : [];
    };

    const resolvePlaceId = async (placeId) => {
      const res = await fetch(`${API_RESOLVE}?placeId=${encodeURIComponent(placeId)}`, {
        headers: { Accept: "application/json" },
      });
      if (!res.ok) return null;
      return await res.json(); // AddressDTO {street, city, state, zip}
    };

    const renderSuggestions = (streetInputEl, suggestions) => {
      const box = ensureSuggestBox(streetInputEl);
      if (!box) return;

      if (!suggestions || !suggestions.length) {
        box.style.display = "none";
        box.innerHTML = "";
        return;
      }

      box.innerHTML = suggestions
        .map((s) => {
          const label = escapeHtml(s.label);
          const placeId = escapeHtml(s.placeId);
          return `<div class="addr-suggest-item" data-place-id="${placeId}" style="padding:10px 12px; cursor:pointer;">${label}</div>`;
        })
        .join("");

      box.style.display = "block";

      // Bind selection to this specific input (so we fill the correct address block)
      [...box.querySelectorAll(".addr-suggest-item")].forEach((item) => {
        item.addEventListener("mousedown", async (e) => {
          // mousedown so selection works before blur hides box
          e.preventDefault();

          const placeId = item.getAttribute("data-place-id");
          const label = (item.textContent || "").trim();

          // Immediately show selection in street box
          streetInputEl.value = label;
          hideSuggestBox(streetInputEl);

          if (!placeId) return;

          const resolved = await resolvePlaceId(placeId);
          if (!resolved) return;

          const prefix = prefixFromStreetName(streetInputEl.name);
          if (!prefix) return;

          const scope = addrModal.overlay;

          // Fill sibling fields in the SAME address block
          if (resolved.street) setByName(scope, `${prefix}street`, resolved.street);
          if (resolved.city) setByName(scope, `${prefix}city`, resolved.city);
          if (resolved.state) setByName(scope, `${prefix}state`, resolved.state);
          if (resolved.zip) setByName(scope, `${prefix}zip`, resolved.zip);
        });
      });
    };

    // Debounce per input
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

        const t = setTimeout(async () => {
          const suggestions = await fetchSuggestions(q);
          renderSuggestions(streetInputEl, suggestions);
        }, 250);

        timers.set(streetInputEl, t);
      });

      streetInputEl.addEventListener("blur", () => {
        // small delay so mousedown selection wins
        setTimeout(() => hideSuggestBox(streetInputEl), 150);
      });
    };

    // Wire existing street inputs (existing addresses) when modal exists
    const existingStreetInputs = addrModal.overlay.querySelectorAll('input[type="text"][name$=".street"]');
    existingStreetInputs.forEach(wireAutocomplete);

    // Wire dynamically added street inputs on focus
    addrModal.overlay.addEventListener("focusin", (e) => {
      const el = e.target;
      if (isStreetInput(el)) wireAutocomplete(el);
    });
  }
})();