<footer class="site-footer">
  <div class="container">
    <div class="footer-top">
      <!-- Marka Bilgisi -->
      <div class="footer-brand">
        <div class="brand-logo">
          <div class="logo-symbol">
            <i class="fa-solid fa-cube"></i>
          </div>
          <div class="brand-text">
            <div class="brand-title">GRIZM<span>MC</span></div>
            <div class="brand-subtitle">NETWORK</div>
          </div>
        </div>
        <p>
          <?php echo htmlspecialchars(SERVER_TAGLINE); ?>. Adil rekabet, kesintisiz altyapı ve samimi topluluk ortamı.
        </p>
        <div class="footer-socials">
          <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="social-btn" aria-label="Discord"><i class="fa-brands fa-discord"></i></a>
          <a href="<?php echo YOUTUBE_URL; ?>" target="_blank" class="social-btn" aria-label="YouTube"><i class="fa-brands fa-youtube"></i></a>
          <a href="<?php echo TIKTOK_URL; ?>" target="_blank" class="social-btn" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
          <a href="<?php echo INSTAGRAM_URL; ?>" target="_blank" class="social-btn" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
        </div>
      </div>

      <!-- Hızlı Bağlantılar -->
      <div class="footer-col">
        <h4>Bağlantılar</h4>
        <ul class="footer-links">
          <li><a href="index.php">Ana Sayfa</a></li>
          <li><a href="launcher.php">Launcher İndir </a></li>
          <li><a href="index.php#modes">Oyun Modları</a></li>
          <li><a href="store.php">VIP Mağazası</a></li>
          <li><a href="rules.php">Sunucu Kuralları</a></li>
          <li><a href="staff.php">Yetkili Kadrosu</a></li>
          <li><a href="apply.php">Kadro Başvurusu</a></li>
        </ul>
      </div>

      <!-- Sunucu Bilgisi -->
      <div class="footer-col">
        <h4>Sunucu</h4>
        <ul class="footer-links">
          <li><a href="#" data-copy-ip="<?php echo SERVER_IP; ?>">Java: <?php echo SERVER_IP; ?></a></li>
          <li><a href="#" data-copy-ip="<?php echo BEDROCK_IP; ?>">Bedrock: <?php echo BEDROCK_IP; ?></a></li>
          <li><a href="#">Sürüm: <?php echo SERVER_VERSION; ?></a></li>
          <li><a href="<?php echo DISCORD_URL; ?>" target="_blank">Discord Topluluğu</a></li>
        </ul>
      </div>

      <!-- Güvenlik & Destek -->
      <div class="footer-col">
        <h4>Destek</h4>
        <ul class="footer-links">
          <li><a href="support.php">Destek Talebi Aç</a></li>
          <li><a href="mailto:<?php echo SUPPORT_EMAIL; ?>"><?php echo SUPPORT_EMAIL; ?></a></li>
          <li><a href="rules.php">Kullanıcı Sözleşmesi</a></li>
          <li><a href="rules.php">Gizlilik & İade Politikası</a></li>
        </ul>
      </div>
    </div>

    <!-- Alt Telif & Yasal Bildiri -->
    <div class="footer-bottom">
      <div>
        &copy; <?php echo date('Y'); ?> <strong><?php echo SERVER_NAME; ?></strong>. Tüm hakları saklıdır.
      </div>
      <div class="footer-disclaimer">
        GrizmMC Network'ün Mojang AB ve Microsoft ile hiçbir alakası, sponsorluğu veya resmi bağı yoktur.
      </div>
    </div>
  </div>
</footer>

<!-- Ana JavaScript Dosyası -->
<script src="assets/js/main.js"></script>
</body>
</html>
