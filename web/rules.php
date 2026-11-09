<?php
$pageTitle = "Sunucu Kuralları";
$activePage = "rules";
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Topluluk Standartları</span>
      <h1 class="section-title">SUNUCU KURALLARI</h1>
      <p class="section-desc">
        GrizmMC Network üzerinde tüm oyuncularımızın huzurlu, adil ve eğlenceli vakit geçirmesi için aşağıdaki kurallara uyulması zorunludur.
      </p>
    </div>

    <div class="rules-container">
      <?php foreach ($RULES as $catKey => $category): ?>
        <div class="rule-category-card">
          <div class="rule-category-header">
            <i class="fa-solid fa-gavel"></i>
            <h3 class="rule-category-title"><?php echo htmlspecialchars($category['title']); ?></h3>
          </div>

          <div class="rule-list">
            <?php foreach ($category['items'] as $item): ?>
              <div class="rule-item">
                <div class="rule-item-top">
                  <span class="rule-item-title"><?php echo htmlspecialchars($item['title']); ?></span>
                  <span class="rule-penalty-badge"><?php echo htmlspecialchars($item['penalty']); ?></span>
                </div>
                <p class="rule-item-desc"><?php echo htmlspecialchars($item['desc']); ?></p>
              </div>
            <?php endforeach; ?>
          </div>
        </div>
      <?php endforeach; ?>

      <!-- Kural İhlali Bildirimi Uyarısı -->
      <div class="feature-box" style="margin-top: 40px; text-align: center;">
        <h4 class="feature-box-title" style="margin-bottom: 8px;">Kural İhlali mi Gördünüz?</h4>
        <p class="feature-box-desc" style="margin-bottom: 20px;">
          Oyun içi ekran görüntüsü veya video kaydı alarak Discord destek kanalımız üzerinden yetkililere bildirebilirsiniz. Hile bildirimleri ödüllendirilebilir!
        </p>
        <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="btn btn-primary btn-sm">
          <i class="fa-brands fa-discord"></i>
          <span>Destek Talebi Aç</span>
        </a>
      </div>
    </div>
  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
