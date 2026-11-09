<?php
/**
 * GrizmMC Network - Launcher Giriş Köprüsü (Handoff)
 * Oturum açan kullanıcının bilgilerini yerel çalışan GrizmMC Launcher sunucusuna aktarır.
 */

function render_launcher_handoff($username, $email, $fullName, $token = null) {
    if (empty($token)) {
        $token = bin2hex(random_bytes(16));
    }
    $port = isset($_SESSION['launcher_port']) ? (int)$_SESSION['launcher_port'] : 58421;
    if ($port < 1024 || $port > 65535) {
        $port = 58421;
    }
    unset($_SESSION['auth_source']);
    unset($_SESSION['launcher_port']);
?>
<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>GrizmMC - Launcher Girişi Başarılı</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800;900&family=JetBrains+Mono:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      background: #08080a;
      color: #ededed;
      font-family: 'Outfit', -apple-system, BlinkMacSystemFont, sans-serif;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      position: relative;
      overflow-x: hidden;
    }
    body::before {
      content: '';
      position: absolute;
      width: 600px;
      height: 600px;
      background: radial-gradient(circle, rgba(255,255,255,0.04) 0%, rgba(0,0,0,0) 70%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      pointer-events: none;
    }
    .card {
      position: relative;
      background: #0e0e13;
      border: 1px solid #24242e;
      box-shadow: 0 30px 80px rgba(0,0,0,0.8), 0 0 40px rgba(255,255,255,0.03);
      border-radius: 20px;
      max-width: 520px;
      width: 100%;
      padding: 48px 36px;
      text-align: center;
      z-index: 1;
    }
    .avatar-wrapper {
      position: relative;
      display: inline-block;
      margin-bottom: 24px;
    }
    .avatar-box {
      width: 110px;
      height: 110px;
      border-radius: 18px;
      background: #14141a;
      border: 2px solid #33333e;
      padding: 10px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.6);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .avatar-box img {
      width: 100%;
      height: 100%;
      image-rendering: pixelated;
      border-radius: 10px;
    }
    .badge-check {
      position: absolute;
      bottom: -6px;
      right: -6px;
      background: #00ff88;
      color: #000000;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 15px;
      font-weight: bold;
      box-shadow: 0 0 20px rgba(0, 255, 136, 0.6);
    }
    .title {
      font-size: 1.8rem;
      font-weight: 800;
      color: #ffffff;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 8px;
    }
    .subtitle {
      font-size: 1.05rem;
      color: #9999a5;
      margin-bottom: 24px;
    }
    .username-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: #15151c;
      border: 1px solid #2b2b37;
      padding: 8px 18px;
      border-radius: 30px;
      font-family: 'JetBrains Mono', monospace;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 24px;
    }
    .status-panel {
      background: #121218;
      border: 1px solid #22222c;
      border-radius: 12px;
      padding: 16px 20px;
      margin-bottom: 28px;
      display: flex;
      align-items: center;
      gap: 14px;
      text-align: left;
    }
    .spinner {
      width: 22px;
      height: 22px;
      border: 3px solid rgba(255,255,255,0.15);
      border-top-color: #00ff88;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      flex-shrink: 0;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .status-text {
      font-size: 0.92rem;
      color: #bbbbcc;
      line-height: 1.4;
    }
    .btn-return {
      display: inline-block;
      width: 100%;
      background: #ffffff;
      color: #08080a;
      text-decoration: none;
      font-weight: 800;
      font-size: 0.95rem;
      padding: 14px 24px;
      border-radius: 10px;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
    }
    .btn-return:hover {
      background: #e6e6e6;
      transform: translateY(-1px);
    }
    .hint {
      margin-top: 18px;
      font-size: 0.8rem;
      color: #666675;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="avatar-wrapper">
      <div class="avatar-box">
        <img src="https://mc-heads.net/avatar/<?= urlencode($username) ?>/90" alt="<?= htmlspecialchars($username) ?>" onerror="this.src='https://mc-heads.net/avatar/Steve/90';">
      </div>
      <div class="badge-check"><i class="fa-solid fa-check"></i></div>
    </div>

    <h1 class="title">Giriş Başarılı!</h1>
    <p class="subtitle">GrizmID hesabınız doğrulandı.</p>

    <div class="username-pill">
      <i class="fa-solid fa-gamepad" style="color:#00ff88;"></i>
      <span><?= htmlspecialchars($username) ?></span>
    </div>

    <div class="status-panel">
      <div class="spinner" id="status-spinner"></div>
      <div class="status-text" id="status-msg">
        <strong>Launcher'a Bağlanılıyor...</strong><br>
        Oturum bilgileriniz yerel GrizmMC Launcher'a aktarılıyor.
      </div>
    </div>

    <button type="button" class="btn-return" id="btn-action" onclick="retryHandoff()">
      <i class="fa-solid fa-arrows-rotate"></i> Bilgileri Tekrar İlet
    </button>

    <p class="hint">
      Bilgiler aktarıldıktan sonra bu sekmeyi kapatıp Launcher üzerinden oyuna başlayabilirsiniz.
    </p>
  </div>

  <script>
    const port = <?= (int)$port ?>;
    const authData = {
      username: <?= json_encode($username) ?>,
      email: <?= json_encode($email) ?>,
      full_name: <?= json_encode($fullName) ?>,
      token: <?= json_encode($token) ?>
    };

    let transferred = false;

    function markSuccess() {
      if (transferred) return;
      transferred = true;
      const spinner = document.getElementById('status-spinner');
      const msg = document.getElementById('status-msg');
      const btn = document.getElementById('btn-action');

      if (spinner) {
        spinner.className = 'fa-solid fa-circle-check';
        spinner.style.animation = 'none';
        spinner.style.color = '#00ff88';
        spinner.style.fontSize = '24px';
        spinner.style.width = 'auto';
        spinner.style.height = 'auto';
        spinner.style.border = 'none';
      }
      if (msg) {
        msg.innerHTML = '<strong style="color:#ffffff;">Launcher Hazır!</strong><br><span style="color:#00ff88;">Bilgiler başarıyla Launcher\'a aktarıldı.</span>';
      }
      if (btn) {
        btn.innerHTML = '<i class="fa-solid fa-xmark"></i> Sekmeyi Kapat';
        btn.onclick = () => { window.close(); };
      }
    }

    function retryHandoff() {
      const q = new URLSearchParams(authData).toString();
      const url = 'http://127.0.0.1:' + port + '/callback?' + q;

      // GET via fetch
      fetch(url, { method: 'GET', mode: 'no-cors' })
        .then(() => markSuccess())
        .catch(() => {});

      // Fallback 1: Image Ping
      const img = new Image();
      img.onload = () => markSuccess();
      img.onerror = () => markSuccess();
      img.src = url + '&_t=' + Date.now();

      // Fallback 2: POST JSON
      fetch('http://127.0.0.1:' + port + '/callback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(authData)
      }).then(() => markSuccess()).catch(() => {});
    }

    retryHandoff();
    setTimeout(retryHandoff, 600);
    setTimeout(markSuccess, 1500);
  </script>
</body>
</html>
<?php
}
