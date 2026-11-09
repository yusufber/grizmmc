<?php
/**
 * GrizmMC Network - Destek Talebi (Ticket) Sistemi
 */
$pageTitle = "Destek Talebi";
$activePage = "support";
require_once __DIR__ . '/config.php';

// Form Gönderimi (Yeni Bilet Oluşturma)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'create_ticket') {
    if (!is_logged_in()) {
        $_SESSION['flash_error'] = 'Destek talebi açmak için lütfen önce giriş yapın.';
        header('Location: login.php');
        exit;
    }

    $title = trim($_POST['title'] ?? '');
    $category = trim($_POST['category'] ?? 'general');
    $priority = trim($_POST['priority'] ?? 'medium');
    $message = trim($_POST['message'] ?? '');

    if (empty($title) || empty($message)) {
        $_SESSION['flash_error'] = 'Lütfen bilet başlığını ve mesajınızı eksiksiz doldurun.';
        header('Location: support.php?tab=new');
        exit;
    }

    if (!isset($TICKET_CATEGORIES[$category])) {
        $category = 'general';
    }

    if (!isset($TICKET_PRIORITIES[$priority])) {
        $priority = 'medium';
    }

    $user = get_auth_user();
    $newTicket = create_ticket([
        'title' => $title,
        'category' => $category,
        'priority' => $priority,
        'message' => $message,
        'user_email' => $user['email'],
        'user_name' => get_auth_username()
    ]);

    $_SESSION['flash_success'] = 'Destek talebiniz #' . $newTicket['id'] . ' başarıyla oluşturuldu!';
    header('Location: ticket.php?id=' . $newTicket['id']);
    exit;
}

$user = get_auth_user();
$isStaff = is_staff_user();
$currentTab = $_GET['tab'] ?? ($isStaff && isset($_GET['staff']) ? 'staff' : (is_logged_in() ? 'my' : 'info'));

// Talepleri Hazırla
$userTickets = is_logged_in() ? get_user_tickets($user['email']) : [];
$allTickets = $isStaff ? get_all_tickets() : [];

// Yetkili listesini son güncellenene göre sırala
if ($isStaff) {
    uasort($allTickets, function($a, $b) {
        return ($b['updated_at'] ?? 0) <=> ($a['updated_at'] ?? 0);
    });
}

// İstatistikler
$openCount = 0;
$resolvedCount = 0;
$pendingStaffCount = 0;

foreach ($allTickets as $t) {
    if (in_array($t['status'], ['open', 'user_reply'])) {
        $pendingStaffCount++;
    }
    if ($t['status'] === 'resolved') {
        $resolvedCount++;
    }
    if ($t['status'] !== 'closed') {
        $openCount++;
    }
}

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section" style="padding-top: 50px; min-height: 85vh;">
  <div class="container">
    
    <!-- Başlık & Bilgi -->
    <div class="section-header" style="margin-bottom: 36px;">
      <span class="section-tag"><i class="fa-solid fa-headset"></i> Destek & Çözüm Merkezi</span>
      <h1 class="section-title">DESTEK TALEPLERİ</h1>
      <p class="section-desc">
        Oyun içi hatalar, mağaza sorunları, şikayetler veya genel sorularınız için 7/24 destek ekibimizle iletişime geçin.
      </p>
    </div>

    <!-- Yetkili Durum Bildirim Çubuğu -->
    <?php if (is_logged_in()): ?>
      <div class="staff-mode-bar">
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="width: 10px; height: 10px; border-radius: 50%; background: <?php echo $isStaff ? '#3fb950' : '#8b949e'; ?>; box-shadow: 0 0 10px <?php echo $isStaff ? '#3fb950' : 'transparent'; ?>;"></span>
          <span style="font-size: 0.88rem; color: #fff;">
            Mod: <strong><?php echo $isStaff ? 'Yetkili / Yönetici Paneli Aktif' : 'Oyuncu Görünümü'; ?></strong>
          </span>
          <?php if ($isStaff): ?>
            <span class="staff-pill"><i class="fa-solid fa-shield-check"></i> Yetkili</span>
          <?php endif; ?>
        </div>
        <a href="?toggle_staff=1" class="btn btn-ghost btn-sm" title="Test ve yetkili modunu değiştir">
          <i class="fa-solid fa-sliders"></i>
          <span><?php echo $isStaff ? 'Oyuncu Moduna Geç' : 'Yetkili Moduna Geç (Test)'; ?></span>
        </a>
      </div>
    <?php endif; ?>

    <!-- Üst İstatistik Kartları -->
    <div class="portal-stats-grid">
      <div class="portal-stat-card">
        <div class="portal-stat-icon" style="color: #58a6ff;">
          <i class="fa-solid fa-bolt"></i>
        </div>
        <div class="portal-stat-content">
          <div class="portal-stat-num">&lt; 30 Dk</div>
          <div class="portal-stat-label">Ortalama Yanıt Süresi</div>
        </div>
      </div>

      <div class="portal-stat-card">
        <div class="portal-stat-icon" style="color: #3fb950;">
          <i class="fa-solid fa-circle-check"></i>
        </div>
        <div class="portal-stat-content">
          <div class="portal-stat-num">%99.4</div>
          <div class="portal-stat-label">Çözüm Memnuniyeti</div>
        </div>
      </div>

      <div class="portal-stat-card">
        <div class="portal-stat-icon" style="color: #f2cc60;">
          <i class="fa-solid fa-comments"></i>
        </div>
        <div class="portal-stat-content">
          <div class="portal-stat-num"><?php echo count($userTickets); ?></div>
          <div class="portal-stat-label"><?php echo is_logged_in() ? 'Açtığınız Talepler' : 'Kişisel Biletleriniz'; ?></div>
        </div>
      </div>

      <?php if ($isStaff): ?>
        <div class="portal-stat-card" style="border-color: rgba(255, 123, 114, 0.4);">
          <div class="portal-stat-icon" style="color: #ff7b72;">
            <i class="fa-solid fa-inbox"></i>
          </div>
          <div class="portal-stat-content">
            <div class="portal-stat-num"><?php echo $pendingStaffCount; ?></div>
            <div class="portal-stat-label">Yanıt Bekleyen (Sunucu)</div>
          </div>
        </div>
      <?php else: ?>
        <div class="portal-stat-card">
          <div class="portal-stat-icon" style="color: #d2a8ff;">
            <i class="fa-solid fa-shield-halved"></i>
          </div>
          <div class="portal-stat-content">
            <div class="portal-stat-num">7/24</div>
            <div class="portal-stat-label">Aktif Moderasyon</div>
          </div>
        </div>
      <?php endif; ?>
    </div>

    <!-- Sekmeler (Tab Menü) -->
    <div class="portal-tabs">
      <?php if (is_logged_in()): ?>
        <a href="support.php?tab=my" class="portal-tab <?php echo ($currentTab === 'my') ? 'active' : ''; ?>">
          <i class="fa-solid fa-folder-open"></i>
          <span>Taleplerim</span>
          <span class="portal-tab-badge"><?php echo count($userTickets); ?></span>
        </a>

        <a href="support.php?tab=new" class="portal-tab <?php echo ($currentTab === 'new') ? 'active' : ''; ?>">
          <i class="fa-solid fa-plus-circle"></i>
          <span>Yeni Talep Oluştur</span>
        </a>

        <?php if ($isStaff): ?>
          <a href="support.php?tab=staff" class="portal-tab <?php echo ($currentTab === 'staff') ? 'active' : ''; ?>" style="color: <?php echo ($currentTab === 'staff') ? '#000' : '#58a6ff'; ?>;">
            <i class="fa-solid fa-shield-halved"></i>
            <span>Tüm Sunucu Talepleri (Yetkili)</span>
            <span class="portal-tab-badge" style="background: <?php echo ($currentTab === 'staff') ? 'rgba(0,0,0,0.15)' : 'rgba(88,166,255,0.2)'; ?>; color: inherit;">
              <?php echo count($allTickets); ?>
            </span>
          </a>
        <?php endif; ?>

      <?php else: ?>
        <a href="support.php" class="portal-tab active">
          <i class="fa-solid fa-circle-info"></i>
          <span>Destek Bilgileri</span>
        </a>
      <?php endif; ?>
    </div>

    <!-- TAB 1: GİRİŞ YAPILMAMIŞSA BİLGİ & GİRİŞ ÇAĞRISI -->
    <?php if (!is_logged_in()): ?>
      <div class="feature-box" style="padding: 48px 36px; text-align: center; max-width: 680px; margin: 0 auto;">
        <div style="width: 80px; height: 80px; border-radius: var(--radius-md); background: #14141a; border: 1px solid var(--border-medium); display: flex; align-items: center; justify-content: center; margin: 0 auto 24px; font-size: 2.2rem; color: #fff;">
          <i class="fa-solid fa-user-lock"></i>
        </div>
        <h2 style="font-family: var(--font-display); font-size: 1.8rem; color: #fff; margin-bottom: 12px; text-transform: uppercase;">
          Destek Almak İçin Giriş Yapın
        </h2>
        <p style="color: var(--text-secondary); font-size: 1rem; line-height: 1.6; margin-bottom: 30px;">
          Destek biletinizin açılabilmesi, güvenliğiniz ve yetkililerimizin oyun içi profilinizi tanıyıp hızlı aksiyon alabilmesi için <strong>GrizmID</strong> ile oturum açmanız gerekmektedir.
        </p>

        <div style="display: flex; gap: 14px; justify-content: center; flex-wrap: wrap;">
          <a href="login.php" class="btn btn-primary btn-lg">
            <i class="fa-solid fa-right-to-bracket"></i>
            <span>GrizmID ile Giriş Yap</span>
          </a>
          <a href="<?php echo DISCORD_URL; ?>" target="_blank" class="btn btn-ghost btn-lg">
            <i class="fa-brands fa-discord" style="color: #5865F2;"></i>
            <span>Discord Desteği</span>
          </a>
        </div>

        <div style="margin-top: 36px; padding-top: 24px; border-top: 1px solid var(--border-subtle); display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; text-align: left;">
          <div>
            <strong style="color: #fff; display: block; font-size: 0.9rem; margin-bottom: 4px;"><i class="fa-solid fa-check"></i> Anlık Bildirim</strong>
            <span style="font-size: 0.78rem; color: var(--text-muted);">Talebinize yanıt geldiğinde sistem üzerinden takip edin.</span>
          </div>
          <div>
            <strong style="color: #fff; display: block; font-size: 0.9rem; margin-bottom: 4px;"><i class="fa-solid fa-check"></i> Doğrulanmış Hesap</strong>
            <span style="font-size: 0.78rem; color: var(--text-muted);">Minecraft karakteriniz otomatik olarak eşleşir.</span>
          </div>
          <div>
            <strong style="color: #fff; display: block; font-size: 0.9rem; margin-bottom: 4px;"><i class="fa-solid fa-check"></i> Güvenli İletişim</strong>
            <span style="font-size: 0.78rem; color: var(--text-muted);">Biletleriniz yalnızca siz ve üst yönetim tarafından görülür.</span>
          </div>
        </div>
      </div>

    <!-- TAB 2: TALEPLERİM -->
    <?php elseif ($currentTab === 'my'): ?>
      <?php if (empty($userTickets)): ?>
        <div class="empty-box">
          <i class="fa-regular fa-folder-open"></i>
          <h3>Henüz Açılmış Bir Destek Talebiniz Yok</h3>
          <p style="margin-bottom: 24px;">Herhangi bir konuda yardıma ihtiyacınız olursa anında yeni bir talep açabilirsiniz.</p>
          <a href="support.php?tab=new" class="btn btn-primary">
            <i class="fa-solid fa-plus"></i>
            <span>Yeni Talep Oluştur</span>
          </a>
        </div>
      <?php else: ?>
        <div class="tickets-list">
          <?php foreach ($userTickets as $ticket): 
            $cat = $TICKET_CATEGORIES[$ticket['category']] ?? $TICKET_CATEGORIES['general'];
            $status = $TICKET_STATUSES[$ticket['status']] ?? $TICKET_STATUSES['open'];
            $priority = $TICKET_PRIORITIES[$ticket['priority']] ?? $TICKET_PRIORITIES['medium'];
            $msgCount = count($ticket['messages'] ?? []);
          ?>
            <a href="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" class="ticket-card">
              <div class="ticket-card-left">
                <div class="ticket-cat-avatar" style="color: <?php echo $cat['color']; ?>;" title="<?php echo htmlspecialchars($cat['title']); ?>">
                  <i class="<?php echo $cat['icon']; ?>"></i>
                </div>
                <div class="ticket-info">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-muted);">#<?php echo htmlspecialchars($ticket['id']); ?></span>
                    <h3 class="ticket-title"><?php echo htmlspecialchars($ticket['title']); ?></h3>
                  </div>
                  <div class="ticket-meta">
                    <span><i class="fa-solid fa-tag"></i> <?php echo htmlspecialchars($cat['short']); ?></span>
                    <span><i class="fa-regular fa-clock"></i> <?php echo time_ago_tr($ticket['updated_at']); ?></span>
                    <span><i class="fa-solid fa-message"></i> <?php echo $msgCount; ?> mesaj</span>
                  </div>
                </div>
              </div>

              <div class="ticket-card-right">
                <span class="badge-priority priority-<?php echo $ticket['priority']; ?>">
                  <i class="<?php echo $priority['icon']; ?>"></i> <?php echo $priority['title']; ?>
                </span>
                <span class="badge-status <?php echo $status['badge']; ?>">
                  <i class="<?php echo $status['icon']; ?>"></i> <?php echo $status['short']; ?>
                </span>
                <div style="color: var(--text-muted); font-size: 1.1rem; padding-left: 6px;">
                  <i class="fa-solid fa-chevron-right"></i>
                </div>
              </div>
            </a>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>

    <!-- TAB 3: YENİ TALEP OLUŞTUR -->
    <?php elseif ($currentTab === 'new'): ?>
      <div class="feature-box" style="padding: 36px; max-width: 820px; margin: 0 auto;">
        <div style="margin-bottom: 24px;">
          <h2 style="font-family: var(--font-display); font-size: 1.6rem; color: #fff; margin-bottom: 6px; text-transform: uppercase;">
            Yeni Destek Talebi Aç
          </h2>
          <p style="color: var(--text-secondary); font-size: 0.92rem;">
            Lütfen sorununuzu en açık şekilde tarif edin. Ekibimiz en kısa sürede talebinizi inceleyecektir.
          </p>
        </div>

        <form method="POST" action="support.php">
          <input type="hidden" name="action" value="create_ticket">

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;" class="form-row">
            <div class="form-group">
              <label class="form-label">Kullanıcı Bilgisi</label>
              <div style="display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-medium); padding: 10px 14px; border-radius: var(--radius-sm);">
                <img 
                  src="https://mc-heads.net/avatar/<?php echo urlencode(get_auth_username()); ?>/28" 
                  alt="Avatar" 
                  style="width: 28px; height: 28px; border-radius: 4px;"
                >
                <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  <strong style="color: #fff; font-size: 0.9rem;"><?php echo htmlspecialchars(get_auth_username()); ?></strong>
                  <small style="color: var(--text-muted); display: block; font-size: 0.75rem;"><?php echo htmlspecialchars($user['email']); ?></small>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label for="category" class="form-label">Kategori Seçin</label>
              <select name="category" id="category" class="form-select" required>
                <?php foreach ($TICKET_CATEGORIES as $key => $cat): ?>
                  <option value="<?php echo $key; ?>"><?php echo htmlspecialchars($cat['title']); ?></option>
                <?php endforeach; ?>
              </select>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 20px;" class="form-row">
            <div class="form-group">
              <label for="title" class="form-label">Konu / Başlık</label>
              <input 
                type="text" 
                id="title" 
                name="title" 
                class="form-input" 
                placeholder="Örn: SkyBlock adamda spawner kayboldu" 
                required 
                maxlength="120"
              >
            </div>

            <div class="form-group">
              <label for="priority" class="form-label">Öncelik Derecesi</label>
              <select name="priority" id="priority" class="form-select">
                <option value="low">Düşük</option>
                <option value="medium" selected>Normal</option>
                <option value="high">Yüksek</option>
                <option value="urgent">Acil</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label for="message" class="form-label">Mesajınız & Detaylı Açıklama</label>
            <textarea 
              name="message" 
              id="message" 
              class="form-textarea" 
              rows="6" 
              placeholder="Olayın ne zaman gerçekleştiğini, oyun modu adını, koordinatları veya işlem kimliklerini (varsa) detaylıca yazınız..." 
              required
            ></textarea>
            <small style="color: var(--text-muted); font-size: 0.78rem; display: block; margin-top: 6px;">
              <i class="fa-solid fa-circle-info"></i> Küfür veya hakaret içeren biletler doğrudan kapatılır ve hesabınız uzaklaştırılabilir.
            </small>
          </div>

          <div style="display: flex; justify-content: flex-end; gap: 14px; margin-top: 24px;">
            <a href="support.php?tab=my" class="btn btn-ghost">İptal</a>
            <button type="submit" class="btn btn-primary btn-lg">
              <i class="fa-solid fa-paper-plane"></i>
              <span>Destek Talebini Gönder</span>
            </button>
          </div>
        </form>
      </div>

    <!-- TAB 4: YETKİLİ GÖRÜNÜMÜ (TÜM SUNUCU TALEPLERİ) -->
    <?php elseif ($currentTab === 'staff' && $isStaff): ?>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 14px;">
        <div>
          <h3 style="font-family: var(--font-display); font-size: 1.4rem; color: #fff;">
            YETKİLİ DESTEK HAVUZU
          </h3>
          <p style="color: var(--text-secondary); font-size: 0.85rem;">
            Tüm oyuncuların açtığı destek taleplerini buradan inceleyebilir, yanıtlayabilir veya kapatabilirsiniz.
          </p>
        </div>
        <span class="portal-tab-badge" style="background: rgba(255,255,255,0.1); color: #fff; padding: 6px 14px; font-size: 0.82rem;">
          Toplam Bilet: <?php echo count($allTickets); ?>
        </span>
      </div>

      <?php if (empty($allTickets)): ?>
        <div class="empty-box">
          <i class="fa-solid fa-inbox"></i>
          <h3>Henüz Hiç Bilet Açılmamış</h3>
          <p>Oyuncular talep oluşturduğunda burada listelenecektir.</p>
        </div>
      <?php else: ?>
        <div class="tickets-list">
          <?php foreach ($allTickets as $ticket): 
            $cat = $TICKET_CATEGORIES[$ticket['category']] ?? $TICKET_CATEGORIES['general'];
            $status = $TICKET_STATUSES[$ticket['status']] ?? $TICKET_STATUSES['open'];
            $priority = $TICKET_PRIORITIES[$ticket['priority']] ?? $TICKET_PRIORITIES['medium'];
            $msgCount = count($ticket['messages'] ?? []);
          ?>
            <a href="ticket.php?id=<?php echo urlencode($ticket['id']); ?>" class="ticket-card" style="border-left: 4px solid <?php echo $status['color']; ?>;">
              <div class="ticket-card-left">
                <img 
                  src="https://mc-heads.net/avatar/<?php echo urlencode($ticket['user_name']); ?>/42" 
                  alt="Avatar" 
                  class="ticket-cat-avatar"
                  onerror="this.src='https://mc-heads.net/avatar/Steve/42';"
                >
                <div class="ticket-info">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-muted);">#<?php echo htmlspecialchars($ticket['id']); ?></span>
                    <h3 class="ticket-title"><?php echo htmlspecialchars($ticket['title']); ?></h3>
                  </div>
                  <div class="ticket-meta">
                    <span style="color: #fff; font-weight: 600;"><i class="fa-solid fa-user"></i> <?php echo htmlspecialchars($ticket['user_name']); ?></span>
                    <span><i class="<?php echo $cat['icon']; ?>" style="color: <?php echo $cat['color']; ?>;"></i> <?php echo htmlspecialchars($cat['short']); ?></span>
                    <span><i class="fa-regular fa-clock"></i> <?php echo time_ago_tr($ticket['updated_at']); ?></span>
                    <span><i class="fa-solid fa-message"></i> <?php echo $msgCount; ?></span>
                  </div>
                </div>
              </div>

              <div class="ticket-card-right">
                <span class="badge-priority priority-<?php echo $ticket['priority']; ?>">
                  <i class="<?php echo $priority['icon']; ?>"></i> <?php echo $priority['title']; ?>
                </span>
                <span class="badge-status <?php echo $status['badge']; ?>">
                  <i class="<?php echo $status['icon']; ?>"></i> <?php echo $status['short']; ?>
                </span>
                <div style="color: var(--text-muted); font-size: 1.1rem; padding-left: 6px;">
                  <i class="fa-solid fa-chevron-right"></i>
                </div>
              </div>
            </a>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>

    <?php endif; ?>

  </div>
</section>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
