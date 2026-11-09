<?php
$pageTitle = "Yetkili Ekibi";
$activePage = "staff";
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Kadro</span>
      <h1 class="section-title">YETKİLİ EKİBİ</h1>
      <p class="section-desc">
        GrizmMC Network altyapısını geliştiren, topluluğu yöneten ve oyuncu deneyimini en üst seviyede tutan ekibimizle tanışın.
      </p>
    </div>

    <div class="staff-grid">
      <?php foreach ($STAFF_MEMBERS as $staff): ?>
        <div class="staff-card">
          <div class="staff-avatar-wrap">
            <img 
              src="https://mc-heads.net/avatar/<?php echo urlencode($staff['username']); ?>/80" 
              alt="<?php echo htmlspecialchars($staff['username']); ?>" 
              class="staff-avatar"
            >
          </div>

          <span class="staff-role-badge"><?php echo htmlspecialchars($staff['role']); ?></span>
          <h3 class="staff-name"><?php echo htmlspecialchars($staff['username']); ?></h3>
          
          <div class="staff-discord">
            <i class="fa-brands fa-discord"></i>
            <span><?php echo htmlspecialchars($staff['discord']); ?></span>
          </div>

          <p class="staff-desc"><?php echo htmlspecialchars($staff['description']); ?></p>
        </div>
      <?php endforeach; ?>
    </div>

    <!-- YETKİLİ ALIMI BAŞVURU KARTI -->
    <div class="discord-cta-card" style="margin-top: 60px;">
      <div class="discord-cta-content">
        <h3>EKİBİMİZE KATILMAK İSTER MİSİNİZ?</h3>
        <p>
          Rehber, Moderatör veya Tasarımcı kadromuz için başvurular belirli aralıklarla açılmaktadır. Şartları öğrenmek ve form doldurmak için Discord sunucumuza katılabilirsiniz.
        </p>
      </div>
      <div>
        <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="btn btn-primary btn-lg">
          <i class="fa-solid fa-file-pen"></i>
          <span>Başvuru Yap</span>
        </a>
      </div>
    </div>
  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
