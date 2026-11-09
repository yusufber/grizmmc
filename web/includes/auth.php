<?php
/**
 * GrizmMC Network - Kimlik Doğrulama & Yetkilendirme Modülü
 * GrizmID OAuth2 oturum kontrolü, yetkili izinleri ve kullanıcı adı zorunluluk denetimi.
 */

// ============================================================================
// YETKİLİ & YÖNETİCİ KONTROL AYARLARI
// ============================================================================
if (!defined('ADMIN_EMAILS')) {
    define('ADMIN_EMAILS', ['admin@grizm.tech', 'destek@grizm.tech']);
}
if (!defined('ADMIN_USERNAMES')) {
    define('ADMIN_USERNAMES', ['Grizm', 'Admin', 'Kurucu', 'GrizmMC']);
}

// ============================================================================
// TEMEL OTURUM VE KULLANICI FONKSİYONLARI
// ============================================================================

function is_logged_in() {
    return isset($_SESSION['auth_user']);
}

function get_auth_user() {
    return $_SESSION['auth_user'] ?? null;
}

function get_auth_username() {
    if (!is_logged_in()) return null;
    $user = get_auth_user();
    return !empty($user['minecraft_username']) ? $user['minecraft_username'] : ($user['full_name'] ?? null);
}

function get_oauth_login_url() {
    if (empty($_SESSION['oauth_state'])) {
        $_SESSION['oauth_state'] = bin2hex(random_bytes(16));
    }
    return rtrim(AUTH_SERVER_URL, '/') . '/authorize?api_key=' . urlencode(AUTH_CLIENT_ID) . '&state=' . urlencode($_SESSION['oauth_state']);
}

function get_auth_login_url() {
    return get_oauth_login_url();
}

function is_staff_user() {
    if (!is_logged_in()) {
        return false;
    }
    $user = get_auth_user();
    $email = strtolower(trim($user['email'] ?? ''));
    $username = strtolower(trim(get_auth_username() ?? ''));

    if (in_array($email, array_map('strtolower', ADMIN_EMAILS), true)) {
        return true;
    }
    if (in_array($username, array_map('strtolower', ADMIN_USERNAMES), true)) {
        return true;
    }

    global $STAFF_MEMBERS;
    if (!empty($STAFF_MEMBERS) && is_array($STAFF_MEMBERS)) {
        foreach ($STAFF_MEMBERS as $staff) {
            if (isset($staff['username']) && strtolower(trim($staff['username'])) === $username) {
                return true;
            }
        }
    }

    // GrizmPerms üzerinden yüksek yetkili rol kontrolü (owner, admin, mod veya grizmperms.admin)
    if ($username !== '' && function_exists('gperm_get_user_primary_role')) {
        $primary = gperm_get_user_primary_role($username);
        $rid = strtolower($primary['role']['id'] ?? '');
        if (in_array($rid, ['owner', 'admin', 'mod'], true) || intval($primary['role']['weight'] ?? 0) >= 750) {
            return true;
        }
    }
    return false;
}

// ============================================================================
// İLK GİRİŞ KULLANICI ADI KONTROLÜ
// ============================================================================

function check_username_requirement() {
    $currentScript = basename($_SERVER['PHP_SELF'] ?? '');
    if (is_logged_in() && !empty($_SESSION['needs_username']) && $currentScript !== 'set-username.php' && $currentScript !== 'logout.php') {
        header('Location: set-username.php');
        exit;
    }
}
