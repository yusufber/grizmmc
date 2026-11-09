<?php
/**
 * GrizmMC Network - Giriş Yönlendiricisi
 * Kullanıcıyı GrizmID OAuth2 Yetkilendirme sayfasına yönlendirir.
 */

require_once __DIR__ . '/config.php';

if (isset($_GET['source']) && $_GET['source'] === 'launcher') {
    $_SESSION['auth_source'] = 'launcher';
}
if (isset($_GET['port']) && is_numeric($_GET['port'])) {
    $_SESSION['launcher_port'] = (int)$_GET['port'];
}

if (is_logged_in()) {
    if (!empty($_SESSION['auth_source']) && $_SESSION['auth_source'] === 'launcher') {
        $user = get_auth_user();
        if (!empty($_SESSION['needs_username']) || empty($user['minecraft_username'])) {
            header('Location: set-username.php');
            exit;
        }
        $username = $user['minecraft_username'];
        $email = $user['email'] ?? '';
        $fullName = $user['full_name'] ?? $username;
        render_launcher_handoff($username, $email, $fullName);
        exit;
    }
    header('Location: profile.php');
    exit;
}

// GrizmID Authorize sayfasına yönlendir
header('Location: ' . get_auth_login_url());
exit;
