<?php
/**
 * GrizmMC Network - İlk Giriş Minecraft Kullanıcı Adı Belirleme
 */

$pageTitle = "Minecraft Kullanıcı Adını Belirle";
$activePage = "set-username";
require_once __DIR__ . '/config.php';

// Oturum kontrolü
if (!is_logged_in()) {
    header('Location: login.php');
    exit;
}

$user = get_auth_user();
$isFirstLogin = !empty($_SESSION['needs_username']) || empty($user['minecraft_username']);
$isEditMode = isset($_GET['edit']) && $_GET['edit'] == '1';

// Eğer kullanıcı adı zaten varsa ve düzenleme modunda değilse profile yönlendir
if (!$isFirstLogin && !$isEditMode) {
    header('Location: profile.php');
    exit;
}

$error = '';
$currentUsername = $user['minecraft_username'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verify_csrf_token();
    $rawUsername = trim($_POST['minecraft_username'] ?? '');

    $userEmailLower = strtolower(trim($user['email'] ?? ''));
    $isEmailAdmin = in_array($userEmailLower, array_map('strtolower', ADMIN_EMAILS), true);
    $isReservedAdminName = in_array(strtolower($rawUsername), array_map('strtolower', ADMIN_USERNAMES), true);

    // Kullanıcı adı başka bir e-postaya kayıtlı mı kontrol et (users.json ve user_perms.json)
    $isUsernameTaken = false;
    $rawLower = strtolower($rawUsername);
    if ($rawLower !== '') {
        $allSavedUsers = get_all_saved_users();
        foreach ($allSavedUsers as $savedEmail => $savedProfile) {
            $savedMc = strtolower(trim($savedProfile['minecraft_username'] ?? ''));
            $ownerEmail = strtolower(trim($savedProfile['email'] ?? $savedEmail));
            if ($savedMc !== '' && $savedMc === $rawLower && $ownerEmail !== $userEmailLower) {
                $isUsernameTaken = true;
                break;
            }
        }

        if (!$isUsernameTaken && function_exists('gperm_get_all_user_perms')) {
            $allPermUsers = gperm_get_all_user_perms(false);
            foreach ($allPermUsers as $permKey => $permRecord) {
                $permMc = strtolower(trim($permRecord['username'] ?? $permKey));
                $permEmail = strtolower(trim($permRecord['email'] ?? ''));
                if ($permMc !== '' && $permMc === $rawLower) {
                    if (($permEmail !== '' && $permEmail !== $userEmailLower) || ($permEmail === '' && strtolower($currentUsername) !== $rawLower)) {
                        $isUsernameTaken = true;
                        break;
                    }
                }
            }
        }
    }

    // Minecraft kullanıcı adı kuralları: 3-16 karakter, harf, rakam, alt çizgi
    if (empty($rawUsername)) {
        $error = 'Lütfen bir Minecraft kullanıcı adı yazın.';
    } elseif (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $rawUsername)) {
        $error = 'Kullanıcı adı 3-16 karakter uzunluğunda olmalı ve yalnızca harf, rakam ve alt çizgi (_) içerebilir.';
    } elseif (!$isEmailAdmin && $isReservedAdminName) {
        $error = 'Bu kullanıcı adı sistem yöneticilerine ayrılmıştır ve alınamaz.';
    } elseif ($isUsernameTaken) {
        $error = 'Bu Minecraft kullanıcı adı başka bir hesaba kayıtlıdır. Lütfen farklı bir kullanıcı adı seçin.';
    } else {
        // Başarıyla kaydet
        save_user_profile($user['email'], [
            'full_name' => $user['full_name'],
            'email' => $user['email'],
            'minecraft_username' => $rawUsername,
            'updated_at' => date('Y-m-d H:i:s')
        ]);

        $_SESSION['auth_user']['minecraft_username'] = $rawUsername;
        unset($_SESSION['needs_username']);

        if (!empty($_SESSION['auth_source']) && $_SESSION['auth_source'] === 'launcher') {
            render_launcher_handoff($rawUsername, $user['email'], $user['full_name']);
            exit;
        }

        $_SESSION['flash_success'] = 'Minecraft kullanıcı adınız "' . htmlspecialchars($rawUsername) . '" olarak kaydedildi!';
        header('Location: profile.php');
        exit;
    }
}

require_once __DIR__ . '/includes/header.php';
require_once __DIR__ . '/includes/navbar.php';
?>

<section class="section" style="padding-top: 60px; min-height: 80vh; display: flex; align-items: center;">
  <div class="container">
    <div style="max-width: 540px; margin: 0 auto;">
      <div class="feature-box" style="padding: 44px 36px; border: 1px solid var(--border-strong); box-shadow: 0 25px 60px rgba(0,0,0,0.8), 0 0 35px rgba(255,255,255,0.06); text-align: center;">
        
        <!-- Canlı 3D Kafa Önizlemesi -->
        <div style="margin-bottom: 24px; position: relative; display: inline-block;">
          <div style="width: 104px; height: 104px; border-radius: var(--radius-md); background: #121216; border: 2px solid var(--border-medium); padding: 8px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center;">
            <img 
              id="avatar-live-preview" 
              src="https://mc-heads.net/avatar/<?php echo !empty($currentUsername) ? urlencode($currentUsername) : 'Steve'; ?>/88" 
              alt="Minecraft Kafa Önizlemesi"
              style="width: 100%; height: 100%; image-rendering: pixelated; border-radius: var(--radius-sm);"
              onerror="this.src='https://mc-heads.net/avatar/Steve/88';"
            >
          </div>
          <span style="display: block; font-family: var(--font-mono); font-size: 0.72rem; color: var(--text-muted); margin-top: 8px;">
            Canlı Karakter Önizlemesi
          </span>
        </div>

        <h2 style="font-family: var(--font-display); font-size: 1.8rem; font-weight: 800; color: #ffffff; margin-bottom: 10px; text-transform: uppercase;">
          <?php echo $isFirstLogin ? 'HOŞ GELDİN, ' . htmlspecialchars($user['full_name']) . '!' : 'KULLANICI ADINI GÜNCELLE'; ?>
        </h2>
        
        <p style="font-size: 0.95rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 28px;">
          <?php if ($isFirstLogin): ?>
            GrizmID hesabınızla ilk kez giriş yaptınız! Sunucumuzda, liderlik tablosunda ve mağazada kullanılacak <strong>Minecraft kullanıcı adınızı</strong> belirleyin.
          <?php else: ?>
            GrizmMC Network hesabınıza bağlı Minecraft kullanıcı adınızı buradan güncelleyebilirsiniz.
          <?php endif; ?>
        </p>

        <?php if (!empty($error)): ?>
          <div style="background: rgba(255, 85, 85, 0.1); border: 1px solid rgba(255, 85, 85, 0.4); color: #ff8888; padding: 12px 16px; border-radius: var(--radius-sm); font-size: 0.88rem; margin-bottom: 22px; text-align: left; display: flex; align-items: center; gap: 10px;">
            <i class="fa-solid fa-triangle-exclamation"></i>
            <span><?php echo htmlspecialchars($error); ?></span>
          </div>
        <?php endif; ?>

        <form method="POST" action="">
          <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(get_csrf_token(), ENT_QUOTES, 'UTF-8'); ?>">
          <div class="form-group" style="text-align: left; margin-bottom: 24px;">
            <label for="minecraft_username" class="form-label" style="font-size: 0.82rem; letter-spacing: 1px;">
              Minecraft Oyuncu Adınız (IGN)
            </label>
            <input 
              type="text" 
              id="minecraft_username" 
              name="minecraft_username" 
              class="form-input" 
              placeholder="Örn: Notch, NightHunter..." 
              value="<?php echo htmlspecialchars($currentUsername); ?>" 
              required 
              autofocus 
              autocomplete="off"
              maxlength="16"
              style="font-family: var(--font-mono); font-size: 1.05rem;"
            >
            <small style="color: var(--text-muted); font-size: 0.76rem; display: block; margin-top: 6px;">
              3-16 karakter, Türkçe karakter olmadan, oyun içindeki adınızla aynı olmalıdır.
            </small>
          </div>

          <button type="submit" class="btn btn-primary btn-lg" style="width: 100%;">
            <i class="fa-solid fa-check"></i>
            <span>Kullanıcı Adımı Kaydet</span>
          </button>
        </form>

      </div>
    </div>
  </div>
</section>

<!-- Anlık Kafa Önizlemesi Scripti -->
<script>
document.addEventListener('DOMContentLoaded', () => {
  const usernameInput = document.getElementById('minecraft_username');
  const avatarImg = document.getElementById('avatar-live-preview');
  let debounceTimeout = null;

  if (usernameInput && avatarImg) {
    usernameInput.addEventListener('input', () => {
      clearTimeout(debounceTimeout);
      debounceTimeout = setTimeout(() => {
        const val = usernameInput.value.trim();
        if (val.length >= 3) {
          avatarImg.src = 'https://mc-heads.net/avatar/' + encodeURIComponent(val) + '/88';
        } else {
          avatarImg.src = 'https://mc-heads.net/avatar/Steve/88';
        }
      }, 350);
    });
  }
});
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
