(function () {
  "use strict";

  var sidebar = document.getElementById("adminSidebar");
  var overlay = document.getElementById("adminOverlay");
  var burger = document.getElementById("adminBurger");

  function openSidebar() {
    if (sidebar) sidebar.classList.add("open");
    if (overlay) overlay.classList.add("open");
  }
  function closeSidebar() {
    if (sidebar) sidebar.classList.remove("open");
    if (overlay) overlay.classList.remove("open");
  }

  if (burger) burger.addEventListener("click", openSidebar);
  if (overlay) overlay.addEventListener("click", closeSidebar);

  document.querySelectorAll(".sidebar-nav a").forEach(function (a) {
    a.addEventListener("click", closeSidebar);
  });

  // Konfirmasi hapus
  document.querySelectorAll("[data-confirm]").forEach(function (el) {
    el.addEventListener("click", function (e) {
      if (!window.confirm(el.getAttribute("data-confirm"))) {
        e.preventDefault();
      }
    });
  });

  // Preview foto sebelum upload
  var fileInput = document.getElementById("imageFile");
  var preview = document.getElementById("imagePreview");
  if (fileInput && preview) {
    fileInput.addEventListener("change", function () {
      var file = this.files[0];
      if (!file) return;
      if (!file.type.match(/image\/(jpeg|png|webp)/)) {
        alert("Format foto harus JPG, PNG, atau WEBP.");
        this.value = "";
        return;
      }
      var reader = new FileReader();
      reader.onload = function (e) { preview.src = e.target.result; preview.style.display = "block"; };
      reader.readAsDataURL(file);
    });
  }

  // Filter status pesanan di dashboard (jika ada)
  var statusFilter = document.getElementById("orderStatusFilter");
  if (statusFilter) {
    statusFilter.addEventListener("change", function () {
      var value = this.value;
      var url = new URL(window.location.href);
      if (value) url.searchParams.set("status", value); else url.searchParams.delete("status");
      url.searchParams.delete("page");
      window.location.href = url.toString();
    });
  }

  // ---------- Dropdown menu profil admin (kanan atas) ----------
  var menuBtn = document.getElementById("adminMenuBtn");
  var menuDropdown = document.getElementById("adminDropdown");
  var menuWrap = document.getElementById("adminMenuWrap");
  if (menuBtn && menuDropdown) {
    menuBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      menuDropdown.classList.toggle("open");
    });
    if (menuWrap) {
      document.addEventListener("click", function (e) {
        if (!menuWrap.contains(e.target)) menuDropdown.classList.remove("open");
      });
    }
  }

  // ---------- Notifikasi realtime (SSE + fallback polling) ----------
  var notifWrap = document.getElementById("notifWrap");
  var notifBtn = document.getElementById("notifBtn");
  var notifPanel = document.getElementById("notifPanel");

  function setNotifBadge(count) {
    if (!notifWrap) return;
    var badge = notifWrap.querySelector(".notif-badge");
    if (count > 0) {
      if (!badge) {
        badge = document.createElement("span");
        badge.className = "notif-badge";
        var btn = notifWrap.querySelector(".notif-btn");
        if (btn) btn.appendChild(badge);
      }
      badge.textContent = count;
    } else if (badge) {
      badge.remove();
    }
  }

  function addNotifRow(d) {
    var list = document.getElementById("notifList");
    if (!list) return;
    var empty = list.querySelector(".notif-empty");
    if (empty) empty.remove();
    var a = document.createElement("a");
    a.className = "notif-item";
    a.href = d.link || "#";
    a.innerHTML = '<span class="notif-dot"></span><span class="notif-text"><span>' +
      (d.message || "").replace(/</g, "&lt;") +
      '</span><small>' + (d.time || "") + "</small></span>";
    list.insertBefore(a, list.firstChild);
    while (list.children.length > 12) list.removeChild(list.lastChild);
  }

  function showToast(msg) {
    var wrap = document.querySelector(".toast-container");
    if (!wrap) {
      wrap = document.createElement("div");
      wrap.className = "toast-container";
      document.body.appendChild(wrap);
    }
    var t = document.createElement("div");
    t.className = "toast info";
    t.textContent = msg;
    wrap.appendChild(t);
    setTimeout(function () {
      t.style.transition = "opacity .4s ease";
      t.style.opacity = "0";
      setTimeout(function () { t.remove(); }, 400);
    }, 4200);
  }

  function pollUnread() {
    fetch("/api/notifications/unread")
      .then(function (r) { return r.json(); })
      .then(function (d) { setNotifBadge(d.count || 0); })
      .catch(function () {});
  }

  if (notifBtn && notifPanel) {
    notifBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      notifPanel.classList.toggle("open");
    });
    document.addEventListener("click", function (e) {
      if (notifWrap && !notifWrap.contains(e.target)) notifPanel.classList.remove("open");
    });
  }
  if (notifWrap) {
    if (typeof EventSource !== "undefined") {
      var es = new EventSource("/api/notifications/stream");
      es.addEventListener("notification", function (evt) {
        try {
          var d = JSON.parse(evt.data);
          setNotifBadge(d.count);
          addNotifRow(d);
          showToast(d.message);
        } catch (e) {}
      });
      es.onerror = function () {
        es.close();
        setTimeout(function () { pollUnread(); }, 4000);
      };
    }
    pollUnread();
    setInterval(pollUnread, 30000);
  }
})();