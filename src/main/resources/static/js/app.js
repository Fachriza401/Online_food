(function () {
  "use strict";

  // Hamburger menu mobile
  var burger = document.querySelector(".hamburger");
  var navLinks = document.querySelector(".nav-links");
  if (burger && navLinks) {
    burger.addEventListener("click", function () {
      navLinks.classList.toggle("open");
      document.querySelectorAll(".hamburger span").forEach(function (s, i) {
        var open = navLinks.classList.contains("open");
        s.style.transform = open ? (i === 0 ? "translateY(7px) rotate(45deg)" : i === 2 ? "translateY(-7px) rotate(-45deg)" : "scaleX(0)") : "";
      });
    });
  }

  // Tutup navbar setelah klik link (mobile)
  document.querySelectorAll(".nav-links a").forEach(function (a) {
    a.addEventListener("click", function () {
      if (navLinks) navLinks.classList.remove("open");
    });
  });

  // Auto-dismiss flash message
  var alertFlash = document.querySelector(".toast-auto");
  if (alertFlash) {
    setTimeout(function () {
      alertFlash.style.transition = "opacity .4s ease";
      alertFlash.style.opacity = "0";
      setTimeout(function () { alertFlash.remove(); }, 400);
    }, 4200);
  }

  // Konfirmasi tombol hapus
  document.querySelectorAll("[data-confirm]").forEach(function (el) {
    el.addEventListener("click", function (e) {
      if (!window.confirm(el.getAttribute("data-confirm"))) {
        e.preventDefault();
      }
    });
  });

  // Sinkronkan subtotal di halaman menu/detail saat memilih qty
  var qtyInputs = document.querySelectorAll(".food-qty-input");
  qtyInputs.forEach(function (input) {
    input.addEventListener("input", function () {
      var price = parseFloat(input.getAttribute("data-price")) || 0;
      var total = document.querySelector(input.getAttribute("data-target"));
      if (total) {
        var qty = parseInt(input.value) || 1;
        total.textContent = "Rp " + (price * Math.max(1, qty)).toLocaleString("id-ID");
      }
    });
  });

  // Autofocus pencarian menu
  var searchInput = document.querySelector("input[name='q']");
  if (searchInput && searchInput.closest(".toolbar")) {
    // tetap biarkan default
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

  // ---------- Preview foto profil ----------
  var avatarFile = document.getElementById("photo");
  var avatarBox = document.getElementById("avatarBox");
  if (avatarFile && avatarBox) {
    avatarFile.addEventListener("change", function () {
      var f = this.files[0];
      if (!f) return;
      if (!f.type.match(/image\/(jpeg|png|webp)/)) {
        alert("Format foto harus JPG, PNG, atau WEBP.");
        this.value = "";
        return;
      }
      var rd = new FileReader();
      rd.onload = function (e) {
        var img = document.getElementById("avatarPreview");
        if (!img) {
          img = document.createElement("img");
          img.id = "avatarPreview";
          img.className = "avatar-img";
          img.alt = "Foto profil";
          img.style.width = "100%";
          img.style.height = "100%";
          avatarBox.appendChild(img);
        }
        img.src = e.target.result;
        var fb = document.getElementById("avatarFallback");
        if (fb) fb.style.display = "none";
      };
      rd.readAsDataURL(f);
    });
  }

  // ---------- Lacak pengiriman realtime ----------
  var trackMap = document.getElementById("trackMap");
  var trackMsg = document.getElementById("trackMsg");
  var trackFill = document.getElementById("trackFill");
  var trackEta = document.getElementById("trackEta");
  var trackOrderId = document.querySelector(".track-wrap [data-order-id]");
  if (trackMap && trackOrderId) {
    var orderId = trackOrderId.getAttribute("data-order-id");
    var lastKey = "";
    function updateTracking() {
      fetch("/api/orders/" + orderId + "/tracking")
        .then(function (r) { return r.json(); })
        .then(function (d) {
          if (trackMsg) trackMsg.textContent = d.message || "";
          if (d.lat && d.lng) {
            var key = d.lat + "," + d.lng;
            if (key !== lastKey) {
              lastKey = key;
              trackMap.src = "https://www.google.com/maps?q=" + d.lat + "," + d.lng + "&z=14&output=embed";
            }
          }
          if (trackFill) trackFill.style.width = Math.round((d.progress || 0) * 100) + "%";
          if (trackEta) {
            if (d.done) trackEta.textContent = "Tiba di lokasi";
            else if (d.etaMinutes >= 0) trackEta.textContent = "~ " + d.etaMinutes + " menit lagi";
            else trackEta.textContent = "Menunggu kurir berangkat";
          }
        })
        .catch(function () {});
    }
    updateTracking();
    setInterval(updateTracking, 4000);
  }
})();