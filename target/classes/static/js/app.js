(function () {
  "use strict";

  // Hamburger menu mobile + backdrop tutup otomatis
  var burger = document.querySelector(".hamburger");
  var navLinks = document.querySelector(".nav-links");
  var navBackdrop = document.getElementById("navBackdrop");

  function openNavMenu() {
    if (!navLinks) return;
    navLinks.classList.add("open");
    if (navBackdrop) navBackdrop.classList.add("show");
    syncBurgerSpans(true);
  }
  function closeNavMenu() {
    if (!navLinks) return;
    navLinks.classList.remove("open");
    if (navBackdrop) navBackdrop.classList.remove("show");
    syncBurgerSpans(false);
  }
  function syncBurgerSpans(open) {
    if (!burger) return;
    burger.querySelectorAll("span").forEach(function (s, i) {
      s.style.transform = open
        ? (i === 0 ? "translateY(7px) rotate(45deg)" : i === 2 ? "translateY(-7px) rotate(-45deg)" : "scaleX(0)")
        : "";
    });
  }
  if (burger && navLinks) {
    burger.addEventListener("click", function () {
      if (navLinks.classList.contains("open")) closeNavMenu();
      else openNavMenu();
    });
  }
  // Klik backdrop / area luar navbar -> tutup menu geser
  if (navBackdrop) {
    navBackdrop.addEventListener("click", closeNavMenu);
  }
  document.addEventListener("click", function (e) {
    if (!navLinks || !navLinks.classList.contains("open")) return;
    if (navLinks.contains(e.target) || (burger && burger.contains(e.target))) return;
    closeNavMenu();
  });
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") closeNavMenu();
  });

  var mobileAccount = document.getElementById("mobileAccount");
  var mobileAccountBtn = document.getElementById("mobileAccountBtn");
  var notifBtn = document.getElementById("notifBtn");
  var notifPanel = document.getElementById("notifPanel");

  function closeMobileAccount() {
    if (!mobileAccount || !mobileAccountBtn) return;
    mobileAccount.classList.remove("open");
    mobileAccountBtn.setAttribute("aria-expanded", "false");
  }

  function closeNotifPanel() {
    if (notifPanel) notifPanel.classList.remove("open");
  }

  if (mobileAccount && mobileAccountBtn) {
    mobileAccountBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      var isOpen = mobileAccount.classList.toggle("open");
      mobileAccountBtn.setAttribute("aria-expanded", String(isOpen));
    });

    document.addEventListener("click", function (e) {
      if (!mobileAccount.contains(e.target)) closeMobileAccount();
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") {
        closeMobileAccount();
        closeNotifPanel();
      }
    });

    // Tutup dropdown setelah menavigasi (mis. klik Keranjang / Profil)
    mobileAccount.querySelectorAll("a").forEach(function (a) {
      a.addEventListener("click", closeMobileAccount);
    });
  }

  // Klik logo/judul: jangan reload bila sudah berada di halaman yang sama
  var brandLink = document.querySelector("a.brand");
  if (brandLink) {
    brandLink.addEventListener("click", function (e) {
      try {
        var target = new URL(brandLink.href, window.location.href);
        if (target.pathname === window.location.pathname && target.search === window.location.search) {
          e.preventDefault();
          window.scrollTo(0, 0);
        }
      } catch (err) {}
    });
  }

  // Tutup navbar setelah klik link (mobile)
  document.querySelectorAll(".nav-links a").forEach(function (a) {
    a.addEventListener("click", closeNavMenu);
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
    } else {
      if (badge) badge.remove();
    }
  }

  function addNotifRow(d) {
    var list = document.getElementById("notifList");
    var mobileList = document.getElementById("mobileNotifList");
    [list, mobileList].forEach(function (target) {
      if (!target) return;
      var empty = target.querySelector(".notif-empty");
      if (empty) empty.remove();
      var a = document.createElement("a");
      a.className = "notif-item";
      a.href = d.link || "#";
      a.innerHTML = '<span class="notif-dot"></span><span class="notif-text"><span>' +
        (d.message || "").replace(/</g, "&lt;") +
        '</span><small>' + (d.time || "") + "</small></span>";
      target.insertBefore(a, target.firstChild);
      while (target.children.length > 12) target.removeChild(target.lastChild);
    });
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

  // ---------- Animasi angka statistik beranda (count-up) ----------
  var statCounters = document.querySelectorAll("[data-count]");
  if (statCounters.length) {
    var suffixOf = function (el) { return el.getAttribute("data-suffix") || ""; };
    statCounters.forEach(function (el) { el.textContent = "0" + suffixOf(el); });

    var runCounter = function (el) {
      if (el.getAttribute("data-animated")) return;
      el.setAttribute("data-animated", "1");
      var target = parseInt(el.getAttribute("data-count"), 10) || 0;
      var suffix = suffixOf(el);
      var duration = 1500;
      var startTime = null;
      var tick = function (ts) {
        if (startTime === null) startTime = ts;
        var progress = Math.min((ts - startTime) / duration, 1);
        var eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(target * eased) + suffix;
        if (progress < 1) requestAnimationFrame(tick);
        else el.textContent = target + suffix;
      };
      requestAnimationFrame(tick);
    };

    if ("IntersectionObserver" in window) {
      var statObserver = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            runCounter(entry.target);
            statObserver.unobserve(entry.target);
          }
        });
      }, { threshold: 0.4 });
      statCounters.forEach(function (el) { statObserver.observe(el); });
    } else {
      statCounters.forEach(runCounter);
    }
  }

  // ---------- Lacak pengiriman realtime ----------
  var trackMap = document.getElementById("trackMap");
  var trackMsg = document.getElementById("trackMsg");
  var trackFill = document.getElementById("trackFill");
  var trackEta = document.getElementById("trackEta");
  var trackOrderId = document.querySelector(".track-wrap[data-order-id]");
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

  // ---------- Keranjang ngambang + tambah tanpa reload ----------
  var floatingCart = document.getElementById("floatingCart");

  function csrfToken() {
    var meta = document.querySelector('meta[name="_csrf"]');
    if (meta && meta.content) return meta.content;
    var input = document.querySelector('input[name="_csrf"]');
    return input ? input.value : "";
  }

  function csrfHeaderName() {
    var meta = document.querySelector('meta[name="_csrf_header"]');
    return meta && meta.content ? meta.content : "X-CSRF-TOKEN";
  }

  function syncNavCartBadge(count) {
    var btn = document.querySelector(".cart-icon-btn");
    if (btn) {
      var badge = btn.querySelector(".cart-count");
      if (count > 0) {
        if (!badge) {
          badge = document.createElement("span");
          badge.className = "cart-count";
          btn.appendChild(badge);
        }
        badge.textContent = count;
      } else if (badge) {
        badge.remove();
      }
    }
    var mobileCartCount = document.getElementById("mobileCartCount");
    if (mobileCartCount) {
      if (count > 0) {
        mobileCartCount.textContent = count;
        mobileCartCount.style.display = "";
      } else {
        mobileCartCount.textContent = "0";
        mobileCartCount.style.display = "none";
      }
    }
  }

  function renderFloatingCart(count, subtotal) {
    if (floatingCart) {
      var c = document.getElementById("floatingCartCount");
      var t = document.getElementById("floatingCartTotal");
      if (c) c.textContent = count;
      if (t) t.textContent = Number(subtotal || 0).toLocaleString("id-ID");
      if (count > 0) {
        floatingCart.classList.add("is-visible");
        floatingCart.classList.remove("bump");
        void floatingCart.offsetWidth;
        floatingCart.classList.add("bump");
      } else {
        floatingCart.classList.remove("is-visible");
      }
    }
    syncNavCartBadge(count);
  }

  if (floatingCart) {
    var path = window.location.pathname;
    if (path === "/cart" || path === "/checkout") {
      floatingCart.style.display = "none";
    }
  }

  document.querySelectorAll("form[data-cart-add]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      if (form.getAttribute("data-no-ajax") === "1") return;
      if (typeof window.fetch !== "function") return;
      e.preventDefault();
      var btn = form.querySelector("button[type='submit']");
      if (btn) btn.disabled = true;
      var headers = {};
      headers[csrfHeaderName()] = csrfToken();
      fetch(form.getAttribute("data-ajax-url") || "/cart/api/add", {
        method: "POST",
        body: new FormData(form),
        headers: headers,
        credentials: "same-origin"
      })
        .then(function (r) {
          if (!r.ok) throw new Error("add failed");
          return r.json();
        })
        .then(function (d) {
          renderFloatingCart(d.count, d.subtotal);
          showToast("Menu ditambahkan ke keranjang");
        })
        .catch(function () {
          form.setAttribute("data-no-ajax", "1");
          form.submit();
        })
        .finally(function () {
          if (btn) btn.disabled = false;
        });
    });
  });
})();