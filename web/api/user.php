<?php
/**
 * GrizmMC Network - User Profile API
 * Launcher ve harici servisler için kullanıcı bilgisi sorgulama servisi
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Server-Key');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config.php';

$email = trim($_GET['email'] ?? $_POST['email'] ?? '');
$username = trim($_GET['username'] ?? $_POST['username'] ?? '');
$serverKey = trim($_SERVER['HTTP_X_SERVER_KEY'] ?? $_POST['server_key'] ?? $_GET['server_key'] ?? '');
$hasValidServerKey = (defined('SERVER_API_KEY') && !empty(SERVER_API_KEY) && $serverKey !== '' && hash_equals(SERVER_API_KEY, $serverKey));

if (empty($email) && empty($username)) {
    if (is_logged_in()) {
        $authUser = get_auth_user();
        $ign = !empty($authUser['minecraft_username']) ? $authUser['minecraft_username'] : $authUser['full_name'];
        echo json_encode([
            'status' => 'success',
            'user' => [
                'username' => $ign,
                'full_name' => $authUser['full_name'],
                'email' => $authUser['email'],
                'avatar' => 'https://mc-heads.net/avatar/' . urlencode($ign) . '/100'
            ]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }

    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Email veya username parametresi gereklidir.'], JSON_UNESCAPED_UNICODE);
    exit;
}

$allUsers = get_all_saved_users();
$foundUser = null;

if (!empty($email)) {
    $isSelfEmail = (is_logged_in() && strtolower(trim(get_auth_user()['email'] ?? '')) === strtolower($email));
    if (!$hasValidServerKey && !is_staff_user() && !$isSelfEmail) {
        http_response_code(403);
        echo json_encode(['status' => 'error', 'message' => 'Yetkisiz kullanıcı sorgusu.'], JSON_UNESCAPED_UNICODE);
        exit;
    }
    $foundUser = get_user_profile($email);
}

if (!$foundUser && !empty($username)) {
    foreach ($allUsers as $u) {
        if (!empty($u['minecraft_username']) && strtolower($u['minecraft_username']) === strtolower($username)) {
            $foundUser = $u;
            break;
        }
    }
}

if ($foundUser) {
    $ign = !empty($foundUser['minecraft_username']) ? $foundUser['minecraft_username'] : ($foundUser['full_name'] ?? 'Player');
    $isAuthorizedForPii = $hasValidServerKey || is_staff_user();
    if (!$isAuthorizedForPii && is_logged_in()) {
        $authUser = get_auth_user();
        $authEmail = strtolower(trim($authUser['email'] ?? ''));
        $authIgn = strtolower(trim(get_auth_username() ?? ''));
        if (($authEmail !== '' && $authEmail === strtolower(trim($foundUser['email'] ?? ''))) || ($authIgn !== '' && $authIgn === strtolower($ign))) {
            $isAuthorizedForPii = true;
        }
    }

    $userPayload = [
        'username' => $ign,
        'avatar' => 'https://mc-heads.net/avatar/' . urlencode($ign) . '/100'
    ];
    if ($isAuthorizedForPii) {
        $userPayload['full_name'] = $foundUser['full_name'] ?? $ign;
        $userPayload['email'] = $foundUser['email'] ?? '';
    }

    echo json_encode([
        'status' => 'success',
        'user' => $userPayload
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

http_response_code(404);
echo json_encode(['status' => 'error', 'message' => 'Kullanıcı bulunamadı.'], JSON_UNESCAPED_UNICODE);
