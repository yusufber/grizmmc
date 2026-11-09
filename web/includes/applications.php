<?php
/**
 * GrizmMC Network - Yetkili & Kadro Başvuru Modülü
 * Açık pozisyonlar, başvuru durumları ve veri yönetim fonksiyonları.
 */

// ============================================================================
// BAŞVURU POZİSYONLARI & DURUMLARI
// ============================================================================

$APPLICATION_POSITIONS = [
    'rehber' => [
        'id' => 'rehber',
        'title' => 'Sunucu Rehberi',
        'badge' => 'REHBER',
        'icon' => 'fa-solid fa-compass',
        'color' => '#3fb950',
        'summary' => 'Yeni başlayan oyuncuları karşılar, soruları yanıtlar ve sohbet kurallarını korur.',
        'reqs' => [
            'En az 15 yaşında olmak',
            'Günde en az 3 saat aktif olabilmek',
            'Sakin, sabırlı ve düzgün Türkçe iletişimi',
            'Temel komutlar ve oyun kurallarına hakimiyet'
        ]
    ],
    'moderator' => [
        'id' => 'moderator',
        'title' => 'Yetkili Moderatör',
        'badge' => 'MODERATÖR',
        'icon' => 'fa-solid fa-shield-halved',
        'color' => '#58a6ff',
        'summary' => 'Hile, hakaret ve kural ihlallerini tespit eder, ceza uygular ve sunucu düzenini sağlar.',
        'reqs' => [
            'En az 16 yaşında olmak',
            'Günde en az 4 saat aktif olabilmek',
            'Video/kanıt alma ve hile kontrolü yapabilme',
            'Daha önce yetkililik tecrübesine sahip olmak'
        ]
    ],
    'mimar' => [
        'id' => 'mimar',
        'title' => 'Mimar / Builder',
        'badge' => 'MİMAR',
        'icon' => 'fa-solid fa-cubes-stacked',
        'color' => '#f2cc60',
        'summary' => 'Lobi, oyun modları, zindanlar ve sezonluk etkinlikler için haritalar ve yapılar inşa eder.',
        'reqs' => [
            'WorldEdit, FAWE ve VoxelSniper araçlarını bilmek',
            'Kendisine ait yapı portfolyosu sunabilmek',
            'Farklı mimari tarzlara hakim olmak',
            'Takım çalışmasına ve teslim sürelerine uyum'
        ]
    ],
    'developer' => [
        'id' => 'developer',
        'title' => 'Geliştirici (Developer)',
        'badge' => 'DEVELOPER',
        'icon' => 'fa-solid fa-code',
        'color' => '#d2a8ff',
        'summary' => 'Spigot/Paper eklentileri, web entegrasyonu, veritabanı ve performans optimizasyonlarını yürütür.',
        'reqs' => [
            'Java ve Minecraft Server API tecrübesi',
            'Git ve sürüm kontrol araçlarını kullanabilmek',
            'Hata loglarını analiz edebilme ve çözme yeteneği',
            'Referans proje veya GitHub profili'
        ]
    ],
    'youtuber' => [
        'id' => 'youtuber',
        'title' => 'İçerik Üreticisi / Medya',
        'badge' => 'MEDYA',
        'icon' => 'fa-brands fa-youtube',
        'color' => '#ff7b72',
        'summary' => 'Sunucuda videolar, canlı yayınlar veya Shorts/TikTok serileri üreterek topluluğu büyütür.',
        'reqs' => [
            'Düzenli video veya yayın yapan aktif kanal',
            'En az 500 abone veya ortalama 500+ izlenme',
            'GrizmMC Network tanıtımı ve IP paylaşımı',
            'Topluluk kurallarına örnek davranış sergilemek'
        ]
    ]
];

$APPLICATION_STATUSES = [
    'pending' => [
        'title' => 'İnceleniyor',
        'short' => 'İncelemede',
        'color' => '#f2cc60',
        'badge' => 'status-pending',
        'icon' => 'fa-solid fa-hourglass-half'
    ],
    'interview' => [
        'title' => 'Mülakata Çağrıldı',
        'short' => 'Mülakat',
        'color' => '#58a6ff',
        'badge' => 'status-interview',
        'icon' => 'fa-solid fa-comments'
    ],
    'accepted' => [
        'title' => 'Kabul Edildi',
        'short' => 'Kabul',
        'color' => '#3fb950',
        'badge' => 'status-accepted',
        'icon' => 'fa-solid fa-circle-check'
    ],
    'rejected' => [
        'title' => 'Reddedildi',
        'short' => 'Red',
        'color' => '#f85149',
        'badge' => 'status-rejected',
        'icon' => 'fa-solid fa-circle-xmark'
    ]
];

// ============================================================================
// VERİ İŞLEMLERİ (APPLICATIONS.JSON)
// ============================================================================

function get_applications_file_path() {
    return dirname(__DIR__) . '/data/applications.json';
}

function get_all_applications() {
    $filePath = get_applications_file_path();
    if (!file_exists($filePath)) {
        return [];
    }
    $content = @file_get_contents($filePath);
    $data = json_decode($content, true);
    return is_array($data) ? $data : [];
}

function save_applications_list($apps) {
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
        @file_put_contents($dir . '/.htaccess', "Deny from all\n");
    }
    @file_put_contents(get_applications_file_path(), json_encode($apps, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT));
}

function get_application_by_id($id) {
    $apps = get_all_applications();
    return $apps[$id] ?? null;
}

function get_user_applications($email) {
    $apps = get_all_applications();
    $result = [];
    $email = strtolower(trim($email));
    foreach ($apps as $a) {
        if (isset($a['user_email']) && strtolower(trim($a['user_email'])) === $email) {
            $result[$a['id']] = $a;
        }
    }
    uasort($result, function($a, $b) {
        return ($b['created_at'] ?? 0) <=> ($a['created_at'] ?? 0);
    });
    return $result;
}

function create_application($data) {
    $apps = get_all_applications();
    $id = 'BSV-' . strtoupper(substr(uniqid(), -5));
    while (isset($apps[$id])) {
        $id = 'BSV-' . strtoupper(substr(uniqid(), -5));
    }

    $time = time();
    $app = [
        'id' => $id,
        'position' => $data['position'],
        'full_name' => trim($data['full_name']),
        'minecraft_username' => trim($data['minecraft_username']),
        'user_email' => strtolower(trim($data['user_email'])),
        'age' => (int)($data['age'] ?? 0),
        'discord' => trim($data['discord']),
        'play_time' => trim($data['play_time']),
        'experience' => trim($data['experience']),
        'why_us' => trim($data['why_us']),
        'about' => trim($data['about']),
        'status' => 'pending',
        'admin_note' => '',
        'created_at' => $time,
        'updated_at' => $time
    ];

    $apps[$id] = $app;
    save_applications_list($apps);
    return $app;
}

function update_application_status($id, $newStatus, $adminNote = null) {
    $apps = get_all_applications();
    if (!isset($apps[$id])) {
        return false;
    }
    $apps[$id]['status'] = $newStatus;
    if ($adminNote !== null) {
        $apps[$id]['admin_note'] = trim($adminNote);
    }
    $apps[$id]['updated_at'] = time();
    save_applications_list($apps);
    return true;
}
