<?php
require_once __DIR__ . '/../config.php';

// Sayfa başlığı kontrolü
$pageTitle = isset($pageTitle) ? $pageTitle . " | " . SERVER_NAME : SERVER_NAME . " - " . SERVER_TAGLINE;
$activePage = $activePage ?? 'home';
?>
<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><?php echo htmlspecialchars($pageTitle); ?></title>
  
  <!-- SEO & Meta Bilgileri -->
  <meta name="description" content="<?php echo htmlspecialchars(SERVER_TAGLINE); ?> - SkyBlock, SMP, PvP ve Turnuvalar. Hemen katılın: <?php echo SERVER_IP; ?>">
  <meta name="keywords" content="Minecraft sunucusu, Türk minecraft sunucusu, SkyBlock, SMP, PvP, GrizmMC, Minecraft Server">
  <meta name="author" content="GrizmMC Network">
  <meta name="theme-color" content="#070708">

  <!-- Open Graph / Sosyal Medya -->
  <meta property="og:title" content="<?php echo htmlspecialchars($pageTitle); ?>">
  <meta property="og:description" content="<?php echo htmlspecialchars(SERVER_TAGLINE); ?>">
  <meta property="og:type" content="website">
  <meta property="og:url" content="<?php echo defined('SITE_URL') ? SITE_URL : 'https://www.grizm.tech/mc'; ?>">

  <!-- FontAwesome 6 CDN -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" integrity="sha512-DTOQO9RWCH3ppGqcWaEA1BIZOC6xxalwEsw9c2QQeAIftl+Vegovlnee1c9QX4TctnWMn13TZye+giMm8e2LwA==" crossorigin="anonymous" referrerpolicy="no-referrer" />

  <!-- Özel Monokrom CSS -->
  <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
  <!-- İnteraktif Arka Plan Kanvası -->
  <canvas id="bg-canvas"></canvas>

  <?php if (!empty($_SESSION['flash_success'])): ?>
    <div class="toast-container">
      <div class="toast">
        <i class="fa-solid fa-circle-check"></i>
        <span><?php echo htmlspecialchars($_SESSION['flash_success']); ?></span>
      </div>
    </div>
    <?php unset($_SESSION['flash_success']); ?>
  <?php endif; ?>

  <?php if (!empty($_SESSION['flash_error'])): ?>
    <div class="toast-container">
      <div class="toast" style="border-color: #ff5555; box-shadow: 0 0 20px rgba(255,85,85,0.3);">
        <i class="fa-solid fa-triangle-exclamation" style="color: #ff5555;"></i>
        <span><?php echo htmlspecialchars($_SESSION['flash_error']); ?></span>
      </div>
    </div>
    <?php unset($_SESSION['flash_error']); ?>
  <?php endif; ?>
