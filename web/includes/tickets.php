<?php
/**
 * GrizmMC Network - Destek Talebi (Ticket) Modülü
 * Kategoriler, durumlar, öncelikler ve CRUD veri işlemleri.
 */

// ============================================================================
// DESTEK TALEBİ KATEGORİLERİ & DURUMLARI
// ============================================================================

$TICKET_CATEGORIES = [
    'bug' => [
        'id' => 'bug',
        'title' => 'Oyun İçi Hata & Bug Bildirimi',
        'short' => 'Hata / Bug',
        'icon' => 'fa-solid fa-bug',
        'badge' => 'BUG',
        'color' => '#ff7b72'
    ],
    'store' => [
        'id' => 'store',
        'title' => 'Mağaza, VIP & Ödeme Sorunu',
        'short' => 'Mağaza & VIP',
        'icon' => 'fa-solid fa-cart-shopping',
        'badge' => 'MAĞAZA',
        'color' => '#58a6ff'
    ],
    'report' => [
        'id' => 'report',
        'title' => 'Hile & Oyuncu Şikayeti (Player Report)',
        'short' => 'Şikayet / Hile',
        'icon' => 'fa-solid fa-shield-halved',
        'badge' => 'ŞİKAYET',
        'color' => '#f85149'
    ],
    'account' => [
        'id' => 'account',
        'title' => 'Hesap & Güvenlik Sorunları',
        'short' => 'Hesap Güvenliği',
        'icon' => 'fa-solid fa-key',
        'badge' => 'GÜVENLİK',
        'color' => '#d2a8ff'
    ],
    'suggestion' => [
        'id' => 'suggestion',
        'title' => 'Öneri & İstek Bildirimi',
        'short' => 'Öneri & İstek',
        'icon' => 'fa-solid fa-lightbulb',
        'badge' => 'ÖNERİ',
        'color' => '#f2cc60'
    ],
    'general' => [
        'id' => 'general',
        'title' => 'Genel Destek & Soru',
        'short' => 'Genel Soru',
        'icon' => 'fa-solid fa-circle-question',
        'badge' => 'GENEL',
        'color' => '#e6edf3'
    ]
];

$TICKET_STATUSES = [
    'open' => [
        'title' => 'Açık (İnceleniyor)',
        'short' => 'Açık',
        'color' => '#58a6ff',
        'badge' => 'status-open',
        'icon' => 'fa-solid fa-circle-dot'
    ],
    'staff_reply' => [
        'title' => 'Yetkili Cevapladı',
        'short' => 'Cevaplandı',
        'color' => '#3fb950',
        'badge' => 'status-staff',
        'icon' => 'fa-solid fa-reply'
    ],
    'user_reply' => [
        'title' => 'Oyuncu Yanıtladı',
        'short' => 'Yanıt Geldi',
        'color' => '#f2cc60',
        'badge' => 'status-user',
        'icon' => 'fa-solid fa-user-clock'
    ],
    'resolved' => [
        'title' => 'Çözüldü',
        'short' => 'Çözüldü',
        'color' => '#56d364',
        'badge' => 'status-resolved',
        'icon' => 'fa-solid fa-circle-check'
    ],
    'closed' => [
        'title' => 'Kapatıldı',
        'short' => 'Kapalı',
        'color' => '#8b949e',
        'badge' => 'status-closed',
        'icon' => 'fa-solid fa-lock'
    ]
];

$TICKET_PRIORITIES = [
    'low' => ['title' => 'Düşük', 'color' => '#8b949e', 'icon' => 'fa-solid fa-arrow-down'],
    'medium' => ['title' => 'Normal', 'color' => '#58a6ff', 'icon' => 'fa-solid fa-minus'],
    'high' => ['title' => 'Yüksek', 'color' => '#f2cc60', 'icon' => 'fa-solid fa-arrow-up'],
    'urgent' => ['title' => 'Acil', 'color' => '#f85149', 'icon' => 'fa-solid fa-triangle-exclamation']
];

// ============================================================================
// VERİ İŞLEMLERİ (TICKETS.JSON)
// ============================================================================

function get_tickets_file_path() {
    return dirname(__DIR__) . '/data/tickets.json';
}

function get_all_tickets() {
    $filePath = get_tickets_file_path();
    if (!file_exists($filePath)) {
        return [];
    }
    $content = @file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function save_tickets_list($tickets) {
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
        @file_put_contents($dir . '/.htaccess', "Deny from all\n");
    }
    @file_put_contents(get_tickets_file_path(), json_encode($tickets, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

function get_ticket_by_id($id) {
    $tickets = get_all_tickets();
    return $tickets[$id] ?? null;
}

function get_user_tickets($email) {
    $tickets = get_all_tickets();
    $result = [];
    $email = strtolower(trim($email));
    foreach ($tickets as $t) {
        if (isset($t['user_email']) && strtolower(trim($t['user_email'])) === $email) {
            $result[$t['id']] = $t;
        }
    }
    uasort($result, function($a, $b) {
        return ($b['updated_at'] ?? 0) <=> ($a['updated_at'] ?? 0);
    });
    return $result;
}

function create_ticket($data) {
    $tickets = get_all_tickets();
    $id = 'TK-' . strtoupper(substr(uniqid(), -5));
    while (isset($tickets[$id])) {
        $id = 'TK-' . strtoupper(substr(uniqid(), -5));
    }

    $time = time();
    $ticket = [
        'id' => $id,
        'title' => trim($data['title']),
        'category' => $data['category'] ?? 'general',
        'priority' => $data['priority'] ?? 'medium',
        'status' => 'open',
        'user_email' => strtolower(trim($data['user_email'])),
        'user_name' => trim($data['user_name']),
        'created_at' => $time,
        'updated_at' => $time,
        'messages' => [
            [
                'id' => 'msg-' . bin2hex(random_bytes(4)),
                'sender_name' => trim($data['user_name']),
                'sender_email' => strtolower(trim($data['user_email'])),
                'sender_role' => 'user',
                'message' => trim($data['message']),
                'created_at' => $time
            ]
        ]
    ];

    $tickets[$id] = $ticket;
    save_tickets_list($tickets);
    return $ticket;
}

function add_ticket_message($ticketId, $senderName, $senderEmail, $senderRole, $message) {
    $tickets = get_all_tickets();
    if (!isset($tickets[$ticketId])) {
        return false;
    }

    $time = time();
    $msg = [
        'id' => 'msg-' . bin2hex(random_bytes(4)),
        'sender_name' => trim($senderName),
        'sender_email' => strtolower(trim($senderEmail)),
        'sender_role' => $senderRole,
        'message' => trim($message),
        'created_at' => $time
    ];

    $tickets[$ticketId]['messages'][] = $msg;
    $tickets[$ticketId]['updated_at'] = $time;

    if ($senderRole === 'staff') {
        $tickets[$ticketId]['status'] = 'staff_reply';
    } else {
        if ($tickets[$ticketId]['status'] !== 'closed') {
            $tickets[$ticketId]['status'] = 'user_reply';
        }
    }

    save_tickets_list($tickets);
    return true;
}

function update_ticket_status($ticketId, $newStatus) {
    $tickets = get_all_tickets();
    if (!isset($tickets[$ticketId])) {
        return false;
    }
    $tickets[$ticketId]['status'] = $newStatus;
    $tickets[$ticketId]['updated_at'] = time();
    save_tickets_list($tickets);
    return true;
}
