<?php
/**
 * GrizmMC Network - Sunucu Yönetim API Uç Noktası
 * Sunucu durumları, başlatma, durdurma, yeniden başlatma, log alma ve RCON komut çalıştırma.
 */

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/server_manager.php';

// 1. Yetki Kontrolü: Kullanıcı giriş yapmış ve yetkili mi?
if (!is_staff_user()) {
    http_response_code(403);
    echo json_encode([
        'success' => false,
        'message' => 'Yetkisiz erişim! Bu paneli kullanmak için yetkili hesabı gereklidir.'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 2. PIN Doğrulama İsteği Kontrolü
$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($action === 'verify_pin') {
    verify_csrf_token();
    $pin = trim($_POST['pin'] ?? '');
    $correctPin = defined('ADMIN_PANEL_PIN') ? ADMIN_PANEL_PIN : 'Grizm2026!';
    if (hash_equals($correctPin, $pin)) {
        $_SESSION['server_panel_unlocked'] = true;
        echo json_encode([
            'success' => true,
            'message' => 'PIN doğrulandı, sunucu kontrollerine erişim açıldı.'
        ], JSON_UNESCAPED_UNICODE);
    } else {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Hatalı PIN kodu! Lütfen tekrar deneyin.'
        ], JSON_UNESCAPED_UNICODE);
    }
    exit;
}

// 3. Kilit Kontrolü: PIN girilmemişse işlem yaptırma
if (empty($_SESSION['server_panel_unlocked'])) {
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'locked' => true,
        'message' => 'Panel kilitli. Lütfen güvenlik PIN kodunu giriniz.'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// Paneli Yeniden Kilitleme
if ($action === 'lock_panel') {
    verify_csrf_token();
    unset($_SESSION['server_panel_unlocked']);
    echo json_encode(['success' => true, 'message' => 'Panel kilitlendi.'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 4. Eylemlerin Yürütülmesi
$serverKey = $_GET['server'] ?? $_POST['server'] ?? '';

switch ($action) {
    case 'status':
        $data = ServerManager::getAllStatuses();
        echo json_encode(['success' => true, 'data' => $data], JSON_UNESCAPED_UNICODE);
        break;

    case 'start':
        verify_csrf_token();
        $res = ServerManager::startServer($serverKey);
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'stop':
        verify_csrf_token();
        $res = ServerManager::stopServer($serverKey);
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'restart':
        verify_csrf_token();
        $res = ServerManager::restartServer($serverKey);
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'start_all':
        verify_csrf_token();
        $res = ServerManager::startAll();
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'stop_all':
        verify_csrf_token();
        $res = ServerManager::stopAll();
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'logs':
        $lines = isset($_GET['lines']) ? (int)$_GET['lines'] : 70;
        $res = ServerManager::getLatestLogs($serverKey, $lines);
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    case 'command':
        verify_csrf_token();
        $command = $_POST['cmd'] ?? '';
        $res = ServerManager::sendCommand($serverKey, $command);
        echo json_encode($res, JSON_UNESCAPED_UNICODE);
        break;

    default:
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Geçersiz API eylemi.'], JSON_UNESCAPED_UNICODE);
        break;
}

