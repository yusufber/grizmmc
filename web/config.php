<?php
/**
 * GrizmMC Network - Ana Yapılandırma ve Bootstrap Dosyası
 * Tüm sunucu bilgileri, kimlik doğrulama anahtarları ve modül yükleyicisi.
 */

// Oturum Başlatma Güvenliği ve Çerez Bayrakları
if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off'),
        'httponly' => true,
        'samesite' => 'Lax'
    ]);
    session_start();
}

// CSRF Token Oluşturma ve Doğrulama Yardımcıları
if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

function get_csrf_token() {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

function verify_csrf_token($token = null, $autoExit = true) {
    if ($token === null) {
        $token = $_SERVER['HTTP_X_CSRF_TOKEN'] ?? $_POST['csrf_token'] ?? '';
        if ($token === '') {
            $raw = file_get_contents('php://input');
            if (!empty($raw)) {
                $decoded = json_decode($raw, true);
                if (is_array($decoded) && isset($decoded['csrf_token'])) {
                    $token = (string)$decoded['csrf_token'];
                }
            }
        }
    }
    $sessionToken = $_SESSION['csrf_token'] ?? '';
    $valid = ($sessionToken !== '' && is_string($token) && $token !== '' && hash_equals($sessionToken, $token));
    if (!$valid && $autoExit) {
        http_response_code(403);
        $isApi = (strpos($_SERVER['SCRIPT_NAME'] ?? '', '/api/') !== false)
            || (stripos($_SERVER['HTTP_ACCEPT'] ?? '', 'application/json') !== false)
            || (stripos($_SERVER['CONTENT_TYPE'] ?? '', 'application/json') !== false);
        if ($isApi) {
            header('Content-Type: application/json; charset=utf-8');
            echo json_encode([
                'status' => 'error',
                'success' => false,
                'message' => 'Güvenlik doğrulaması başarısız (CSRF Token geçersiz veya eksik). Lütfen sayfayı yenileyin.'
            ], JSON_UNESCAPED_UNICODE);
        } else {
            echo '403 Forbidden - Geçersiz CSRF güvenlik anahtarı. Lütfen sayfayı yenileyip tekrar deneyin.';
        }
        exit;
    }
    return $valid;
}

// ============================================================================
// 1. SUNUCU & WEB SİTESİ TEMEL AYARLARI
// ============================================================================
define('SERVER_NAME', 'GrizmMC Network');
define('SERVER_TAGLINE', 'Sınırları Zorlayan Yeni Nesil Minecraft Deneyimi');
define('SITE_URL', 'https://www.grizm.tech/mc');

// Minecraft Bağlantı Bilgileri
define('SERVER_IP', 'play-mc.grizm.tech');
define('BEDROCK_IP', 'play-mc.grizm.tech');
define('BEDROCK_PORT', '19132');
define('SERVER_VERSION', '1.16.5 - 1.20.x');

// ============================================================================
// 2. SOSYAL MEDYA & DESTEK KANALLARI
// ============================================================================
define('DISCORD_URL', 'https://discord.gg/grizmmc');
define('YOUTUBE_URL', 'https://youtube.com/@grizmmc');
define('TIKTOK_URL', 'https://tiktok.com/@grizmmc');
define('INSTAGRAM_URL', 'https://instagram.com/grizmmc');

define('STORE_CURRENCY', '₺');
define('SUPPORT_EMAIL', 'destek@grizm.tech');

// ============================================================================
// 3. LAUNCHER İNDİRME VE SÜRÜM AYARLARI
// ============================================================================
define('LAUNCHER_FILENAME', 'GrizmMcLauncher_Setup_v1.0.1.exe');
define('LAUNCHER_DOWNLOAD_URL', 'downloads/GrizmMcLauncher_Setup_v1.0.1.exe');
define('LAUNCHER_DIRECT_URL', 'downloads/GrizmMcLauncher_Setup_v1.0.1.exe');
define('LAUNCHER_VERSION', '1.0.1');
define('LAUNCHER_MC_VERSION', '1.21.4');

// ============================================================================
// 4. GRIZMID OAUTH2 KİMLİK DOĞRULAMA AYARLARI
// ============================================================================
define('AUTH_SERVER_URL', getenv('AUTH_SERVER_URL') ?: 'https://auth.dlmtd.com');
define('AUTH_CLIENT_ID', getenv('AUTH_CLIENT_ID') ?: '871cea3dbba7a4c10474');
define('AUTH_CLIENT_SECRET', getenv('AUTH_CLIENT_SECRET') ?: '6553923b681fbc5808b1f747e035493b7793b7ec');
define('AUTH_CALLBACK_URL', 'callback.php');
define('SERVER_API_KEY', getenv('SERVER_API_KEY') ?: 'grizm_mc_sec_auth_9471b028');
define('ADMIN_PANEL_PIN', getenv('ADMIN_PANEL_PIN') ?: 'Grizm2026!'); // Sunucu Kontrol Paneli Güvenlik PIN Kodu
define('PAYMENT_WEBHOOK_SECRET', getenv('PAYMENT_WEBHOOK_SECRET') ?: 'grizm_pay_whsec_8842a9f1c3e7'); // Shopier / PayTR / İyzico Webhook Gizli Anahtarı

// ============================================================================
// 5. MODÜLLERİN YÜKLENMESİ (BOOTSTRAP)
// ============================================================================
require_once __DIR__ . '/includes/db.php';           // Veri saklama (JSON flat-file) & yardımcılar
require_once __DIR__ . '/includes/handoff.php';      // Launcher giriş köprüsü (handoff) şablonu
require_once __DIR__ . '/includes/data.php';         // Oyun modları, kurallar, GrizmPerms Rol, İzin, VIP Mağaza & Cüzdan motoru
require_once __DIR__ . '/includes/auth.php';         // Oturum, OAuth ve yetki kontrolleri
require_once __DIR__ . '/includes/tickets.php';      // Destek biletleri (ticket) sistemi
require_once __DIR__ . '/includes/applications.php'; // Kadro başvurusu sistemi

// İlk Girişte Kullanıcı Adı Belirleme Kontrolü
check_username_requirement();
