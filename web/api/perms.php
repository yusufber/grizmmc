<?php
/**
 * GrizmMC Network - GrizmPerms Web API
 *
 * 1) Minecraft Sunucu Senkronizasyon Uç Noktaları (X-Server-Key doğrulamalı)
 * 2) Oyuncu Mağaza VIP Satın Alım & Kupon Kullanım Uç Noktaları (Oturum doğrulamalı)
 * 3) Ödeme Sağlayıcı Webhook Uç Noktası (Shopier / PayTR / İyzico uyumlu)
 * 4) Yönetim Paneli Rol, İzin, Bakiye ve Kupon CRUD Uç Noktaları (Staff + PIN doğrulamalı)
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Server-Key, X-Webhook-Secret, X-Webhook-Signature, X-Requested-With, X-CSRF-Token');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config.php';

$rawInput = file_get_contents('php://input');
$inputData = [];
if (!empty($rawInput)) {
    $decoded = json_decode($rawInput, true);
    if (is_array($decoded)) {
        $inputData = $decoded;
    }
}

$action = trim($_GET['action'] ?? $_POST['action'] ?? $inputData['action'] ?? 'status');

function gperm_json_out($payload, $code = 200) {
    http_response_code($code);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function gperm_require_server_key($inputData) {
    $key = trim($_SERVER['HTTP_X_SERVER_KEY'] ?? $_POST['server_key'] ?? $inputData['server_key'] ?? $_GET['server_key'] ?? '');
    if (!defined('SERVER_API_KEY') || empty(SERVER_API_KEY) || !hash_equals(SERVER_API_KEY, $key)) {
        gperm_json_out(['status' => 'error', 'message' => 'Yetkisiz sunucu anahtarı (X-Server-Key).'], 403);
    }
}

function gperm_require_admin_unlocked() {
    if (!is_staff_user()) {
        gperm_json_out(['status' => 'error', 'message' => 'Yetkili oturumu bulunamadı.'], 403);
    }
    if (empty($_SESSION['server_panel_unlocked'])) {
        gperm_json_out(['status' => 'error', 'code' => 'pin_locked', 'message' => 'Yönetici PIN kilidi açık değil.'], 403);
    }
}

// ============================================================================
// BÖLÜM 1: MINECRAFT SUNUCUSU SENKRONİZASYON UÇ NOKTALARI (X-Server-Key)
// ============================================================================

if ($action === 'sync_all') {
    gperm_require_server_key($inputData);

    $sinceRev = intval($_GET['since_revision'] ?? $inputData['since_revision'] ?? 0);
    $users = gperm_get_all_user_perms(true);
    $roles = gperm_get_roles();
    $meta = gperm_get_sync_meta();
    $currentRev = intval($meta['revision'] ?? 1);

    if ($sinceRev > 0 && $sinceRev === $currentRev) {
        gperm_json_out([
            'status' => 'up_to_date',
            'revision' => $currentRev,
            'server_time' => time()
        ]);
    }

    gperm_json_out([
        'status' => 'success',
        'revision' => $currentRev,
        'server_time' => time(),
        'default_role_id' => gperm_get_default_role_id(),
        'roles' => $roles,
        'users' => $users
    ]);
}

if ($action === 'get_player') {
    gperm_require_server_key($inputData);
    $username = trim($_GET['username'] ?? $_POST['username'] ?? $inputData['username'] ?? '');
    if ($username === '') {
        gperm_json_out(['status' => 'error', 'message' => 'Oyuncu adı gerekli.'], 400);
    }

    $user = gperm_get_user($username);
    $meta = gperm_get_sync_meta();

    gperm_json_out([
        'status' => 'success',
        'revision' => intval($meta['revision'] ?? 1),
        'server_time' => time(),
        'user' => $user
    ]);
}

if ($action === 'ack_delivery') {
    gperm_require_server_key($inputData);
    $username = trim($inputData['username'] ?? $_POST['username'] ?? '');
    $deliveryId = trim($inputData['delivery_id'] ?? $_POST['delivery_id'] ?? '');
    $serverId = trim($inputData['server_id'] ?? $_POST['server_id'] ?? 'unknown');
    $celebrated = isset($inputData['celebrated']) ? (bool)$inputData['celebrated'] : true;

    if ($username === '' || $deliveryId === '') {
        gperm_json_out(['status' => 'error', 'message' => 'Eksik parametre (username / delivery_id).'], 400);
    }

    $res = gperm_ack_delivery($username, $deliveryId, $serverId, $celebrated);
    $meta = gperm_get_sync_meta();

    gperm_json_out([
        'status' => 'success',
        'changed' => $res['changed'],
        'revision' => intval($meta['revision'] ?? 1)
    ]);
}

if ($action === 'game_mutation') {
    gperm_require_server_key($inputData);
    $op = trim($inputData['operation'] ?? '');
    $actor = trim($inputData['actor'] ?? 'CONSOLE');

    switch ($op) {
        case 'grant_role': {
            $username = trim($inputData['username'] ?? '');
            if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
                gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
            }
            $roleId = trim($inputData['role_id'] ?? '');
            $days = intval($inputData['duration_days'] ?? 0);
            $celebrate = !empty($inputData['celebrate']);
            $res = gperm_grant_user_role($username, $roleId, $days, "ingame:$actor", $celebrate);
            if (!$res['success']) gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
            if ($celebrate) {
                gperm_log_order([
                    'username' => $username,
                    'type' => 'ingame_grant',
                    'role_id' => $roleId,
                    'role_name' => $res['role']['name'] ?? $roleId,
                    'amount' => 0,
                    'duration_days' => $days,
                    'provider' => "ingame ($actor)",
                    'note' => 'Oyun içi /gperm komutu ile verildi'
                ]);
            }
            gperm_json_out(['status' => 'success', 'result' => $res]);
        }

        case 'set_role': {
            $username = trim($inputData['username'] ?? '');
            if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
                gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
            }
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $days = intval($inputData['duration_days'] ?? 0);
            $role = gperm_get_role($roleId);
            if (!$role) gperm_json_out(['status' => 'error', 'message' => 'Rol bulunamadı.'], 404);

            $user = gperm_get_user($username);
            $now = time();
            $exp = $days > 0 ? ($now + ($days * 86400)) : 0;
            $user['roles'] = [[
                'role_id' => $role['id'],
                'assigned_at' => $now,
                'expires_at' => $exp,
                'source' => "ingame:$actor"
            ]];
            $rev = gperm_save_user_record($username, $user, "ingame_set_role:$username:$roleId", true);
            gperm_json_out(['status' => 'success', 'revision' => $rev]);
        }

        case 'revoke_role': {
            $username = trim($inputData['username'] ?? '');
            if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
                gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
            }
            $roleId = trim($inputData['role_id'] ?? '');
            $res = gperm_revoke_user_role($username, $roleId, true);
            if (!$res['success']) gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        case 'add_user_perm': {
            $username = trim($inputData['username'] ?? '');
            if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
                gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
            }
            $perm = trim($inputData['permission'] ?? '');
            $serverScope = strtolower(trim($inputData['server'] ?? ''));
            if ($username === '' || $perm === '') gperm_json_out(['status' => 'error', 'message' => 'Eksik parametre.'], 400);

            $user = gperm_get_user($username);
            if ($serverScope !== '' && $serverScope !== 'global') {
                $list = $user['server_permissions'][$serverScope] ?? [];
                if (!in_array($perm, $list, true)) $list[] = $perm;
                $user['server_permissions'][$serverScope] = $list;
            } else {
                if (!in_array($perm, $user['permissions'], true)) {
                    $user['permissions'][] = $perm;
                }
            }
            $rev = gperm_save_user_record($username, $user, "ingame_add_uperm:$username", true);
            gperm_json_out(['status' => 'success', 'revision' => $rev]);
        }

        case 'remove_user_perm': {
            $username = trim($inputData['username'] ?? '');
            if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
                gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
            }
            $perm = trim($inputData['permission'] ?? '');
            $serverScope = strtolower(trim($inputData['server'] ?? ''));
            $user = gperm_get_user($username);
            if ($serverScope !== '' && $serverScope !== 'global') {
                $list = $user['server_permissions'][$serverScope] ?? [];
                $user['server_permissions'][$serverScope] = array_values(array_filter($list, fn($p) => $p !== $perm));
            } else {
                $user['permissions'] = array_values(array_filter($user['permissions'], fn($p) => $p !== $perm));
            }
            $rev = gperm_save_user_record($username, $user, "ingame_rem_uperm:$username", true);
            gperm_json_out(['status' => 'success', 'revision' => $rev]);
        }

        case 'add_role_perm': {
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $perm = trim($inputData['permission'] ?? '');
            $serverScope = strtolower(trim($inputData['server'] ?? ''));
            $role = gperm_get_role($roleId);
            if (!$role) gperm_json_out(['status' => 'error', 'message' => 'Rol bulunamadı.'], 404);

            if ($serverScope !== '' && $serverScope !== 'global') {
                $list = $role['server_permissions'][$serverScope] ?? [];
                if (!in_array($perm, $list, true)) $list[] = $perm;
                $role['server_permissions'][$serverScope] = $list;
            } else {
                if (!in_array($perm, $role['permissions'], true)) {
                    $role['permissions'][] = $perm;
                }
            }
            $res = gperm_save_role($role);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        case 'remove_role_perm': {
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $perm = trim($inputData['permission'] ?? '');
            $serverScope = strtolower(trim($inputData['server'] ?? ''));
            $role = gperm_get_role($roleId);
            if (!$role) gperm_json_out(['status' => 'error', 'message' => 'Rol bulunamadı.'], 404);

            if ($serverScope !== '' && $serverScope !== 'global') {
                $list = $role['server_permissions'][$serverScope] ?? [];
                $role['server_permissions'][$serverScope] = array_values(array_filter($list, fn($p) => $p !== $perm));
            } else {
                $role['permissions'] = array_values(array_filter($role['permissions'], fn($p) => $p !== $perm));
            }
            $res = gperm_save_role($role);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        case 'update_role_meta': {
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $role = gperm_get_role($roleId);
            if (!$role) gperm_json_out(['status' => 'error', 'message' => 'Rol bulunamadı.'], 404);

            if (isset($inputData['prefix'])) $role['prefix'] = strval($inputData['prefix']);
            if (isset($inputData['suffix'])) $role['suffix'] = strval($inputData['suffix']);
            if (isset($inputData['weight'])) $role['weight'] = intval($inputData['weight']);
            if (isset($inputData['chat_color'])) $role['chat_color'] = strval($inputData['chat_color']);
            if (isset($inputData['add_parent'])) {
                $p = strtolower(trim($inputData['add_parent']));
                if ($p !== '' && $p !== $roleId && !in_array($p, $role['inheritance'], true)) {
                    $role['inheritance'][] = $p;
                }
            }
            if (isset($inputData['remove_parent'])) {
                $p = strtolower(trim($inputData['remove_parent']));
                $role['inheritance'] = array_values(array_filter($role['inheritance'], fn($x) => $x !== $p));
            }
            $res = gperm_save_role($role);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        case 'create_role': {
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $name = trim($inputData['name'] ?? strtoupper($roleId));
            $weight = intval($inputData['weight'] ?? 50);
            if (gperm_get_role($roleId)) {
                gperm_json_out(['status' => 'error', 'message' => 'Bu ID ile bir rol zaten mevcut.'], 400);
            }
            $res = gperm_save_role([
                'id' => $roleId,
                'name' => $name,
                'weight' => $weight,
                'prefix' => "&7[{$name}] &f",
                'inheritance' => ['default'],
                'permissions' => []
            ]);
            if (!$res['success']) gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        case 'delete_role': {
            $roleId = strtolower(trim($inputData['role_id'] ?? ''));
            $res = gperm_delete_role($roleId);
            if (!$res['success']) gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
            gperm_json_out(['status' => 'success', 'revision' => $res['revision']]);
        }

        default:
            gperm_json_out(['status' => 'error', 'message' => "Bilinmeyen işlem: $op"], 400);
    }
}

// ============================================================================
// BÖLÜM 2: OYUNCU MAĞAZA & CÜZDAN UÇ NOKTALARI (Web Oturum Doğrulamalı)
// ============================================================================

if ($action === 'my_status') {
    if (!is_logged_in()) {
        gperm_json_out(['status' => 'guest', 'logged_in' => false]);
    }
    $username = get_auth_username();
    $userRecord = gperm_get_user($username);
    $primary = gperm_get_user_primary_role($username);
    $activeVip = gperm_get_user_active_vip($username);

    gperm_json_out([
        'status' => 'success',
        'logged_in' => true,
        'username' => $username,
        'credits' => $userRecord['credits'] ?? 0.0,
        'primary_role' => $primary['role'],
        'active_vip' => $activeVip,
        'active_vip_remaining_text' => $activeVip ? gperm_format_remaining($activeVip['expires_at']) : null
    ]);
}

if ($action === 'buy_vip') {
    if (!is_logged_in()) {
        gperm_json_out([
            'status' => 'error',
            'code' => 'auth_required',
            'message' => 'VIP satın almak için önce GrizmID hesabınızla giriş yapmalısınız.'
        ], 401);
    }
    verify_csrf_token();

    $buyerUsername = get_auth_username();
    $targetUsername = trim($inputData['target_username'] ?? $_POST['target_username'] ?? $buyerUsername);
    if ($targetUsername === '') {
        $targetUsername = $buyerUsername;
    }
    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $targetUsername)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
    }

    $roleId = strtolower(trim($inputData['role_id'] ?? $_POST['role_id'] ?? ''));
    $role = gperm_get_role($roleId);
    if (!$role || empty($role['is_vip'])) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçerli bir VIP paketi seçilmedi.'], 400);
    }

    $isSelf = (strtolower($buyerUsername) === strtolower($targetUsername));
    if ($isSelf) {
        $res = gperm_purchase_vip_with_wallet($buyerUsername, $roleId);
        if (!$res['success']) {
            gperm_json_out([
                'status' => 'error',
                'code' => $res['code'] ?? 'purchase_failed',
                'credits' => $res['credits'] ?? 0,
                'message' => $res['message']
            ], 400);
        }
        gperm_json_out([
            'status' => 'success',
            'message' => $res['message'],
            'credits' => $res['credits'],
            'expires_at' => $res['expires_at'],
            'remaining_text' => gperm_format_remaining($res['expires_at']),
            'order' => $res['order']
        ]);
    } else {
        // Başka bir oyuncuya kendi bakiyesiyle hediye VIP gönderme (atomik kilit altında)
        $giftResult = gperm_with_lock(function () use ($buyerUsername, $targetUsername, $role) {
            $price = round(floatval($role['vip_price'] ?? 0), 2);
            $days = intval($role['vip_duration_days'] ?? 30);
            if ($price <= 0) {
                return [
                    'success' => false,
                    'message' => 'Bu paket şu anda mağaza satışına veya hediye gönderimine kapalıdır.'
                ];
            }
            $buyer = gperm_get_user($buyerUsername);
            $currentCredits = round(floatval($buyer['credits'] ?? 0), 2);
            if ($currentCredits < $price) {
                $missing = number_format($price - $currentCredits, 2, '.', '');
                return [
                    'success' => false,
                    'code' => 'insufficient_balance',
                    'credits' => $currentCredits,
                    'message' => "Yetersiz bakiye! Mevcut bakiyeniz: {$currentCredits} ₺ (Eksik: {$missing} ₺)."
                ];
            }

            $buyer['credits'] = round($currentCredits - $price, 2);
            gperm_save_user_record($buyerUsername, $buyer, "gift_debit:$buyerUsername:$price", false);

            $grant = gperm_grant_user_role($targetUsername, $role['id'], $days, "gift_from:$buyerUsername", true);
            if (!$grant['success']) {
                gperm_adjust_user_credits($buyerUsername, $price, 'gift_refund');
                return ['success' => false, 'message' => $grant['message']];
            }

            $order = gperm_log_order([
                'username' => $targetUsername,
                'email' => $buyer['email'] ?? '',
                'type' => 'vip_gift',
                'role_id' => $role['id'],
                'role_name' => $role['name'],
                'amount' => $price,
                'duration_days' => $days,
                'provider' => 'wallet_gift',
                'note' => "{$buyerUsername} tarafından {$targetUsername} oyuncusuna hediye edildi"
            ]);

            $updatedBuyer = gperm_get_user($buyerUsername);
            return [
                'success' => true,
                'message' => "{$role['name']} paketi {$targetUsername} adlı oyuncuya başarıyla hediye edildi ve oyunda aktifleşti!",
                'credits' => $updatedBuyer['credits'] ?? $buyer['credits'],
                'expires_at' => $grant['expires_at'],
                'remaining_text' => gperm_format_remaining($grant['expires_at']),
                'order' => $order
            ];
        });

        if (!$giftResult['success']) {
            gperm_json_out([
                'status' => 'error',
                'code' => $giftResult['code'] ?? 'purchase_failed',
                'credits' => $giftResult['credits'] ?? 0,
                'message' => $giftResult['message']
            ], 400);
        }

        gperm_json_out([
            'status' => 'success',
            'message' => $giftResult['message'],
            'credits' => $giftResult['credits'],
            'expires_at' => $giftResult['expires_at'],
            'remaining_text' => $giftResult['remaining_text'],
            'order' => $giftResult['order']
        ]);
    }
}

if ($action === 'buy_crate_key') {
    if (!is_logged_in()) {
        gperm_json_out([
            'status' => 'error',
            'code' => 'auth_required',
            'message' => 'Kasa anahtarı satın almak için önce GrizmID hesabınızla giriş yapmalısınız.'
        ], 401);
    }
    verify_csrf_token();

    $buyerUsername = get_auth_username();
    $targetUsername = trim($inputData['target_username'] ?? $_POST['target_username'] ?? $buyerUsername);
    if ($targetUsername === '') {
        $targetUsername = $buyerUsername;
    }
    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $targetUsername)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
    }

    $tier = strtolower(trim($inputData['tier'] ?? $_POST['tier'] ?? ''));
    $amount = max(1, intval($inputData['amount'] ?? $_POST['amount'] ?? 1));

    $res = gperm_purchase_crate_keys_with_wallet($buyerUsername, $targetUsername, $tier, $amount);
    if (!$res['success']) {
        gperm_json_out([
            'status' => 'error',
            'code' => $res['code'] ?? 'purchase_failed',
            'credits' => $res['credits'] ?? 0,
            'message' => $res['message']
        ], 400);
    }

    gperm_json_out([
        'status' => 'success',
        'message' => $res['message'],
        'credits' => $res['credits'],
        'order' => $res['order'],
        'tier' => $res['tier'],
        'amount' => $res['amount'],
        'total_price' => $res['total_price'],
        'delivery_id' => $res['delivery_id']
    ]);
}

if ($action === 'redeem_coupon') {
    if (!is_logged_in()) {
        gperm_json_out(['status' => 'error', 'code' => 'auth_required', 'message' => 'Kupon kullanmak için önce giriş yapmalısınız.'], 401);
    }
    verify_csrf_token();

    $username = get_auth_username();
    $code = trim($inputData['code'] ?? $_POST['code'] ?? '');
    $res = gperm_redeem_coupon($username, $code);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }

    gperm_json_out([
        'status' => 'success',
        'type' => $res['type'],
        'credits' => $res['credits'],
        'message' => $res['message']
    ]);
}

// ============================================================================
// BÖLÜM 3: ÖDEME SAĞLAYICI WEBHOOK UÇ NOKTASI (Shopier / PayTR / İyzico)
// ============================================================================

if ($action === 'payment_webhook') {
    $providedSecret = trim($_SERVER['HTTP_X_WEBHOOK_SECRET'] ?? $_POST['secret'] ?? $inputData['secret'] ?? '');
    $providedSig = trim($_SERVER['HTTP_X_WEBHOOK_SIGNATURE'] ?? '');
    $expectedSig = hash_hmac('sha256', $rawInput, PAYMENT_WEBHOOK_SECRET);

    $authorized = false;
    if ($providedSecret !== '' && hash_equals(PAYMENT_WEBHOOK_SECRET, $providedSecret)) {
        $authorized = true;
    } elseif ($providedSig !== '' && hash_equals($expectedSig, $providedSig)) {
        $authorized = true;
    }

    if (!$authorized) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçersiz webhook imzası veya gizli anahtar.'], 403);
    }

    $extOrderId = trim($inputData['order_id'] ?? $_POST['order_id'] ?? '');
    $username = trim($inputData['username'] ?? $_POST['username'] ?? '');
    $mode = strtolower(trim($inputData['mode'] ?? $_POST['mode'] ?? 'credit'));
    $provider = trim($inputData['provider'] ?? $_POST['provider'] ?? 'payment_gateway');
    $amount = round(floatval($inputData['amount'] ?? $_POST['amount'] ?? 0), 2);

    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'], 400);
    }

    // Mükerrer (idempotency) sipariş kontrolü
    if ($extOrderId !== '') {
        foreach (gperm_get_orders() as $existingOrder) {
            if (($existingOrder['order_id'] ?? '') === $extOrderId) {
                gperm_json_out(['status' => 'already_processed', 'order_id' => $extOrderId]);
            }
        }
    }

    if ($mode === 'vip') {
        $roleId = strtolower(trim($inputData['role_id'] ?? $_POST['role_id'] ?? ''));
        $role = gperm_get_role($roleId);
        if (!$role) {
            gperm_json_out(['status' => 'error', 'message' => "Rol bulunamadı: $roleId"], 400);
        }
        $days = intval($inputData['days'] ?? $_POST['days'] ?? ($role['vip_duration_days'] ?? 30));
        $grant = gperm_grant_user_role($username, $role['id'], $days, "webhook:$provider", true);
        $order = gperm_log_order([
            'order_id' => $extOrderId ?: ('WH-' . strtoupper(bin2hex(random_bytes(5)))),
            'username' => $username,
            'email' => gperm_find_email_for_username($username),
            'type' => 'webhook_vip',
            'role_id' => $role['id'],
            'role_name' => $role['name'],
            'amount' => $amount > 0 ? $amount : floatval($role['vip_price'] ?? 0),
            'duration_days' => $days,
            'provider' => $provider,
            'note' => "Otomatik Ödeme Webhook ({$provider})"
        ]);
        gperm_json_out(['status' => 'success', 'mode' => 'vip', 'order' => $order]);
    } else {
        if ($amount <= 0) {
            gperm_json_out(['status' => 'error', 'message' => 'Yüklenecek tutar 0\'dan büyük olmalıdır.'], 400);
        }
        $adj = gperm_adjust_user_credits($username, $amount, "webhook_topup:$provider");
        $order = gperm_log_order([
            'order_id' => $extOrderId ?: ('WH-' . strtoupper(bin2hex(random_bytes(5)))),
            'username' => $username,
            'email' => gperm_find_email_for_username($username),
            'type' => 'credit_topup',
            'role_id' => '',
            'role_name' => 'Bakiye Yükleme',
            'amount' => $amount,
            'duration_days' => 0,
            'provider' => $provider,
            'note' => "Otomatik Bakiye Yükleme ({$provider})"
        ]);
        gperm_json_out(['status' => 'success', 'mode' => 'credit', 'credits' => $adj['credits'], 'order' => $order]);
    }
}

// ============================================================================
// BÖLÜM 4: YÖNETİCİ PANELİ UÇ NOKTALARI (Staff + PIN Kilidi)
// ============================================================================

if ($action === 'unlock_panel') {
    if (!is_staff_user()) {
        gperm_json_out(['status' => 'error', 'message' => 'Yetkili erişimi gerekli.'], 403);
    }
    verify_csrf_token();
    $pin = trim($inputData['pin'] ?? $_POST['pin'] ?? '');
    if (defined('ADMIN_PANEL_PIN') && hash_equals(ADMIN_PANEL_PIN, $pin)) {
        $_SESSION['server_panel_unlocked'] = true;
        gperm_json_out(['status' => 'success', 'message' => 'Yönetim kilidi açıldı.']);
    }
    gperm_json_out(['status' => 'error', 'message' => 'Hatalı yönetici PIN kodu!'], 403);
}

if ($action === 'lock_panel') {
    verify_csrf_token();
    $_SESSION['server_panel_unlocked'] = false;
    gperm_json_out(['status' => 'success']);
}

if ($action === 'admin_overview') {
    gperm_require_admin_unlocked();
    $roles = gperm_get_roles();
    $users = gperm_get_all_user_perms(true);
    $coupons = gperm_get_coupons();
    $orders = gperm_get_orders();
    $meta = gperm_get_sync_meta();

    // Web'de kayıtlı ama henüz user_perms.json'da kaydı olmayan oyuncuları da listeye dahil et
    $webUsers = function_exists('get_all_saved_users') ? get_all_saved_users() : [];
    foreach ($webUsers as $email => $wu) {
        $mcName = trim($wu['minecraft_username'] ?? '');
        if ($mcName === '') continue;
        $uKey = strtolower($mcName);
        if (!isset($users[$uKey])) {
            $users[$uKey] = [
                'username' => $mcName,
                'email' => $wu['email'] ?? $email,
                'credits' => 0.0,
                'roles' => [],
                'permissions' => [],
                'server_permissions' => [],
                'pending_deliveries' => [],
                'updated_at' => intval($wu['updated_at'] ?? time())
            ];
        }
    }

    gperm_json_out([
        'status' => 'success',
        'revision' => intval($meta['revision'] ?? 1),
        'updated_at' => intval($meta['updated_at'] ?? time()),
        'roles' => array_values($roles),
        'users' => array_values($users),
        'coupons' => array_values($coupons),
        'orders' => array_slice($orders, 0, 100)
    ]);
}

if ($action === 'admin_save_role') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $res = gperm_save_role($inputData);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }
    gperm_json_out(['status' => 'success', 'role' => $res['role'], 'revision' => $res['revision'], 'message' => 'Rol ve izin ayarları kaydedildi ve sunuculara senkronize edildi.']);
}

if ($action === 'admin_delete_role') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $roleId = trim($inputData['id'] ?? $_POST['id'] ?? '');
    $res = gperm_delete_role($roleId);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }
    gperm_json_out(['status' => 'success', 'revision' => $res['revision'], 'message' => 'Rol silindi.']);
}

if ($action === 'admin_grant_role') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $username = trim($inputData['username'] ?? '');
    $roleId = trim($inputData['role_id'] ?? '');
    $days = intval($inputData['duration_days'] ?? 0);
    $celebrate = !empty($inputData['celebrate']);

    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçerli bir Minecraft oyuncu adı girin (3-16 karakter).'], 400);
    }

    $adminName = get_auth_username() ?: 'Admin';
    $res = gperm_grant_user_role($username, $roleId, $days, "web_admin:$adminName", $celebrate);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }

    gperm_log_order([
        'username' => $username,
        'email' => gperm_find_email_for_username($username),
        'type' => 'admin_grant',
        'role_id' => $res['role']['id'],
        'role_name' => $res['role']['name'],
        'amount' => 0,
        'duration_days' => $days,
        'provider' => "Panel ($adminName)",
        'note' => ($days > 0 ? "{$days} Gün" : 'Kalıcı') . ($celebrate ? ' + Oyun İçi Kutlama/Ödül' : '')
    ]);

    gperm_json_out([
        'status' => 'success',
        'message' => "{$username} oyuncusuna {$res['role']['name']} yetkisi tanımlandı!",
        'revision' => $res['revision']
    ]);
}

if ($action === 'admin_revoke_role') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $username = trim($inputData['username'] ?? '');
    $roleId = trim($inputData['role_id'] ?? '');
    $res = gperm_revoke_user_role($username, $roleId, true);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }
    gperm_json_out(['status' => 'success', 'message' => 'Rol oyuncudan kaldırıldı.', 'revision' => $res['revision']]);
}

if ($action === 'admin_save_user_perms') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $username = trim($inputData['username'] ?? '');
    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçerli bir Minecraft oyuncu adı girin (3-16 karakter).'], 400);
    }
    $user = gperm_get_user($username);
    $rawPerms = $inputData['permissions'] ?? [];
    if (!is_array($rawPerms)) {
        $rawPerms = preg_split('/[\r\n,]+/', strval($rawPerms));
    }
    $user['permissions'] = array_values(array_unique(array_filter(array_map('trim', $rawPerms))));

    if (isset($inputData['server_permissions']) && is_array($inputData['server_permissions'])) {
        $cleanSrv = [];
        foreach ($inputData['server_permissions'] as $srv => $list) {
            $srv = strtolower(trim($srv));
            $arr = is_array($list) ? $list : preg_split('/[\r\n,]+/', strval($list));
            $filtered = array_values(array_unique(array_filter(array_map('trim', $arr))));
            if (!empty($filtered)) {
                $cleanSrv[$srv] = $filtered;
            }
        }
        $user['server_permissions'] = $cleanSrv;
    }

    $rev = gperm_save_user_record($username, $user, "admin_user_perms:$username", true);
    gperm_json_out(['status' => 'success', 'message' => 'Oyuncu özel izinleri güncellendi.', 'revision' => $rev]);
}

if ($action === 'admin_adjust_credits') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $username = trim($inputData['username'] ?? '');
    $mode = $inputData['mode'] ?? 'add'; // add | set
    $amount = round(floatval($inputData['amount'] ?? 0), 2);
    $adminName = get_auth_username() ?: 'Admin';

    if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $username)) {
        gperm_json_out(['status' => 'error', 'message' => 'Geçerli bir Minecraft oyuncu adı girin.'], 400);
    }

    if ($mode === 'set') {
        $res = gperm_set_user_credits($username, $amount, "admin_set_credit:$username");
    } else {
        $res = gperm_adjust_user_credits($username, $amount, "admin_add_credit:$username");
    }

    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }

    gperm_log_order([
        'username' => $username,
        'email' => gperm_find_email_for_username($username),
        'type' => 'admin_credit',
        'role_id' => '',
        'role_name' => 'Cüzdan İşlemi',
        'amount' => $amount,
        'duration_days' => 0,
        'provider' => "Panel ($adminName)",
        'note' => ($mode === 'set' ? "Bakiye sabitlendi: {$res['credits']} ₺" : "Bakiye değişimi: " . ($amount >= 0 ? "+$amount" : "$amount") . " ₺")
    ]);

    gperm_json_out([
        'status' => 'success',
        'credits' => $res['credits'],
        'message' => "{$username} yeni cüzdan bakiyesi: {$res['credits']} ₺"
    ]);
}

if ($action === 'admin_create_coupon') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $res = gperm_create_coupon($inputData);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }
    gperm_json_out(['status' => 'success', 'coupon' => $res['coupon'], 'message' => 'Kupon kodu oluşturuldu.']);
}

if ($action === 'admin_delete_coupon') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $code = trim($inputData['code'] ?? '');
    $res = gperm_delete_coupon($code);
    if (!$res['success']) {
        gperm_json_out(['status' => 'error', 'message' => $res['message']], 400);
    }
    gperm_json_out(['status' => 'success', 'message' => 'Kupon silindi.']);
}

if ($action === 'admin_force_sync') {
    gperm_require_admin_unlocked();
    verify_csrf_token();
    $rev = gperm_bump_revision('admin_force_sync');
    gperm_notify_servers_sync();
    gperm_json_out(['status' => 'success', 'revision' => $rev, 'message' => 'Tüm açık sunuculara anlık senkronizasyon sinyali gönderildi!']);
}

// Varsayılan sağlık durumu
$meta = gperm_get_sync_meta();
gperm_json_out([
    'status' => 'success',
    'service' => 'GrizmPerms Web API',
    'version' => '1.0.0',
    'revision' => intval($meta['revision'] ?? 1),
    'server_time' => date('Y-m-d H:i:s')
]);

