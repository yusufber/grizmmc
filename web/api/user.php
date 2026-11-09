<?php
/**
 * GrizmMC Network - User Profile API
 * Launcher ve harici servisler için kullanıcı bilgisi sorgulama servisi
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config.php';

$email = trim($_GET['email'] ?? $_POST['email'] ?? '');
$username = trim($_GET['username'] ?? $_POST['username'] ?? '');

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
    echo json_encode([
        'status' => 'success',
        'user' => [
            'username' => $ign,
            'full_name' => $foundUser['full_name'] ?? $ign,
            'email' => $foundUser['email'] ?? '',
            'avatar' => 'https://mc-heads.net/avatar/' . urlencode($ign) . '/100'
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

http_response_code(404);
echo json_encode(['status' => 'error', 'message' => 'Kullanıcı bulunamadı.'], JSON_UNESCAPED_UNICODE);
