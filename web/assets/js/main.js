/**
 * GrizmMC Network - Ana Etkileşim, VIP Mağaza Cüzdanı ve Dinamik Özellikler
 */

document.addEventListener('DOMContentLoaded', () => {
  initParticleBackground();
  initCopyIpHandlers();
  initServerStatusPolling();
  initMobileMenu();
  initStoreModal();
  initCouponRedeemer();
});

/* ==========================================================================
   1. CLIPBOARD IP KOPYALAMA & SESLİ/TOAST BİLDİRİM
   ========================================================================== */
function initCopyIpHandlers() {
  const copyElements = document.querySelectorAll('[data-copy-ip]');

  copyElements.forEach(element => {
    element.addEventListener('click', (e) => {
      e.preventDefault();
      const ip = element.getAttribute('data-copy-ip') || 'play-mc.grizm.tech';

      navigator.clipboard.writeText(ip).then(() => {
        playCopySound();
        showToast(`Sunucu IP Kopyalandı: ${ip}`);

        const btnText = element.querySelector('.copy-label') || element;
        const originalText = btnText.innerHTML;

        btnText.innerHTML = '<i class="fa-solid fa-check"></i> KOPYALANDI!';
        element.classList.add('copied');

        setTimeout(() => {
          btnText.innerHTML = originalText;
          element.classList.remove('copied');
        }, 2200);
      }).catch(() => {
        showToast(`IP: ${ip}`);
      });
    });
  });
}

function playCopySound() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(800, audioCtx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(1200, audioCtx.currentTime + 0.08);

    gain.gain.setValueAtTime(0.08, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.08);

    osc.connect(gain);
    gain.connect(audioCtx.destination);

    osc.start();
    osc.stop(audioCtx.currentTime + 0.09);
  } catch (e) {
    // Ses API desteklenmiyorsa sessizce devam et
  }
}

function showToast(message) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3800);
}

function updateWalletBalanceElements(newCredits) {
  const formatted = Number(newCredits || 0).toFixed(2);
  document.querySelectorAll('[data-wallet-balance]').forEach(el => {
    el.textContent = formatted;
  });
  const modal = document.getElementById('checkout-modal');
  if (modal) {
    modal.setAttribute('data-user-credits', formatted);
  }
}

/* ==========================================================================
   2. ANLIK SUNUCU DURUMU (PHP API İLE GÜNCELLEME)
   ========================================================================== */
function initServerStatusPolling() {
  const countElements = document.querySelectorAll('.server-player-count');
  if (countElements.length === 0) return;

  function fetchStatus() {
    fetch('api/status.php')
      .then(res => res.json())
      .then(data => {
        if (data && data.players) {
          const count = data.players.online;
          countElements.forEach(el => {
            el.innerText = count;
          });
        }
      })
      .catch(() => {});
  }

  fetchStatus();
  setInterval(fetchStatus, 45000);
}

/* ==========================================================================
   3. ARKA PLAN GEOMETRİK PARÇACIK ANİMASYONU
   ========================================================================== */
function initParticleBackground() {
  const canvas = document.getElementById('bg-canvas');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  let width = canvas.width = window.innerWidth;
  let height = canvas.height = window.innerHeight;

  window.addEventListener('resize', () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  });

  const particles = [];
  const particleCount = Math.min(Math.floor(width / 22), 65);

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      size: Math.random() * 1.8 + 0.6,
      vx: (Math.random() - 0.5) * 0.35,
      vy: (Math.random() - 0.5) * 0.35,
      alpha: Math.random() * 0.5 + 0.2
    });
  }

  function draw() {
    ctx.clearRect(0, 0, width, height);

    for (let i = 0; i < particles.length; i++) {
      const p = particles[i];
      p.x += p.vx;
      p.y += p.vy;

      if (p.x < 0) p.x = width;
      if (p.x > width) p.x = 0;
      if (p.y < 0) p.y = height;
      if (p.y > height) p.y = 0;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(255, 255, 255, ${p.alpha})`;
      ctx.fill();

      for (let j = i + 1; j < particles.length; j++) {
        const p2 = particles[j];
        const dist = Math.hypot(p.x - p2.x, p.y - p2.y);
        if (dist < 110) {
          ctx.beginPath();
          ctx.moveTo(p.x, p.y);
          ctx.lineTo(p2.x, p2.y);
          ctx.strokeStyle = `rgba(255, 255, 255, ${0.1 * (1 - dist / 110)})`;
          ctx.lineWidth = 0.5;
          ctx.stroke();
        }
      }
    }

    requestAnimationFrame(draw);
  }

  draw();
}

/* ==========================================================================
   4. MOBİL MENÜ TETİKLEYİCİSİ
   ========================================================================== */
function initMobileMenu() {
  const openBtn = document.getElementById('menuDrawerOpenBtn');
  const closeBtn = document.getElementById('menuDrawerCloseBtn');
  const drawer = document.getElementById('siteDrawer');
  const overlay = document.getElementById('siteDrawerOverlay');

  function openDrawer() {
    if (drawer && overlay) {
      drawer.classList.add('active');
      overlay.classList.add('active');
      document.body.style.overflow = 'hidden';
    }
  }

  function closeDrawer() {
    if (drawer && overlay) {
      drawer.classList.remove('active');
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    }
  }

  if (openBtn) openBtn.addEventListener('click', openDrawer);
  if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
  if (overlay) overlay.addEventListener('click', closeDrawer);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && drawer && drawer.classList.contains('active')) {
      closeDrawer();
    }
  });

  const drawerLinks = document.querySelectorAll('.drawer-link');
  drawerLinks.forEach(link => {
    link.addEventListener('click', () => {
      closeDrawer();
    });
  });
}

/* ==========================================================================
   5. MAĞAZA CÜZDAN SATIN ALIM MODALI (GRIZMPERMS API ENTEGRELİ)
   ========================================================================== */
function initStoreModal() {
  const modal = document.getElementById('checkout-modal');
  const closeBtn = modal ? modal.querySelector('.modal-close') : null;
  const buyBtns = document.querySelectorAll('[data-buy-vip]');

  if (!modal) return;

  let selectedPriceNum = 0;

  function refreshCheckoutMath() {
    const userCredits = parseFloat(modal.getAttribute('data-user-credits') || '0');
    const summaryPrice = document.getElementById('checkout-summary-price');
    const afterLabel = document.getElementById('checkout-after-label');
    const afterBal = document.getElementById('checkout-after-balance');
    const insuffBox = document.getElementById('checkout-insufficient-box');
    const submitBtn = document.getElementById('checkout-submit-btn');

    if (summaryPrice) summaryPrice.textContent = `${selectedPriceNum.toFixed(2)} ₺`;

    const diff = userCredits - selectedPriceNum;
    if (afterBal) {
      if (diff >= 0) {
        if (afterLabel) afterLabel.textContent = 'İşlem Sonrası Bakiye:';
        afterBal.textContent = `${diff.toFixed(2)} ₺`;
        afterBal.style.color = '#ffffff';
        if (insuffBox) insuffBox.style.display = 'none';
        if (submitBtn) submitBtn.disabled = false;
      } else {
        if (afterLabel) afterLabel.textContent = 'Eksik Bakiye:';
        afterBal.textContent = `${Math.abs(diff).toFixed(2)} ₺`;
        afterBal.style.color = '#ff6b6b';
        if (insuffBox) insuffBox.style.display = 'block';
        if (submitBtn) submitBtn.disabled = true;
      }
    }
  }

  buyBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const roleId = btn.getAttribute('data-role-id') || '';
      const pkgName = btn.getAttribute('data-package-name') || 'VIP Üyelik';
      const pkgPrice = btn.getAttribute('data-package-price') || '0 ₺';
      const pkgDuration = btn.getAttribute('data-package-duration') || '30 Gün';
      selectedPriceNum = parseFloat(btn.getAttribute('data-package-price-num') || '0');

      const modalTitle = document.getElementById('modal-package-title');
      const modalPrice = document.getElementById('modal-package-price');
      const modalDuration = document.getElementById('modal-package-duration');
      const roleIdInput = document.getElementById('checkout-role-id');
      const feedbackMsg = document.getElementById('checkout-feedback-msg');

      if (modalTitle) modalTitle.innerText = pkgName;
      if (modalPrice) modalPrice.innerText = pkgPrice;
      if (modalDuration) modalDuration.innerText = `/ ${pkgDuration}`;
      if (roleIdInput) roleIdInput.value = roleId;
      if (feedbackMsg) feedbackMsg.style.display = 'none';

      refreshCheckoutMath();
      modal.classList.add('active');
    });
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', () => {
      modal.classList.remove('active');
    });
  }

  modal.addEventListener('click', (e) => {
    if (e.target === modal) {
      modal.classList.remove('active');
    }
  });

  const form = document.getElementById('checkout-form');
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const usernameInput = document.getElementById('mc-username');
      const roleIdInput = document.getElementById('checkout-role-id');
      const submitBtn = document.getElementById('checkout-submit-btn');
      const feedbackMsg = document.getElementById('checkout-feedback-msg');

      const targetUsername = usernameInput ? usernameInput.value.trim() : '';
      const roleId = roleIdInput ? roleIdInput.value.trim() : '';

      if (!targetUsername) {
        showToast('Lütfen Minecraft oyuncu adını yazın!');
        return;
      }

      const originalBtnHtml = submitBtn ? submitBtn.innerHTML : '';
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <span>Sunucuya Tanımlanıyor...</span>';
      }

      try {
        const res = await fetch('api/perms.php?action=buy_vip', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            role_id: roleId,
            target_username: targetUsername
          })
        });
        const data = await res.json();

        if (res.ok && data.status === 'success') {
          playCopySound();
          updateWalletBalanceElements(data.credits);
          refreshCheckoutMath();

          if (feedbackMsg) {
            feedbackMsg.style.display = 'block';
            feedbackMsg.style.background = 'rgba(255, 255, 255, 0.08)';
            feedbackMsg.style.border = '1px solid rgba(255, 255, 255, 0.35)';
            feedbackMsg.style.color = '#ffffff';
            feedbackMsg.innerHTML = `<i class="fa-solid fa-circle-check"></i> <strong>${data.message}</strong>`;
          }

          showToast(data.message);
          setTimeout(() => {
            window.location.reload();
          }, 1600);
        } else {
          if (feedbackMsg) {
            feedbackMsg.style.display = 'block';
            feedbackMsg.style.background = 'rgba(255, 85, 85, 0.1)';
            feedbackMsg.style.border = '1px solid rgba(255, 85, 85, 0.4)';
            feedbackMsg.style.color = '#fca5a5';
            feedbackMsg.innerHTML = `<i class="fa-solid fa-circle-xmark"></i> ${data.message || 'İşlem sırasında bir hata oluştu.'}`;
          }
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
          }
        }
      } catch (err) {
        showToast('Sunucu ile bağlantı kurulamadı.');
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.innerHTML = originalBtnHtml;
        }
      }
    });
  }
}

/* ==========================================================================
   6. KUPON / HEDİYE KODU KULLANIMI (MAĞAZA & PROFİL)
   ========================================================================== */
function initCouponRedeemer() {
  const form = document.getElementById('store-coupon-form');
  const input = document.getElementById('store-coupon-input');
  if (!form || !input) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const code = input.value.trim();
    if (!code) {
      showToast('Lütfen bir kupon veya hediye kodu girin.');
      return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    const origHtml = submitBtn ? submitBtn.innerHTML : '';
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
    }

    try {
      const res = await fetch('api/perms.php?action=redeem_coupon', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
      });
      const data = await res.json();

      if (res.ok && data.status === 'success') {
        playCopySound();
        input.value = '';
        updateWalletBalanceElements(data.credits);
        showToast(data.message);
        if (data.type === 'vip') {
          setTimeout(() => window.location.reload(), 1300);
        }
      } else {
        showToast(data.message || 'Kupon kodu kullanılamadı.');
      }
    } catch (err) {
      showToast('Bağlantı hatası oluştu.');
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = origHtml;
      }
    }
  });
}
