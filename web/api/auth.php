<?php
/**
 * GrizmMC Network - Grizm Auth Web API
 * Launcher oturum başlatma ve Minecraft Paper/Spigot sunucu doğrulama uç noktası
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Server-Key, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config.php';

// JSON veya Form verilerini al
$inputData = [];
$rawInput = file_get_contents('php://input');
if (!empty($rawInput)) {
    $decoded = json_decode($rawInput, true);
    if (is_array($decoded)) {
        $inputData = $decoded;
    }
}

$action = trim($_GET['action'] ?? $_POST['action'] ?? $inputData['action'] ?? 'status');
$clientIp = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
if (strpos($clientIp, ',') !== false) {
    $clientIp = trim(explode(',', $clientIp)[0]);
}

// ----------------------------------------------------------------------------
// 1. LAUNCHER OTURUM BAŞLATMA (start_session)
// ----------------------------------------------------------------------------
if ($action === 'start_session') {
    $username = trim($_POST['username'] ?? $inputData['username'] ?? $_GET['username'] ?? '');
    $token = trim($_POST['token'] ?? $inputData['token'] ?? $_GET['token'] ?? '');
    $email = trim($_POST['email'] ?? $inputData['email'] ?? $_GET['email'] ?? '');

    if (empty($username)) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Kullanıcı adı (username) zorunludur.'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if (empty($token)) {
        $token = bin2hex(random_bytes(16));
    }

    $session = create_launcher_session($username, $token, $email, $clientIp);

    echo json_encode([
        'status' => 'success',
        'message' => 'Launcher oturumu oluşturuldu.',
        'session_id' => $session['session_id'],
        'username' => $session['username'],
        'expires_in' => 300
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// ----------------------------------------------------------------------------
// 2. MINECRAFT SUNUCUSU OYUNCU DOĞRULAMA (verify_join)
// ----------------------------------------------------------------------------
if ($action === 'verify_join') {
    $username = trim($_POST['username'] ?? $inputData['username'] ?? $_GET['username'] ?? '');
    $playerIp = trim($_POST['ip'] ?? $inputData['ip'] ?? $_GET['ip'] ?? $clientIp);
    $serverKey = trim($_SERVER['HTTP_X_SERVER_KEY'] ?? $_POST['server_key'] ?? $inputData['server_key'] ?? $_GET['server_key'] ?? '');

    if (empty($username)) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'authenticated' => false,
            'message' => 'Kullanıcı adı belirtilmedi.'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Güvenlik anahtarı kontrolü (varsa doğrula)
    if (defined('SERVER_API_KEY') && !empty(SERVER_API_KEY) && !empty($serverKey)) {
        if (!hash_equals(SERVER_API_KEY, $serverKey)) {
            http_response_code(403);
            echo json_encode([
                'status' => 'error',
                'authenticated' => false,
                'message' => 'Geçersiz sunucu güvenlik anahtarı (X-Server-Key).'
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }

    $verifiedSession = verify_launcher_session($username, $playerIp);

    if ($verifiedSession) {
        // Kullanıcı detaylarını bul
        $allUsers = get_all_saved_users();
        $foundUser = null;
        foreach ($allUsers as $u) {
            if (!empty($u['minecraft_username']) && strtolower($u['minecraft_username']) === strtolower($username)) {
                $foundUser = $u;
                break;
            }
        }

        echo json_encode([
            'status' => 'success',
            'authenticated' => true,
            'launcher_verified' => true,
            'session_id' => $verifiedSession['session_id'],
            'user' => [
                'username' => $verifiedSession['username'],
                'email' => $verifiedSession['email'] ?: ($foundUser['email'] ?? ''),
                'full_name' => $foundUser['full_name'] ?? $verifiedSession['username'],
                'avatar' => 'https://mc-heads.net/avatar/' . urlencode($verifiedSession['username']) . '/100'
            ]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }

    // Doğrulanmadı - Kullanıcı web sitesinde kayıtlı mı kontrol et
    $allUsers = get_all_saved_users();
    $isRegistered = false;
    foreach ($allUsers as $u) {
        if (!empty($u['minecraft_username']) && strtolower($u['minecraft_username']) === strtolower($username)) {
            $isRegistered = true;
            break;
        }
    }

    echo json_encode([
        'status' => 'error',
        'authenticated' => false,
        'launcher_verified' => false,
        'registered_on_web' => $isRegistered,
        'message' => 'Aktif Launcher oturumu bulunamadı. Lütfen oyuna resmi GrizmMC Launcher üzerinden giriş yapınız.'
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// ----------------------------------------------------------------------------
// 3. OYUNCU ÇIKIŞ BİLDİRİMİ (player_quit)
// ----------------------------------------------------------------------------
if ($action === 'player_quit') {
    $username = trim($_POST['username'] ?? $inputData['username'] ?? $_GET['username'] ?? '');
    if (!empty($username)) {
        close_launcher_session($username);
    }
    echo json_encode(['status' => 'success', 'message' => 'Oturum kapatıldı.'], JSON_UNESCAPED_UNICODE);
    exit;
}

// ----------------------------------------------------------------------------
// 4. SERVİS DURUMU & SAĞLIK KONTROLÜ (status)
// ----------------------------------------------------------------------------
$allSessions = get_all_sessions();
$now = time();
$activeCount = 0;
foreach ($allSessions as $s) {
    if (isset($s['expires_at']) && $s['expires_at'] >= $now) {
        $activeCount++;
    }
}

echo json_encode([
    'status' => 'success',
    'service' => 'GrizmMC Auth API',
    'version' => '1.0.0',
    'active_launcher_sessions' => $activeCount,
    'server_time' => date('Y-m-d H:i:s')
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
