<?php
/**
 * GrizmMC Network - Veritabanı ve Flat-File JSON Yönetimi
 * Kullanıcı profilleri, Launcher oturumları ve veri dosyası yardımcıları.
 */

// ============================================================================
// KULLANICI PROFİLLERİ (USERS.JSON)
// ============================================================================

function get_users_file_path() {
    return dirname(__DIR__) . '/data/users.json';
}

function get_all_saved_users() {
    $filePath = get_users_file_path();
    if (!file_exists($filePath)) {
        return [];
    }
    $content = @file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function get_user_profile($email) {
    $users = get_all_saved_users();
    $key = strtolower(trim($email));
    return $users[$key] ?? null;
}

function save_user_profile($email, $data) {
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
        @file_put_contents($dir . '/.htaccess', "Deny from all\n");
    }
    $users = get_all_saved_users();
    $key = strtolower(trim($email));
    $users[$key] = array_merge($users[$key] ?? [], $data);
    @file_put_contents(get_users_file_path(), json_encode($users, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

// ============================================================================
// LAUNCHER & SUNUCU OTURUM DOĞRULAMA (SESSIONS.JSON)
// ============================================================================

function get_sessions_file_path() {
    return dirname(__DIR__) . '/data/sessions.json';
}

function get_all_sessions() {
    $filePath = get_sessions_file_path();
    if (!file_exists($filePath)) {
        return [];
    }
    $content = @file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function save_sessions_list($sessions) {
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
        @file_put_contents($dir . '/.htaccess', "Deny from all\n");
    }
    @file_put_contents(get_sessions_file_path(), json_encode($sessions, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

function create_launcher_session($username, $token, $email = '', $ip = '') {
    $sessions = get_all_sessions();
    $now = time();

    // 10 dakikadan eski süresi dolmuş oturumları temizle
    foreach ($sessions as $key => $s) {
        if (isset($s['expires_at']) && $s['expires_at'] < ($now - 600)) {
            unset($sessions[$key]);
        }
    }

    $sessionId = 'SES-' . strtoupper(bin2hex(random_bytes(6)));
    $userKey = strtolower(trim($username));

    $sessionData = [
        'session_id' => $sessionId,
        'username' => trim($username),
        'token' => trim($token),
        'email' => trim($email),
        'client_ip' => trim($ip),
        'created_at' => $now,
        'expires_at' => $now + 300, // 5 dakika geçerli
        'claimed' => false,
        'claimed_at' => null
    ];

    $sessions[$userKey] = $sessionData;
    save_sessions_list($sessions);
    return $sessionData;
}

function verify_launcher_session($username, $ip = '') {
    $sessions = get_all_sessions();
    $userKey = strtolower(trim($username));

    if (!isset($sessions[$userKey])) {
        return null;
    }

    $session = $sessions[$userKey];
    $now = time();

    // Süre kontrolü
    if ($now > ($session['expires_at'] ?? 0)) {
        return null;
    }

    // Başarıyla doğrulandı, claimed olarak güncelle ve oyunda kalma süresince (2 saat) uzat
    $sessions[$userKey]['claimed'] = true;
    $sessions[$userKey]['claimed_at'] = $now;
    $sessions[$userKey]['expires_at'] = $now + 7200;
    save_sessions_list($sessions);

    return $session;
}

function close_launcher_session($username) {
    $sessions = get_all_sessions();
    $userKey = strtolower(trim($username));
    if (isset($sessions[$userKey])) {
        unset($sessions[$userKey]);
        save_sessions_list($sessions);
        return true;
    }
    return false;
}

// ============================================================================
// GENEL YARDIMCI FONKSİYONLAR
// ============================================================================

/**
 * Zaman Farkı Türkçe Yardımcısı (Örn: '5 dk önce', '2 saat önce')
 */
function time_ago_tr($time) {
    $diff = time() - $time;
    if ($diff < 60) {
        return 'Az önce';
    } elseif ($diff < 3600) {
        return floor($diff / 60) . ' dk önce';
    } elseif ($diff < 86400) {
        return floor($diff / 3600) . ' saat önce';
    } elseif ($diff < 86400 * 7) {
        return floor($diff / 86400) . ' gün önce';
    } else {
        return date('d.m.Y H:i', $time);
    }
}
