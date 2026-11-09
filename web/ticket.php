<?php
/**
 * GrizmMC Network - Destek Talebi Detay & Mesajlaşma Ekranı
 */
$pageTitle = "Talep Detayı";
$activePage = "support";
require_once __DIR__ . '/config.php';

if (!is_logged_in()) {
    $_SESSION['flash_error'] = 'Bilet detayını görüntülemek için lütfen giriş yapın.';
    header('Location: login.php');
    exit;
}

$ticketId = trim($_GET['id'] ?? '');
$ticket = get_ticket_by_id($ticketId);

if (!$ticket) {
    $_SESSION['flash_error'] = 'Belirtilen destek talebi bulunamadı.';
    header('Location: support.php');
    exit;
}

$user = get_auth_user();
$userEmail = strtolower(trim($user['email']));
$isStaff = is_staff_user();
$isOwner = (strtolower(trim($ticket['user_email'])) === $userEmail);

// Yetki kontrolü: Yalnızca bilet sahibi veya yetkili görebilir
if (!$isOwner && !$isStaff) {
    $_SESSION['flash_error'] = 'Bu destek talebini görüntüleme yetkiniz bulunmamaktadır.';
    header('Location: support.php');
    exit;
}

// POST: Mesaj Yanıtlama
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'reply') {
    verify_csrf_token();
    $replyText = trim($_POST['message'] ?? '');

    if (empty($replyText)) {
        $_SESSION['flash_error'] = 'Lütfen boş mesaj göndermeyiniz.';
        header('Location: ticket.php?id=' . urlencode($ticketId));
        exit;
    }

    $senderRole = $isStaff ? 'staff' : 'user';
    $senderName = get_auth_username();

    add_ticket_message($ticketId, $senderName, $userEmail, $senderRole, $replyText);

    $_SESSION['flash_success'] = 'Yanıtınız başarıyla iletildi.';
    header('Location: ticket.php?id=' . urlencode($ticketId));
    exit;
}

// POST: Durum Güncelleme
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'update_status') {
    verify_csrf_token();
    $newStatus = trim($_POST['status'] ?? '');

    if (isset($TICKET_STATUSES[$newStatus])) {
        // Normal oyuncu yalnızca kapatabilir veya çözüldü yapabilir; yetkili her duruma geçirebilir
        if ($isStaff || in_array($newStatus, ['closed', 'resolved', 'open'])) {
            update_ticket_status($ticketId, $newStatus);
            $_SESSION['flash_success'] = 'Talep durumu güncellendi: ' . $TICKET_STATUSES[$newStatus]['title'];
        }
    }

    header('Location: ticket.php?id=' . urlencode($ticketId));
    exit;
}

// Güncel veriyi tekrar çek
$ticket = get_ticket_by_id($ticketId);
$cat = $TICKET_CATEGORIES[$ticket['category']] ?? $TICKET_CATEGORIES['general'];
$status = $TICKET_STATUSES[$ticket['status']] ?? $TICKET_STATUSES['open'];
$priority = $TICKET_PRIORITIES[$ticket['priority']] ?? $TICKET_PRIORITIES['medium'];
$messages = $ticket['messages'] ?? [];

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section" style="padding-top: 50px; min-height: 85vh;">
  <div class="container" style="max-width: 960px;">

    <!-- Üst Gezinme & Geri Butonu -->
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 14px;">
      <a href="support.php<?php echo ($isStaff && !$isOwner) ? '?tab=staff' : ''; ?>" class="btn btn-ghost btn-sm">
        <i class="fa-solid fa-arrow-left"></i>
        <span>Taleplere Dön</span>
      </a>

      <div style="display: flex; align-items: center; gap: 10px;">
        <span class="badge-priority priority-<?php echo $ticket['priority']; ?>">
          <i class="<?php echo $priority['icon']; ?>"></i> <?php echo $priority['title']; ?> Öncelik
        </span>
        <span class="badge-status <?php echo $status['badge']; ?>">
          <i class="<?php echo $status['icon']; ?>"></i> <?php echo $status['title']; ?>
        </span>
      </div>
    </div>

    <!-- Bilet Ana Başlık Kartı -->
    <div class="feature-box" style="padding: 30px; margin-bottom: 26px;">
      <div style="display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; flex-wrap: wrap;">
        <div style="display: flex; align-items: center; gap: 18px;">
          <div class="ticket-cat-avatar" style="width: 56px; height: 56px; font-size: 1.5rem; color: <?php echo $cat['color']; ?>;">
            <i class="<?php echo $cat['icon']; ?>"></i>
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 4px;">
              <span style="font-family: var(--font-mono); font-size: 0.85rem; color: var(--text-muted);">#<?php echo htmlspecialchars($ticket['id']); ?></span>
              <span style="font-size: 0.82rem; color: var(--text-secondary);"><i class="fa-solid fa-tag"></i> <?php echo htmlspecialchars($cat['title']); ?></span>
            </div>
            <h1 style="font-family: var(--font-display); font-size: 1.5rem; color: #fff; margin: 0; line-height: 1.3;">
              <?php echo htmlspecialchars($ticket['title']); ?>
            </h1>
          </div>
        </div>

        <!-- Hızlı Durum Butonları -->
        <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
          <?php if ($ticket['status'] !== 'closed'): ?>
            <form method="POST" action="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" style="display: inline;">
              <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
              <input type="hidden" name="action" value="update_status">
              <input type="hidden" name="status" value="closed">
              <button type="submit" class="btn btn-ghost btn-sm" style="color: #ff7b72; border-color: rgba(255, 123, 114, 0.4);" onclick="return confirm('Bu destek talebini kapatmak istediğinize emin misiniz?');">
                <i class="fa-solid fa-lock"></i>
                <span>Talebi Kapat</span>
              </button>
            </form>
          <?php else: ?>
            <form method="POST" action="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" style="display: inline;">
              <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
              <input type="hidden" name="action" value="update_status">
              <input type="hidden" name="status" value="open">
              <button type="submit" class="btn btn-ghost btn-sm" style="color: #58a6ff; border-color: rgba(88, 166, 255, 0.4);">
                <i class="fa-solid fa-lock-open"></i>
                <span>Yeniden Aç</span>
              </button>
            </form>
          <?php endif; ?>

          <?php if ($ticket['status'] !== 'resolved' && $ticket['status'] !== 'closed'): ?>
            <form method="POST" action="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" style="display: inline;">
              <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
              <input type="hidden" name="action" value="update_status">
              <input type="hidden" name="status" value="resolved">
              <button type="submit" class="btn btn-primary btn-sm">
                <i class="fa-solid fa-check"></i>
                <span>Çözüldü Olarak İşaretle</span>
              </button>
            </form>
          <?php endif; ?>
        </div>
      </div>

      <!-- Bilgi Çubuğu -->
      <div style="margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--border-subtle); display: flex; align-items: center; gap: 24px; font-size: 0.85rem; color: var(--text-secondary); flex-wrap: wrap;">
        <div>
          <span style="color: var(--text-muted);"><i class="fa-solid fa-user"></i> Oluşturan:</span>
          <strong style="color: #fff; margin-left: 4px;"><?php echo htmlspecialchars($ticket['user_name']); ?></strong>
        </div>
        <div>
          <span style="color: var(--text-muted);"><i class="fa-regular fa-calendar"></i> Açılış:</span>
          <span style="color: #fff; margin-left: 4px;"><?php echo date('d.m.Y H:i', $ticket['created_at']); ?></span>
        </div>
        <div>
          <span style="color: var(--text-muted);"><i class="fa-regular fa-clock"></i> Son Hareket:</span>
          <span style="color: #fff; margin-left: 4px;"><?php echo time_ago_tr($ticket['updated_at']); ?></span>
        </div>
      </div>
    </div>

    <!-- Yetkili Durum Yönetim Çubuğu -->
    <?php if ($isStaff): ?>
      <div class="staff-mode-bar" style="margin-bottom: 24px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <span class="staff-pill"><i class="fa-solid fa-user-shield"></i> Yetkili İşlemleri</span>
          <span style="font-size: 0.85rem; color: #fff;">Bu talebin durumunu manuel olarak değiştirebilirsiniz:</span>
        </div>
        <form method="POST" action="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" style="display: flex; align-items: center; gap: 10px;">
          <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
          <input type="hidden" name="action" value="update_status">
          <select name="status" class="form-select" style="padding: 6px 12px; font-size: 0.85rem; width: auto;" onchange="this.form.submit()">
            <?php foreach ($TICKET_STATUSES as $k => $st): ?>
              <option value="<?php echo $k; ?>" <?php echo ($ticket['status'] === $k) ? 'selected' : ''; ?>>
                <?php echo htmlspecialchars($st['title']); ?>
              </option>
            <?php endforeach; ?>
          </select>
        </form>
      </div>
    <?php endif; ?>

    <!-- Mesajlaşma Akışı (Thread) -->
    <div class="ticket-thread-container">
      <?php foreach ($messages as $msg): 
        $isStaffMsg = ($msg['sender_role'] === 'staff');
      ?>
        <div class="ticket-msg-box <?php echo $isStaffMsg ? 'staff-msg' : ''; ?>">
          <div class="msg-header">
            <div class="msg-sender">
              <img 
                src="https://mc-heads.net/avatar/<?php echo urlencode($msg['sender_name']); ?>/40" 
                alt="Avatar" 
                class="msg-avatar"
                onerror="this.src='https://mc-heads.net/avatar/Steve/40';"
              >
              <div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span class="msg-name"><?php echo htmlspecialchars($msg['sender_name']); ?></span>
                  <?php if ($isStaffMsg): ?>
                    <span class="staff-pill"><i class="fa-solid fa-shield"></i> GRIZM EKİBİ</span>
                  <?php else: ?>
                    <span class="user-pill"><i class="fa-solid fa-gamepad"></i> OYUNCU</span>
                  <?php endif; ?>
                </div>
              </div>
            </div>

            <span class="msg-time" title="<?php echo date('d.m.Y H:i:s', $msg['created_at']); ?>">
              <i class="fa-regular fa-clock"></i> <?php echo time_ago_tr($msg['created_at']); ?>
            </span>
          </div>

          <div class="msg-body">
            <?php echo nl2br(htmlspecialchars($msg['message'])); ?>
          </div>
        </div>
      <?php endforeach; ?>
    </div>

    <!-- Yanıt Gönderme Formu -->
    <?php if ($ticket['status'] !== 'closed' || $isStaff): ?>
      <div class="feature-box" style="padding: 28px;">
        <h3 style="font-family: var(--font-display); font-size: 1.25rem; color: #fff; margin-bottom: 6px;">
          <i class="fa-solid fa-reply"></i> Yanıt Yaz
        </h3>
        <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 18px;">
          <?php if ($isStaff): ?>
            <strong style="color: #3fb950;">Yetkili olarak yanıt veriyorsunuz.</strong> Mesajınız oyuncuya yetkili rozeti ile iletilecektir.
          <?php else: ?>
            Destek ekibimize ek bilgi veya cevap iletmek için mesajınızı yazın.
          <?php endif; ?>
        </p>

        <form method="POST" action="ticket.php?id=<?php echo urlencode($ticket['id']); ?>">
          <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
          <input type="hidden" name="action" value="reply">

          <div class="form-group">
            <textarea 
              name="message" 
              class="form-textarea" 
              rows="5" 
              placeholder="Cevabınızı buraya yazınız..." 
              required
            ></textarea>
          </div>

          <div style="display: flex; justify-content: flex-end; gap: 14px;">
            <button type="submit" class="btn btn-primary">
              <i class="fa-solid fa-paper-plane"></i>
              <span>Yanıtı Gönder</span>
            </button>
          </div>
        </form>
      </div>
    <?php else: ?>
      <div class="empty-box" style="padding: 40px 24px;">
        <i class="fa-solid fa-lock" style="font-size: 2.2rem; color: var(--text-muted); margin-bottom: 12px;"></i>
        <h4 style="color: #fff; font-size: 1.15rem; margin-bottom: 6px;">Bu Destek Talebi Kapatılmıştır</h4>
        <p style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 18px;">
          Sorununuz çözüldü veya yetkililer tarafından arşivlendi. Farklı bir sorunuz varsa lütfen yeni bir talep açınız.
        </p>
        <a href="support.php?tab=new" class="btn btn-primary btn-sm">
          <i class="fa-solid fa-plus"></i>
          <span>Yeni Destek Talebi Aç</span>
        </a>
      </div>
    <?php endif; ?>

  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
