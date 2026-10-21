(function () {
  "use strict";

  /* ============================================================
     SIDEBAR MOBILE (tombol 3 garis / hamburger)
     - Tombol hamburger bersifat toggle (buka <-> tutup)
     - Overlay/backdrop gelap muncul saat sidebar terbuka
     - Klik area di luar sidebar -> sidebar tertutup otomatis
     - Tekan ESC -> sidebar tertutup
     - Gulir halaman dikunci selama sidebar / modal terbuka
     ============================================================ */
  var sidebar = document.getElementById("adminSidebar");
  var burger = document.getElementById("adminBurger");
  var sidebarClose = document.getElementById("adminSidebarClose");

  // Pastikan overlay selalu ada (fallback bila markup tidak ikut ter-render)
  var overlay = document.getElementById("adminOverlay");
  if (!overlay) {
    overlay = document.createElement("div");
    overlay.id = "adminOverlay";
    overlay.className = "overlay-admin";
    overlay.setAttribute("aria-hidden", "true");
    document.body.appendChild(overlay);
  }

  var MOBILE_QUERY = "(max-width: 860px)";

  function isMobile() {
    return window.matchMedia && window.matchMedia(MOBILE_QUERY).matches;
  }

  function isSidebarOpen() {
    return !!(sidebar && sidebar.classList.contains("open"));
  }

  function openSidebar() {
    if (!sidebar) return;
    sidebar.classList.add("open");
    if (overlay) overlay.classList.add("open");
    if (burger) {
      burger.setAttribute("aria-expanded", "true");
      burger.setAttribute("aria-label", "Tutup menu");
    }
    if (isMobile()) {
      document.body.classList.add("sidebar-open");
    }
  }

  function closeSidebar() {
    if (sidebar) sidebar.classList.remove("open");
    if (overlay) overlay.classList.remove("open");
    if (burger) {
      burger.setAttribute("aria-expanded", "false");
      burger.setAttribute("aria-label", "Menu");
    }
    document.body.classList.remove("sidebar-open");
  }

  function toggleSidebar() {
    if (isSidebarOpen()) {
      closeSidebar();
    } else {
      openSidebar();
    }
  }

  if (burger) {
    burger.setAttribute("aria-expanded", "false");
    burger.setAttribute("aria-controls", "adminSidebar");
    burger.addEventListener("click", function (e) {
      e.stopPropagation();
      toggleSidebar();
    });
  }
  if (sidebarClose) {
    sidebarClose.addEventListener("click", function (e) {
      e.stopPropagation();
      closeSidebar();
    });
  }
  if (overlay) {
    overlay.addEventListener("click", function (e) {
      e.preventDefault();
      closeSidebar();
    });
  }

  // Klik di luar kotak sidebar -> tutup otomatis (fase capture agar selalu wins)
  document.addEventListener("click", function (e) {
    if (!isSidebarOpen()) return;
    var target = e.target;
    if (sidebar && sidebar.contains(target)) return;
    if (burger && burger.contains(target)) return;
    closeSidebar();
  }, true);

  // Tekan ESC -> tutup
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && isSidebarOpen()) {
      closeSidebar();
    }
  });

  // Klik salah satu menu -> sidebar tertutup (navigasi ke halaman tujuan)
  document.querySelectorAll(".sidebar-nav a").forEach(function (a) {
    a.addEventListener("click", closeSidebar);
  });

  // Kembali ke lebar desktop -> pastikan sidebar dalam kondisi tertutup
  window.addEventListener("resize", function () {
    if (!isMobile()) closeSidebar();
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

  // ---------- Pratinjau & Cetak Laporan (A4 / dashboard modern) ----------
  var pp = document.getElementById("printPreview");
  var openBtn = document.querySelector("[data-print-preview]");
  var doPrintBtn = document.getElementById("btnDoPrint");
  var doPdfBtn = document.getElementById("btnDoPrintPdf");

  var DAYS = ["Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"];
  var MONTHS = ["Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"];

  function two(n) { return (n < 10 ? "0" : "") + n; }

  function stampNow() {
    var d = new Date();
    return DAYS[d.getDay()] + ", " + two(d.getDate()) + " " + MONTHS[d.getMonth()] + " " +
      d.getFullYear() + " pukul " + two(d.getHours()) + ":" + two(d.getMinutes()) + ":" + two(d.getSeconds());
  }

  function stampShort() {
    var d = new Date();
    return two(d.getDate()) + " " + MONTHS[d.getMonth()] + " " + d.getFullYear();
  }

  // Isi timestamp real-time ke dokumen laporan
  function fillPrintDate() {
    var full = document.getElementById("printDateRealtime");
    var sign = document.getElementById("printDateSign");
    var note = document.getElementById("printDateNote");
    if (full) full.textContent = stampNow();
    if (sign) sign.textContent = stampShort();
    if (note) note.textContent = stampNow();
  }

  function openPrint() {
    if (!pp) return;
    fillPrintDate();
    pp.classList.add("open");
    pp.setAttribute("aria-hidden", "false");
    document.body.classList.add("pp-open");
  }

  function closePrint() {
    if (!pp) return;
    pp.classList.remove("open");
    pp.setAttribute("aria-hidden", "true");
    document.body.classList.remove("pp-open");
  }

  function triggerPrint() {
    if (!pp) {
      // Tidak ada modal (mis. halaman cetak mandiri) -> langsung cetak
      fillPrintDate();
      window.setTimeout(function () { window.print(); }, 120);
      return;
    }
    if (!pp.classList.contains("open")) openPrint();
    fillPrintDate();
    // Beri waktu render sebelum dialog cetak dibuka (optimal untuk webview Android)
    window.setTimeout(function () { window.print(); }, 120);
  }

  if (openBtn) openBtn.addEventListener("click", openPrint);
  if (doPrintBtn) doPrintBtn.addEventListener("click", triggerPrint);
  if (doPdfBtn) doPdfBtn.addEventListener("click", triggerPrint);

  document.querySelectorAll("[data-print-close]").forEach(function (el) {
    el.addEventListener("click", closePrint);
  });

  if (pp) {
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && pp.classList.contains("open")) closePrint();
    });
  }

  // Timestamp tetap akurat bila tab dibuka lama (diperbarui saat print)
  window.addEventListener("beforeprint", fillPrintDate);

  // Helper global dipakai halaman cetak mandiri (admin/report-print.html)
  window.AdminPrint = {
    fill: fillPrintDate,
    open: openPrint,
    close: closePrint,
    print: triggerPrint
  };
})();

// Auto-dismiss flash message admin
var alertFlashAdmin = document.querySelector('.toast-auto');
if (alertFlashAdmin) {
  setTimeout(function () {
    alertFlashAdmin.style.transition = 'opacity .4s ease';
    alertFlashAdmin.style.opacity = '0';
    setTimeout(function () { alertFlashAdmin.remove(); }, 400);
  }, 4200);
}
