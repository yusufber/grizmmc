<?php
/**
 * GrizmMC Network - Merkezi Site İçeriği, Rol, İzin, VIP Mağaza & Cüzdan Motoru
 * Oyun modları, Yetkili kadrosu, Sunucu kuralları ve GrizmPerms Web Mağaza/Yetki çekirdeği tek dosyada birleştirilmiştir.
 */

if (!defined('PAYMENT_WEBHOOK_SECRET')) {
    define('PAYMENT_WEBHOOK_SECRET', getenv('PAYMENT_WEBHOOK_SECRET') ?: 'grizm_pay_whsec_8842a9f1c3e7');
}

// ============================================================================
// OYUN MODLARI LİSTESİ
// ============================================================================
$GAME_MODES = [
    [
        'id' => 'skyblock',
        'name' => 'TekBlock',
        'badge' => 'POPÜLER',
        'version' => '1.21.4',
        'description' => 'Özel adalar, gelişmiş minyon sistemi, borsa & ekonomi, ada yükseltmeleri ile eşsiz bir SkyBlock deneyimi.',
        'features' => ['Özel Minyonlar', 'Ada Seviye Sistemi', 'Dinamik Borsa', 'Özel Görevler', 'Ada Turnuvaları'],
        'icon' => 'fa-solid fa-cube'
    ],
    [
        'id' => 'smp',
        'name' => 'Towny SMP',
        'badge' => 'KLASİK',
        'version' => '1.21.4',
        'description' => 'Kasaba ve krallık yönetimi, ulus savaşları, oyuncu pazarları ve hilesiz adil hayatta kalma macerası.',
        'features' => ['Kasaba & Ulus Sistemi', 'Oyuncu Pazarı (QuickShop)', 'AuraSkills Yetenekler', 'Kuşatma Savaşları', 'Gelişmiş Claim'],
        'icon' => 'fa-solid fa-shield-halved'
    ],
    [
        'id' => 'pvp',
        'name' => 'Competitive PvP',
        'badge' => 'REKABETÇİ',
        'version' => '1.16 - 1.21.4',
        'description' => 'Sıralamalı 1v1 düellolar, FFA arenaları, özel kombolar, elmas ligi ve haftalık ödüllü turnuvalar.',
        'features' => ['1v1 & 2v2 Ranked Düello', 'FFA & Arena', 'Gecikmesiz Hit Algılama', 'Özel Kitler & ELO', 'Haftalık Turnuvalar'],
        'icon' => 'fa-solid fa-crosshairs'
    ],
    [
        'id' => 'lobby',
        'name' => 'Merkez Lobi',
        'badge' => 'HUB',
        'version' => '1.16 - 1.21.4',
        'description' => 'Hızlı sunucu geçişleri, NPC dünyası, sesli sohbet, kozmetikler ve etkinlik alanı.',
        'features' => ['Ultra Düşük Gecikme', 'Özel Kozmetikler', 'Sesli Sohbet (VoiceChat)', 'Hızlı Geçiş NPC\'leri', 'Etkinlik Alanı'],
        'icon' => 'fa-solid fa-compass'
    ]
];

// ============================================================================
// YETKİLİ EKİBİ LİSTESİ
// ============================================================================
$STAFF_MEMBERS = [
    [
        'username' => 'Grizm',
        'role' => 'Kurucu',
        'role_badge' => 'owner',
        'discord' => 'grizm#0001',
        'description' => 'Ağ kurucusu, altyapı mimarı ve sistem yöneticisi.'
    ]
];

// ============================================================================
// SUNUCU KURALLARI
// ============================================================================
$RULES = [
    'genel' => [
        'title' => 'Genel Sunucu Kuralları',
        'items' => [
            ['title' => 'Hile ve Üçüncü Parti Yazılımlar', 'desc' => 'Otomatik tıklayıcı (AutoClicker), X-Ray, Fly, KillAura veya haksız avantaj sağlayan herhangi bir yazılım/mod kullanımı kesinlikle yasaktır.', 'penalty' => 'Süresiz Ban'],
            ['title' => 'Sunucu Açıklarını Kötüye Kullanma (Bug Abuse)', 'desc' => 'Bulunan açıklar yetkililere bildirilmelidir. Açıkları kendi çıkarı için kullanan veya yayan hesaplar sıfırlanır.', 'penalty' => 'Süresiz Ban / Envanter Sıfırlama'],
            ['title' => 'Birden Fazla Hesap (Multi-Account)', 'desc' => 'Ceza sürecini atlatmak veya ekonomik avantaj sağlamak için birden fazla yan hesap açmak yasaktır.', 'penalty' => 'IP & Hesap Banı']
        ]
    ],
    'sohbet' => [
        'title' => 'Sohbet & İletişim Kuralları',
        'items' => [
            ['title' => 'Küfür, Hakaret ve Ailevi Değerler', 'desc' => 'Oyun içi genel sohbette, özel mesajlarda veya klan sohbetlerinde hakaret, küfür ve nefret söylemi yasaktır.', 'penalty' => '1 Saat - 7 Gün Susturma (Mute)'],
            ['title' => 'Reklam ve Tanıtım', 'desc' => 'Başka sunucuların, web sitelerinin, YouTube/Discord kanallarının yetkisiz tanıtımı kesinlikle yasaktır.', 'penalty' => 'Kalıcı Ban'],
            ['title' => 'Spam ve Büyük Harf (Caps-Lock)', 'desc' => 'Sohbeti kirletici ardışık mesajlar, harf uzatmaları ve sürekli büyük harf kullanımı yasaktır.', 'penalty' => '15 Dakika - 2 Saat Susturma']
        ]
    ],
    'ticaret' => [
        'title' => 'Oyun İçi & Ticaret Kuralları',
        'items' => [
            ['title' => 'Gerçek Para ile Ticaret (RMT)', 'desc' => 'Oyun içi eşyaları, paraları veya hesapları gerçek para karşılığında satmak veya takas etmek yasaktır.', 'penalty' => 'Kalıcı Ban'],
            ['title' => 'Dolandırıcılık (Scam)', 'desc' => 'Oyun içi ticaret esnasında hileli yollarla oyuncuları kandırmak ve eşyalarını çalmak yasaktır.', 'penalty' => '14 Gün Ban & Eşya İadesi']
        ]
    ]
];

// ============================================================================
// 1. DOSYA YOLLARI VE ATOMİK JSON OKUMA / YAZMA YARDIMCILARI
// ============================================================================

function gperm_data_dir() {
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
    }
    $htaccess = $dir . '/.htaccess';
    if (!file_exists($htaccess)) {
        @file_put_contents($htaccess, "<IfModule mod_authz_core.c>\n    Require all denied\n</IfModule>\n<IfModule !mod_authz_core.c>\n    Order allow,deny\n    Deny from all\n</IfModule>\n");
    }
    $indexFile = $dir . '/index.php';
    if (!file_exists($indexFile)) {
        @file_put_contents($indexFile, "<?php http_response_code(403); exit;\n");
    }
    return $dir;
}

function gperm_with_lock(callable $fn) {
    static $lockFp = null;
    static $lockDepth = 0;

    if ($lockDepth > 0 && $lockFp !== null) {
        $lockDepth++;
        try {
            return $fn();
        } finally {
            $lockDepth--;
        }
    }

    $lockFile = gperm_data_dir() . '/.gperm.lock';
    $fp = @fopen($lockFile, 'c+');
    if ($fp === false) {
        return $fn();
    }

    @flock($fp, LOCK_EX);
    $lockFp = $fp;
    $lockDepth = 1;
    try {
        return $fn();
    } finally {
        $lockDepth = 0;
        $lockFp = null;
        @flock($fp, LOCK_UN);
        @fclose($fp);
    }
}

function gperm_read_json($filename, $default = []) {
    $path = gperm_data_dir() . '/' . $filename;
    if (!file_exists($path)) {
        return $default;
    }
    $raw = @file_get_contents($path);
    if ($raw === false || $raw === '') {
        return $default;
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : $default;
}

function gperm_write_json($filename, $data) {
    $path = gperm_data_dir() . '/' . $filename;
    $json = json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    return @file_put_contents($path, $json, LOCK_EX) !== false;
}

function gperm_get_sync_meta() {
    $meta = gperm_read_json('sync_meta.json', null);
    if (!$meta || !isset($meta['revision'])) {
        $meta = [
            'revision' => 1,
            'updated_at' => time(),
            'last_reason' => 'initial_seed'
        ];
        gperm_write_json('sync_meta.json', $meta);
    }
    return $meta;
}

function gperm_bump_revision($reason = 'update') {
    $meta = gperm_get_sync_meta();
    $meta['revision'] = intval($meta['revision']) + 1;
    $meta['updated_at'] = time();
    $meta['last_reason'] = $reason;
    gperm_write_json('sync_meta.json', $meta);
    return $meta['revision'];
}

// ============================================================================
// 2. VARSAYILAN ROLLER VE İZİN AĞACI (SEED)
// ============================================================================

function gperm_default_roles_seed() {
    return [
        'owner' => [
            'id' => 'owner',
            'name' => 'Kurucu',
            'weight' => 1000,
            'prefix' => '&4&l[KURUCU] &c',
            'suffix' => ' &4✦',
            'chat_color' => '&f',
            'badge_color' => '#ff4d4d',
            'is_default' => false,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => 'YÖNETİM',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => ['admin'],
            'permissions' => ['*'],
            'server_permissions' => []
        ],
        'admin' => [
            'id' => 'admin',
            'name' => 'Yönetici',
            'weight' => 900,
            'prefix' => '&c&l[YÖNETİCİ] &c',
            'suffix' => '',
            'chat_color' => '&f',
            'badge_color' => '#f87171',
            'is_default' => false,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => 'YÖNETİM',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => ['mod', 'netherite-vip'],
            'permissions' => [
                'essentials.*',
                'worldedit.*',
                'coreprotect.*',
                'grizmauth.admin',
                'grizmauth.bypass',
                'grizmoneblock.admin',
                'grizmoneblock.bypass',
                'grizmtowny.admin',
                'grizmtowny.bypass',
                'grizmperms.admin',
                'tab.admin',
                'multiverse.*'
            ],
            'server_permissions' => []
        ],
        'mod' => [
            'id' => 'mod',
            'name' => 'Moderatör',
            'weight' => 750,
            'prefix' => '&9&l[MOD] &b',
            'suffix' => '',
            'chat_color' => '&f',
            'badge_color' => '#60a5fa',
            'is_default' => false,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => 'YETKİLİ',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => ['helper'],
            'permissions' => [
                'essentials.tempban',
                'essentials.ban',
                'essentials.unban',
                'essentials.unmute',
                'essentials.vanish',
                'essentials.tp',
                'essentials.tphere',
                'essentials.invsee',
                'coreprotect.lookup',
                'coreprotect.rollback',
                'grizmauth.bypass',
                'grizmperms.staff'
            ],
            'server_permissions' => []
        ],
        'helper' => [
            'id' => 'helper',
            'name' => 'Rehber',
            'weight' => 600,
            'prefix' => '&a&l[REHBER] &a',
            'suffix' => '',
            'chat_color' => '&f',
            'badge_color' => '#4ade80',
            'is_default' => false,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => 'REHBER',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => ['default'],
            'permissions' => [
                'essentials.mute',
                'essentials.kick',
                'essentials.helpop.receive',
                'essentials.socialspy',
                'essentials.seen',
                'essentials.chat.color',
                'coreprotect.inspect'
            ],
            'server_permissions' => []
        ],
        'builder' => [
            'id' => 'builder',
            'name' => 'Mimar',
            'weight' => 550,
            'prefix' => '&6&l[MİMAR] &e',
            'suffix' => '',
            'chat_color' => '&f',
            'badge_color' => '#fbbf24',
            'is_default' => false,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => 'MİMAR',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => ['default'],
            'permissions' => [
                'worldedit.*',
                'essentials.gamemode',
                'essentials.gamemode.creative',
                'essentials.gamemode.spectator',
                'essentials.gamemode.survival',
                'essentials.fly',
                'essentials.speed',
                'essentials.chat.color'
            ],
            'server_permissions' => [
                'pvp' => ['-essentials.fly', '-essentials.gamemode']
            ]
        ],
        'netherite-vip' => [
            'id' => 'netherite-vip',
            'name' => 'NETHERITE VIP',
            'weight' => 400,
            'prefix' => '&5&l[NetheriteVIP] &d',
            'suffix' => ' &d✦',
            'chat_color' => '&f',
            'badge_color' => '#c084fc',
            'is_default' => false,
            'is_vip' => true,
            'vip_tier' => 'netherite',
            'vip_price' => 350,
            'vip_duration_days' => 30,
            'vip_badge' => 'EN POPÜLER',
            'vip_highlight' => true,
            'vip_perks' => [
                'Tüm VIP Paketlerinin Tüm Hakları',
                'Sohbette Efsanevi [NetheriteVIP] Ön Eki',
                'Sınırsız /home Belirleme Hakkı',
                'Sınırsız /fly Uçuş Yetkisi (Lobi, TekBlok & SMP)',
                'Eşya Tamiri: /repair all (Sınırsız)',
                'Satın Alımda 100.000 Oyun İçi Ödül Bakiye',
                'Tüm Kozmetikler, /hat ve Renkli /nick',
                'Özel Discord VIP Odası ve Öncelikli Destek'
            ],
            'vip_reward_commands' => [
                ['server' => 'skyblock', 'command' => 'eco give {player} 100000'],
                ['server' => 'smp', 'command' => 'eco give {player} 100000']
            ],
            'inheritance' => ['diamond-vip'],
            'permissions' => [
                'essentials.kits.netheritevip',
                'essentials.sethome.multiple.unlimited',
                'essentials.repair.all',
                'essentials.repair.enchanted',
                'essentials.hat',
                'essentials.nick',
                'essentials.nick.color',
                'grizmperms.vip.netherite'
            ],
            'server_permissions' => [
                'pvp' => ['-essentials.repair.all', '-essentials.repair.enchanted', '-essentials.fly', '-essentials.heal']
            ]
        ],
        'diamond-vip' => [
            'id' => 'diamond-vip',
            'name' => 'DIAMOND VIP',
            'weight' => 300,
            'prefix' => '&b&l[DiamondVIP] &b',
            'suffix' => ' &b❖',
            'chat_color' => '&f',
            'badge_color' => '#38bdf8',
            'is_default' => false,
            'is_vip' => true,
            'vip_tier' => 'diamond',
            'vip_price' => 220,
            'vip_duration_days' => 30,
            'vip_badge' => 'ELİT',
            'vip_highlight' => false,
            'vip_perks' => [
                'Gold VIP’nin Tüm Hakları',
                'Sohbette Özel [DiamondVIP] Ön Eki & Kristal Sembolü',
                '+10 Ekstra Ev (Toplam 13 Home) Belirleme Hakkı',
                '/enderchest Komutu ile Hızlı Sandık Erişimi',
                '/fly Komutu (Lobi, TekBlok & SMP Uçuş)',
                '/heal Komutu (Sağlık ve açlık yenileme)',
                'Satın Alımda 50.000 Oyun İçi Ödül Bakiye',
                'Discord Sunucusunda Diamond VIP Rolü'
            ],
            'vip_reward_commands' => [
                ['server' => 'skyblock', 'command' => 'eco give {player} 50000'],
                ['server' => 'smp', 'command' => 'eco give {player} 50000']
            ],
            'inheritance' => ['gold-vip'],
            'permissions' => [
                'essentials.kits.diamondvip',
                'essentials.sethome.multiple.diamond',
                'essentials.enderchest',
                'essentials.fly',
                'essentials.heal',
                'grizmperms.vip.diamond'
            ],
            'server_permissions' => [
                'pvp' => ['-essentials.fly', '-essentials.heal', '-essentials.enderchest']
            ]
        ],
        'gold-vip' => [
            'id' => 'gold-vip',
            'name' => 'GOLD VIP',
            'weight' => 200,
            'prefix' => '&6&l[GoldVIP] &e',
            'suffix' => ' &e★',
            'chat_color' => '&f',
            'badge_color' => '#facc15',
            'is_default' => false,
            'is_vip' => true,
            'vip_tier' => 'gold',
            'vip_price' => 140,
            'vip_duration_days' => 30,
            'vip_badge' => 'AVANTAJLI',
            'vip_highlight' => false,
            'vip_perks' => [
                'Iron VIP’nin Tüm Hakları',
                'Sohbette Özel [GoldVIP] Ön Eki & Yıldız İkonu',
                '+6 Ekstra Ev (Toplam 9 Home) Belirleme Hakkı',
                '/craft (/workbench) Komutu ile Hızlı Çalışma Masası',
                '/repair Komutu ile Eşya Tamiri',
                'Günlük Özel Gold VIP Kiti',
                'Satın Alımda 25.000 Oyun İçi Ödül Bakiye',
                'Pazar Alanında Ekstra İlan Ayrıcalığı'
            ],
            'vip_reward_commands' => [
                ['server' => 'skyblock', 'command' => 'eco give {player} 25000'],
                ['server' => 'smp', 'command' => 'eco give {player} 25000']
            ],
            'inheritance' => ['iron-vip'],
            'permissions' => [
                'essentials.kits.goldvip',
                'essentials.sethome.multiple.gold',
                'essentials.workbench',
                'essentials.repair',
                'essentials.chat.format',
                'grizmperms.vip.gold'
            ],
            'server_permissions' => [
                'pvp' => ['-essentials.repair', '-essentials.workbench']
            ]
        ],
        'iron-vip' => [
            'id' => 'iron-vip',
            'name' => 'IRON VIP',
            'weight' => 100,
            'prefix' => '&7&l[IronVIP] &f',
            'suffix' => '',
            'chat_color' => '&f',
            'badge_color' => '#d4d4d8',
            'is_default' => false,
            'is_vip' => true,
            'vip_tier' => 'iron',
            'vip_price' => 75,
            'vip_duration_days' => 30,
            'vip_badge' => 'BAŞLANGIÇ',
            'vip_highlight' => false,
            'vip_perks' => [
                'Sohbette Özel [IronVIP] Ön Eki & Beyaz Yazı',
                'Dolu Sunuculara Öncelikli Giriş Hakkı',
                'Günlük Özel VIP Kiti (/kit ironvip)',
                '+3 Ekstra Ev (Toplam 6 Home) Belirleme Hakkı',
                'Sohbette Renkli Yazı Yazabilme (& kodları)',
                '/feed Komutu ile Açlık Doldurma',
                'Merkez Lobide /fly Uçuş Ayrıcalığı',
                'Satın Alımda 10.000 Oyun İçi Ödül Bakiye'
            ],
            'vip_reward_commands' => [
                ['server' => 'skyblock', 'command' => 'eco give {player} 10000'],
                ['server' => 'smp', 'command' => 'eco give {player} 10000']
            ],
            'inheritance' => ['default'],
            'permissions' => [
                'essentials.joinfullserver',
                'essentials.kits.ironvip',
                'essentials.sethome.multiple.iron',
                'essentials.chat.color',
                'essentials.feed',
                'grizmperms.vip',
                'grizmperms.vip.iron'
            ],
            'server_permissions' => [
                'lobby' => ['essentials.fly'],
                'pvp' => ['-essentials.feed']
            ]
        ],
        'default' => [
            'id' => 'default',
            'name' => 'Oyuncu',
            'weight' => 10,
            'prefix' => '&8[Oyuncu] &7',
            'suffix' => '',
            'chat_color' => '&7',
            'badge_color' => '#71717a',
            'is_default' => true,
            'is_vip' => false,
            'vip_tier' => '',
            'vip_price' => 0,
            'vip_duration_days' => 0,
            'vip_badge' => '',
            'vip_highlight' => false,
            'vip_perks' => [],
            'vip_reward_commands' => [],
            'inheritance' => [],
            'permissions' => [
                'essentials.spawn',
                'essentials.tpa',
                'essentials.tpaccept',
                'essentials.tpdeny',
                'essentials.home',
                'essentials.sethome',
                'essentials.delhome',
                'essentials.sethome.multiple',
                'essentials.warp',
                'essentials.warp.list',
                'essentials.msg',
                'essentials.reply',
                'essentials.balance',
                'essentials.pay',
                'essentials.kit',
                'essentials.kits.starter',
                'essentials.help',
                'essentials.list',
                'essentials.rules',
                'grizmperms.player'
            ],
            'server_permissions' => [
                'pvp' => ['-essentials.tpa', '-essentials.sethome', '-essentials.home']
            ]
        ]
    ];
}

// ============================================================================
// 2.1 KASA VE ANAHTAR (CRATE KEYS) MARKETİ KATALOĞU
// ============================================================================
$CRATE_KEYS_STORE = [
    'siradan' => [
        'id' => 'siradan',
        'tier' => 'siradan',
        'name' => 'Sıradan Kasa Anahtarı',
        'subtitle' => 'Seviye I &bull; Başlangıç Kasası',
        'unit_price' => 10.0,
        'color' => '#e4e4e7',
        'glow_color' => 'rgba(228, 228, 231, 0.35)',
        'badge' => 'SEVİYE I',
        'icon' => 'fa-solid fa-key',
        'description' => 'Temel hayatta kalma eşyaları ve başlangıç ödülleri.',
        'highlights' => [
            'Temel alet ve maden kitleri',
            'Başlangıç oyun içi nakit ödülleri',
            'Demir ve kömür kaynak paketleri',
            'Giriş seviyesi yemek ve iksirler'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 10.0,  'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 27.0,  'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 42.5,  'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 80.0,  'badge' => '%20 İndirim']
        ]
    ],
    'gelismis' => [
        'id' => 'gelismis',
        'tier' => 'gelismis',
        'name' => 'Gelişmiş Kasa Anahtarı',
        'subtitle' => 'Seviye II &bull; Macera Kasası',
        'unit_price' => 25.0,
        'color' => '#22c55e',
        'glow_color' => 'rgba(34, 197, 94, 0.4)',
        'badge' => 'SEVİYE II',
        'icon' => 'fa-solid fa-gem',
        'description' => 'Gelişmiş aletler, cevherler ve XP şişeleri.',
        'highlights' => [
            'Gelişmiş elmas ve zümrüt madenleri',
            'Büyük XP depolama şişeleri',
            'Verimlilik & Kırılmazlık aletleri',
            'Özel madenci şans iksirleri'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 25.0,   'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 67.5,   'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 106.25, 'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 200.0,  'badge' => '%20 İndirim']
        ]
    ],
    'nadir' => [
        'id' => 'nadir',
        'tier' => 'nadir',
        'name' => 'Nadir Kasa Anahtarı',
        'subtitle' => 'Seviye III &bull; Usta Kasası',
        'unit_price' => 50.0,
        'color' => '#3b82f6',
        'glow_color' => 'rgba(59, 130, 246, 0.45)',
        'badge' => 'SEVİYE III',
        'icon' => 'fa-solid fa-shield-halved',
        'description' => 'Büyülü elmas ekipmanlar ve değerli madenler.',
        'highlights' => [
            'Seviye IV Büyülü Elmas Zırh Setleri',
            'Keskinlik V & Güç V silahlar',
            'Yüksek miktarda oyun içi ekonomi ödülü',
            'Nadir spawner & minyon yükseltmeleri'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 50.0,   'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 135.0,  'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 212.5,  'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 400.0,  'badge' => '%20 İndirim']
        ]
    ],
    'epik' => [
        'id' => 'epik',
        'tier' => 'epik',
        'name' => 'Epik Kasa Anahtarı',
        'subtitle' => 'Seviye IV &bull; Kahraman Kasası',
        'unit_price' => 90.0,
        'color' => '#a855f7',
        'glow_color' => 'rgba(168, 85, 247, 0.45)',
        'badge' => 'SEVİYE IV',
        'icon' => 'fa-solid fa-wand-magic-sparkles',
        'description' => 'Özel büyülü zırhlar, kılıçlar ve şanslı sandıklar.',
        'highlights' => [
            'Özel efsunlu netherite takviyeleri',
            'Kritik vuruşlu özel silahlar',
            'Şanslı sandık & elit spawnerlar',
            'Sunucu geneli yüksek değerli borsa kuponları'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 90.0,   'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 243.0,  'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 382.5,  'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 720.0,  'badge' => '%20 İndirim']
        ]
    ],
    'mistik' => [
        'id' => 'mistik',
        'tier' => 'mistik',
        'name' => 'Mistik Kasa Anahtarı',
        'subtitle' => 'Seviye V &bull; Kadim Büyü Kasası',
        'unit_price' => 150.0,
        'color' => '#ec4899',
        'glow_color' => 'rgba(236, 72, 153, 0.45)',
        'badge' => 'SEVİYE V',
        'icon' => 'fa-solid fa-star',
        'description' => 'Netherite parçaları, totemler ve büyülü altın elmalar.',
        'highlights' => [
            'Saf Netherite külçeleri & kalıpları',
            'Ölümsüzlük Totemleri & Notch Elması',
            'Seviye VI özel sunucu büyü kitapları',
            'Mega para ödülleri & özel unvanlar'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 150.0,  'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 405.0,  'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 637.5,  'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 1200.0, 'badge' => '%20 İndirim']
        ]
    ],
    'efsanevi' => [
        'id' => 'efsanevi',
        'tier' => 'efsanevi',
        'name' => 'Efsanevi Kasa Anahtarı',
        'subtitle' => 'Seviye VI &bull; Nihai Güç Kasası',
        'unit_price' => 250.0,
        'color' => '#eab308',
        'glow_color' => 'rgba(234, 179, 8, 0.55)',
        'badge' => 'NİHAİ SEVİYE',
        'icon' => 'fa-solid fa-crown',
        'description' => 'Maksimum seviye unsafe büyülü netherite setler ve süper nadir ödüller.',
        'highlights' => [
            'Maksimum seviye Unsafe Efsunlu Netherite Set',
            'Efsanevi Kılıç & Yay (Özel Parçacık Efektli)',
            'Yüksek kademe spawnerlar & ada yükseltmeleri',
            'Ömür boyu kalıcı kozmetik partikül hakları'
        ],
        'bundles' => [
            1  => ['amount' => 1,  'discount_percent' => 0,  'price' => 250.0,  'badge' => ''],
            3  => ['amount' => 3,  'discount_percent' => 10, 'price' => 675.0,  'badge' => '%10 İndirim'],
            5  => ['amount' => 5,  'discount_percent' => 15, 'price' => 1062.5, 'badge' => '%15 İndirim'],
            10 => ['amount' => 10, 'discount_percent' => 20, 'price' => 2000.0, 'badge' => '%20 İndirim']
        ]
    ]
];

function gperm_get_crate_keys_catalog() {
    global $CRATE_KEYS_STORE;
    return $CRATE_KEYS_STORE;
}

function gperm_get_crate_key($tier) {
    global $CRATE_KEYS_STORE;
    $t = strtolower(trim($tier));
    return $CRATE_KEYS_STORE[$t] ?? null;
}

function gperm_calculate_crate_key_price($tier, $amount) {
    $item = gperm_get_crate_key($tier);
    if (!$item) return null;
    $amount = intval($amount);
    if ($amount <= 0) return null;

    if (isset($item['bundles'][$amount])) {
        return round(floatval($item['bundles'][$amount]['price']), 2);
    }

    $unit = floatval($item['unit_price'] ?? 0);
    $raw = $unit * $amount;
    $discount = 0;
    if ($amount >= 10) {
        $discount = 0.20;
    } elseif ($amount >= 5) {
        $discount = 0.15;
    } elseif ($amount >= 3) {
        $discount = 0.10;
    }

    return round($raw * (1.0 - $discount), 2);
}

// ============================================================================
// 3. ROL YÖNETİMİ FONKSİYONLARI
// ============================================================================

function gperm_get_roles() {
    $roles = gperm_read_json('roles.json', null);
    if (!$roles || empty($roles)) {
        $roles = gperm_default_roles_seed();
        gperm_write_json('roles.json', $roles);
        gperm_get_sync_meta();
    }

    uasort($roles, function ($a, $b) {
        return intval($b['weight'] ?? 0) <=> intval($a['weight'] ?? 0);
    });

    return $roles;
}

function gperm_get_role($roleId) {
    $roles = gperm_get_roles();
    $key = strtolower(trim($roleId));
    return $roles[$key] ?? null;
}

function gperm_get_default_role_id() {
    $roles = gperm_get_roles();
    foreach ($roles as $id => $r) {
        if (!empty($r['is_default'])) {
            return $id;
        }
    }
    return isset($roles['default']) ? 'default' : array_key_last($roles);
}

function gperm_save_role($roleData) {
    $id = strtolower(preg_replace('/[^a-z0-9_-]/', '', strtolower(trim($roleData['id'] ?? ''))));
    if (empty($id)) {
        return ['success' => false, 'message' => 'Geçersiz rol kimliği (ID). Sadece harf, rakam ve tire kullanın.'];
    }

    $roles = gperm_get_roles();
    $existing = $roles[$id] ?? [];

    $isDefault = !empty($roleData['is_default']);
    if ($isDefault) {
        foreach ($roles as $rKey => &$rVal) {
            $rVal['is_default'] = ($rKey === $id);
        }
        unset($rVal);
    }

    $perms = [];
    if (isset($roleData['permissions'])) {
        $rawPerms = is_array($roleData['permissions'])
            ? $roleData['permissions']
            : preg_split('/[\r\n,]+/', strval($roleData['permissions']));
        foreach ($rawPerms as $p) {
            $p = trim($p);
            if ($p !== '' && !in_array($p, $perms, true)) {
                $perms[] = $p;
            }
        }
    } else {
        $perms = $existing['permissions'] ?? [];
    }

    $inheritance = [];
    if (isset($roleData['inheritance'])) {
        $rawInh = is_array($roleData['inheritance'])
            ? $roleData['inheritance']
            : preg_split('/[\r\n,]+/', strval($roleData['inheritance']));
        foreach ($rawInh as $inh) {
            $inh = strtolower(trim($inh));
            if ($inh !== '' && $inh !== $id && !in_array($inh, $inheritance, true)) {
                $inheritance[] = $inh;
            }
        }
    } else {
        $inheritance = $existing['inheritance'] ?? [];
    }

    $serverPerms = $existing['server_permissions'] ?? [];
    if (isset($roleData['server_permissions']) && is_array($roleData['server_permissions'])) {
        $serverPerms = [];
        foreach ($roleData['server_permissions'] as $srvKey => $sPerms) {
            $srvKey = strtolower(trim($srvKey));
            if ($srvKey === '') continue;
            $cleanList = [];
            $arr = is_array($sPerms) ? $sPerms : preg_split('/[\r\n,]+/', strval($sPerms));
            foreach ($arr as $sp) {
                $sp = trim($sp);
                if ($sp !== '' && !in_array($sp, $cleanList, true)) {
                    $cleanList[] = $sp;
                }
            }
            if (!empty($cleanList)) {
                $serverPerms[$srvKey] = $cleanList;
            }
        }
    }

    $vipPerks = $existing['vip_perks'] ?? [];
    if (isset($roleData['vip_perks'])) {
        $vipPerks = [];
        $rawPerks = is_array($roleData['vip_perks'])
            ? $roleData['vip_perks']
            : preg_split('/[\r\n]+/', strval($roleData['vip_perks']));
        foreach ($rawPerks as $perk) {
            $perk = trim($perk);
            if ($perk !== '') {
                $vipPerks[] = $perk;
            }
        }
    }

    $rewardCommands = $existing['vip_reward_commands'] ?? [];
    if (isset($roleData['vip_reward_commands']) && is_array($roleData['vip_reward_commands'])) {
        $rewardCommands = [];
        foreach ($roleData['vip_reward_commands'] as $rc) {
            if (is_array($rc) && !empty($rc['command'])) {
                $rewardCommands[] = [
                    'server' => strtolower(trim($rc['server'] ?? 'first')) ?: 'first',
                    'command' => trim($rc['command'])
                ];
            }
        }
    }

    $roles[$id] = [
        'id' => $id,
        'name' => trim(strip_tags(strval($roleData['name'] ?? ($existing['name'] ?? strtoupper($id))))),
        'weight' => intval($roleData['weight'] ?? ($existing['weight'] ?? 10)),
        'prefix' => strval($roleData['prefix'] ?? ($existing['prefix'] ?? '')),
        'suffix' => strval($roleData['suffix'] ?? ($existing['suffix'] ?? '')),
        'chat_color' => strval($roleData['chat_color'] ?? ($existing['chat_color'] ?? '&f')),
        'badge_color' => strval($roleData['badge_color'] ?? ($existing['badge_color'] ?? '#ffffff')),
        'is_default' => $isDefault || (!empty($existing['is_default']) && !isset($roleData['is_default'])),
        'is_vip' => isset($roleData['is_vip']) ? (bool)$roleData['is_vip'] : (bool)($existing['is_vip'] ?? false),
        'vip_tier' => trim(strip_tags(strval($roleData['vip_tier'] ?? ($existing['vip_tier'] ?? '')))),
        'vip_price' => floatval($roleData['vip_price'] ?? ($existing['vip_price'] ?? 0)),
        'vip_duration_days' => intval($roleData['vip_duration_days'] ?? ($existing['vip_duration_days'] ?? 30)),
        'vip_badge' => trim(strip_tags(strval($roleData['vip_badge'] ?? ($existing['vip_badge'] ?? 'VIP')))),
        'vip_highlight' => isset($roleData['vip_highlight']) ? (bool)$roleData['vip_highlight'] : (bool)($existing['vip_highlight'] ?? false),
        'vip_perks' => $vipPerks,
        'vip_reward_commands' => $rewardCommands,
        'inheritance' => $inheritance,
        'permissions' => $perms,
        'server_permissions' => $serverPerms
    ];

    gperm_write_json('roles.json', $roles);
    $rev = gperm_bump_revision("save_role:$id");
    gperm_notify_servers_sync();

    return ['success' => true, 'role' => $roles[$id], 'revision' => $rev];
}

function gperm_delete_role($roleId) {
    $id = strtolower(trim($roleId));
    $roles = gperm_get_roles();
    if (!isset($roles[$id])) {
        return ['success' => false, 'message' => 'Rol bulunamadı.'];
    }
    if (!empty($roles[$id]['is_default']) || $id === 'default') {
        return ['success' => false, 'message' => 'Varsayılan oyuncu rolü silinemez.'];
    }

    unset($roles[$id]);
    foreach ($roles as &$r) {
        if (!empty($r['inheritance'])) {
            $r['inheritance'] = array_values(array_filter($r['inheritance'], fn($x) => $x !== $id));
        }
    }
    unset($r);

    gperm_write_json('roles.json', $roles);
    $rev = gperm_bump_revision("delete_role:$id");
    gperm_notify_servers_sync();

    return ['success' => true, 'revision' => $rev];
}

function gperm_get_store_vip_packages() {
    $roles = gperm_get_roles();
    $packages = [];
    foreach ($roles as $r) {
        if (!empty($r['is_vip']) && floatval($r['vip_price']) > 0) {
            $days = intval($r['vip_duration_days'] ?? 30);
            $packages[] = [
                'id' => $r['id'],
                'name' => $r['name'],
                'tier' => $r['vip_tier'] ?: str_replace('-vip', '', $r['id']),
                'price' => rtrim(rtrim(number_format(floatval($r['vip_price']), 2, '.', ''), '0'), '.'),
                'price_num' => floatval($r['vip_price']),
                'duration' => $days > 0 ? ($days . ' Gün') : 'Süresiz',
                'duration_days' => $days,
                'badge' => $r['vip_badge'] ?: 'VIP',
                'badge_color' => $r['badge_color'] ?? '#ffffff',
                'prefix' => $r['prefix'] ?? '',
                'highlight' => !empty($r['vip_highlight']),
                'perks' => !empty($r['vip_perks']) ? $r['vip_perks'] : ['Özel VIP ayrıcalıkları ve komutları']
            ];
        }
    }

    usort($packages, function ($a, $b) {
        return $a['price_num'] <=> $b['price_num'];
    });

    return $packages;
}

// ============================================================================
// 4. OYUNCU ROLLERİ, İZİNLERİ VE CÜZDAN BAKİYESİ YÖNETİMİ
// ============================================================================

function gperm_default_users_seed() {
    return [
        'grizm' => [
            'username' => 'Grizm',
            'email' => 'admin@grizm.tech',
            'credits' => 1000.0,
            'roles' => [
                [
                    'role_id' => 'owner',
                    'assigned_at' => time(),
                    'expires_at' => 0,
                    'source' => 'system_seed'
                ]
            ],
            'permissions' => [],
            'server_permissions' => [],
            'pending_deliveries' => [],
            'updated_at' => time()
        ]
    ];
}

function gperm_get_all_user_perms($pruneExpired = true) {
    $users = gperm_read_json('user_perms.json', null);
    if ($users === null) {
        $users = gperm_default_users_seed();
        gperm_write_json('user_perms.json', $users);
    }

    if ($pruneExpired) {
        $now = time();
        $changed = false;
        foreach ($users as $uKey => &$uData) {
            if (!empty($uData['roles']) && is_array($uData['roles'])) {
                $filtered = [];
                foreach ($uData['roles'] as $assignment) {
                    $exp = intval($assignment['expires_at'] ?? 0);
                    if ($exp > 0 && $exp <= $now) {
                        $changed = true;
                        continue;
                    }
                    $filtered[] = $assignment;
                }
                $uData['roles'] = $filtered;
            }
        }
        unset($uData);

        if ($changed) {
            gperm_write_json('user_perms.json', $users);
            gperm_bump_revision('expire_prune');
        }
    }

    return $users;
}

function gperm_find_email_for_username($username) {
    if (!function_exists('get_all_saved_users')) {
        return '';
    }
    $allWebUsers = get_all_saved_users();
    $target = strtolower(trim($username));
    foreach ($allWebUsers as $email => $u) {
        if (!empty($u['minecraft_username']) && strtolower(trim($u['minecraft_username'])) === $target) {
            return $u['email'] ?? $email;
        }
    }
    return '';
}

function gperm_get_user($username) {
    $uKey = strtolower(trim($username));
    if ($uKey === '') {
        return null;
    }

    $users = gperm_get_all_user_perms(true);
    if (!isset($users[$uKey])) {
        return [
            'username' => trim($username),
            'email' => gperm_find_email_for_username($username),
            'credits' => 0.0,
            'roles' => [],
            'permissions' => [],
            'server_permissions' => [],
            'pending_deliveries' => [],
            'updated_at' => time()
        ];
    }

    if (empty($users[$uKey]['email'])) {
        $users[$uKey]['email'] = gperm_find_email_for_username($username);
    }

    return $users[$uKey];
}

function gperm_save_user_record($username, $record, $reason = 'user_update', $notifyServers = true) {
    $cleanName = trim($username);
    $uKey = strtolower($cleanName);
    if ($uKey === '' || !preg_match('/^[a-zA-Z0-9_]{3,16}$/', $cleanName)) return false;

    $users = gperm_get_all_user_perms(false);
    $record['username'] = $record['username'] ?? trim($username);
    if (empty($record['email'])) {
        $record['email'] = gperm_find_email_for_username($username);
    }
    $record['credits'] = round(max(0.0, floatval($record['credits'] ?? 0)), 2);
    $record['roles'] = array_values($record['roles'] ?? []);
    $record['permissions'] = array_values(array_unique(array_filter(array_map('trim', $record['permissions'] ?? []))));
    $record['server_permissions'] = $record['server_permissions'] ?? [];
    $record['pending_deliveries'] = array_values($record['pending_deliveries'] ?? []);
    $record['updated_at'] = time();

    $users[$uKey] = $record;
    gperm_write_json('user_perms.json', $users);
    $rev = gperm_bump_revision($reason);

    if ($notifyServers) {
        gperm_notify_servers_sync($record['username']);
    }

    return $rev;
}

function gperm_get_user_primary_role($username) {
    $roles = gperm_get_roles();
    $defaultId = gperm_get_default_role_id();
    $user = gperm_get_user($username);

    $bestRole = $roles[$defaultId] ?? reset($roles);
    $bestAssignment = ['role_id' => $defaultId, 'expires_at' => 0, 'assigned_at' => 0];
    $bestWeight = intval($bestRole['weight'] ?? 0);

    if ($user && !empty($user['roles'])) {
        $now = time();
        foreach ($user['roles'] as $ass) {
            $rid = strtolower($ass['role_id'] ?? '');
            $exp = intval($ass['expires_at'] ?? 0);
            if ($exp > 0 && $exp <= $now) continue;
            if (isset($roles[$rid])) {
                $w = intval($roles[$rid]['weight'] ?? 0);
                if ($w >= $bestWeight) {
                    $bestWeight = $w;
                    $bestRole = $roles[$rid];
                    $bestAssignment = $ass;
                }
            }
        }
    }

    return [
        'role' => $bestRole,
        'assignment' => $bestAssignment
    ];
}

function gperm_get_user_active_vip($username) {
    $roles = gperm_get_roles();
    $user = gperm_get_user($username);
    if (!$user || empty($user['roles'])) {
        return null;
    }

    $now = time();
    $bestVip = null;
    $bestWeight = -1;

    foreach ($user['roles'] as $ass) {
        $rid = strtolower($ass['role_id'] ?? '');
        $exp = intval($ass['expires_at'] ?? 0);
        if ($exp > 0 && $exp <= $now) continue;
        if (isset($roles[$rid]) && !empty($roles[$rid]['is_vip'])) {
            $w = intval($roles[$rid]['weight'] ?? 0);
            if ($w > $bestWeight) {
                $bestWeight = $w;
                $bestVip = [
                    'role' => $roles[$rid],
                    'assigned_at' => intval($ass['assigned_at'] ?? $now),
                    'expires_at' => $exp,
                    'remaining_seconds' => $exp > 0 ? max(0, $exp - $now) : -1
                ];
            }
        }
    }

    return $bestVip;
}

/**
 * Oyuncunun tüm aktif rollerini (miras dahil) ve efektif izin (perm) listesini döndürür
 */
function gperm_get_user_effective_summary($username) {
    $roles = gperm_get_roles();
    $defaultId = gperm_get_default_role_id();
    $user = gperm_get_user($username);
    $now = time();

    $assignedRoles = [];
    $inheritedIds = [$defaultId => true];

    $collectInh = function ($rid) use (&$collectInh, &$inheritedIds, $roles) {
        $rid = strtolower(trim($rid));
        if ($rid === '' || !isset($roles[$rid])) return;
        $inheritedIds[$rid] = true;
        foreach ($roles[$rid]['inheritance'] ?? [] as $parent) {
            $parent = strtolower(trim($parent));
            if (!isset($inheritedIds[$parent])) {
                $collectInh($parent);
            }
        }
    };

    $collectInh($defaultId);

    if ($user && !empty($user['roles'])) {
        foreach ($user['roles'] as $ass) {
            $rid = strtolower($ass['role_id'] ?? '');
            $exp = intval($ass['expires_at'] ?? 0);
            if ($exp > 0 && $exp <= $now) continue;
            if (isset($roles[$rid])) {
                $assignedRoles[] = [
                    'role' => $roles[$rid],
                    'expires_at' => $exp,
                    'remaining' => gperm_format_remaining($exp)
                ];
                $collectInh($rid);
            }
        }
    }

    if (empty($assignedRoles) && isset($roles[$defaultId])) {
        $assignedRoles[] = [
            'role' => $roles[$defaultId],
            'expires_at' => 0,
            'remaining' => 'Süresiz'
        ];
    }

    // Düşük ağırlıktan yüksek ağırlığa izinleri birleştir
    $orderedRoleObjs = [];
    foreach (array_keys($inheritedIds) as $rid) {
        if (isset($roles[$rid])) {
            $orderedRoleObjs[] = $roles[$rid];
        }
    }
    usort($orderedRoleObjs, fn($a, $b) => intval($a['weight'] ?? 0) <=> intval($b['weight'] ?? 0));

    $effectivePerms = [];
    foreach ($orderedRoleObjs as $rObj) {
        foreach ($rObj['permissions'] ?? [] as $p) {
            $effectivePerms[$p] = true;
        }
    }
    if ($user && !empty($user['permissions'])) {
        foreach ($user['permissions'] as $up) {
            $effectivePerms[$up] = true;
        }
    }

    return [
        'assigned_roles' => $assignedRoles,
        'permissions' => array_keys($effectivePerms),
        'custom_permissions' => $user['permissions'] ?? []
    ];
}

function gperm_grant_user_role($username, $roleId, $durationDays = 0, $source = 'admin', $queueCelebration = false) {
    $role = gperm_get_role($roleId);
    if (!$role) {
        return ['success' => false, 'message' => "Belirtilen rol bulunamadı: $roleId"];
    }

    $user = gperm_get_user($username);
    if (!$user) {
        return ['success' => false, 'message' => 'Geçersiz oyuncu adı.'];
    }

    $now = time();
    $rid = $role['id'];
    $durationSeconds = $durationDays > 0 ? ($durationDays * 86400) : 0;
    $found = false;
    $newExpiresAt = $durationSeconds > 0 ? ($now + $durationSeconds) : 0;

    foreach ($user['roles'] as &$ass) {
        if (strtolower($ass['role_id']) === $rid) {
            $found = true;
            if ($durationSeconds === 0) {
                $ass['expires_at'] = 0;
                $newExpiresAt = 0;
            } else {
                $currentExp = intval($ass['expires_at'] ?? 0);
                if ($currentExp === 0) {
                    $newExpiresAt = 0;
                } else {
                    $baseTime = max($now, $currentExp);
                    $ass['expires_at'] = $baseTime + $durationSeconds;
                    $newExpiresAt = $ass['expires_at'];
                }
            }
            $ass['source'] = $source;
            break;
        }
    }
    unset($ass);

    if (!$found) {
        $user['roles'][] = [
            'role_id' => $rid,
            'assigned_at' => $now,
            'expires_at' => $newExpiresAt,
            'source' => $source
        ];
    }

    if ($queueCelebration) {
        $deliveryId = 'DEL-' . strtoupper(bin2hex(random_bytes(5)));
        $targetServers = [];
        if (!empty($role['vip_reward_commands']) && is_array($role['vip_reward_commands'])) {
            foreach ($role['vip_reward_commands'] as $cmdObj) {
                $srv = strtolower(trim($cmdObj['server'] ?? 'first'));
                if ($srv !== '' && !in_array($srv, $targetServers, true)) {
                    $targetServers[] = $srv;
                }
            }
        }

        $user['pending_deliveries'][] = [
            'id' => $deliveryId,
            'role_id' => $rid,
            'role_name' => $role['name'],
            'duration_days' => $durationDays,
            'created_at' => $now,
            'celebrated' => false,
            'required_servers' => $targetServers,
            'executed_servers' => []
        ];
    }

    $rev = gperm_save_user_record($username, $user, "grant_role:$username:$rid", true);

    return [
        'success' => true,
        'role' => $role,
        'expires_at' => $newExpiresAt,
        'extended' => $found,
        'revision' => $rev
    ];
}

function gperm_revoke_user_role($username, $roleId, $notifyServers = true) {
    $user = gperm_get_user($username);
    if (!$user) {
        return ['success' => false, 'message' => 'Geçersiz oyuncu adı.'];
    }
    $rid = strtolower(trim($roleId));
    $before = count($user['roles']);
    $user['roles'] = array_values(array_filter($user['roles'], function ($ass) use ($rid) {
        return strtolower($ass['role_id'] ?? '') !== $rid;
    }));

    if (count($user['roles']) === $before) {
        return ['success' => false, 'message' => 'Oyuncuda bu rol zaten bulunmuyor.'];
    }

    $rev = gperm_save_user_record($username, $user, "revoke_role:$username:$rid", $notifyServers);
    return ['success' => true, 'revision' => $rev];
}

function gperm_adjust_user_credits($username, $delta, $reason = 'credit_adjust') {
    $user = gperm_get_user($username);
    if (!$user) {
        return ['success' => false, 'message' => 'Geçersiz oyuncu adı.'];
    }
    $newBalance = round(floatval($user['credits'] ?? 0) + floatval($delta), 2);
    if ($newBalance < 0) {
        return ['success' => false, 'message' => 'Yetersiz cüzdan bakiyesi.'];
    }
    $user['credits'] = $newBalance;
    $rev = gperm_save_user_record($username, $user, $reason, false);
    return ['success' => true, 'credits' => $newBalance, 'revision' => $rev];
}

function gperm_set_user_credits($username, $amount, $reason = 'admin_set_credit') {
    $user = gperm_get_user($username);
    if (!$user) {
        return ['success' => false, 'message' => 'Geçersiz oyuncu adı.'];
    }
    $user['credits'] = round(max(0.0, floatval($amount)), 2);
    $rev = gperm_save_user_record($username, $user, $reason, false);
    return ['success' => true, 'credits' => $user['credits'], 'revision' => $rev];
}

// ============================================================================
// 5. MAĞAZA SATIN ALIM, SİPARİŞ KAYDI VE KUPON SİSTEMİ
// ============================================================================

function gperm_get_orders() {
    return gperm_read_json('vip_orders.json', []);
}

function gperm_log_order($entry) {
    $orders = gperm_get_orders();
    $record = array_merge([
        'order_id' => 'ORD-' . strtoupper(bin2hex(random_bytes(5))),
        'username' => '',
        'email' => '',
        'type' => 'vip_purchase',
        'role_id' => '',
        'role_name' => '',
        'amount' => 0.0,
        'duration_days' => 0,
        'provider' => 'wallet',
        'note' => '',
        'created_at' => time()
    ], $entry);

    array_unshift($orders, $record);
    if (count($orders) > 500) {
        $orders = array_slice($orders, 0, 500);
    }
    gperm_write_json('vip_orders.json', $orders);
    return $record;
}

function gperm_get_user_orders($username) {
    $orders = gperm_get_orders();
    $target = strtolower(trim($username));
    return array_values(array_filter($orders, function ($o) use ($target) {
        return strtolower($o['username'] ?? '') === $target;
    }));
}

function gperm_purchase_vip_with_wallet($username, $roleId) {
    return gperm_with_lock(function () use ($username, $roleId) {
        $role = gperm_get_role($roleId);
        if (!$role || empty($role['is_vip'])) {
            return ['success' => false, 'message' => 'Geçerli bir VIP paketi seçilmedi.'];
        }

        $price = round(floatval($role['vip_price'] ?? 0), 2);
        $days = intval($role['vip_duration_days'] ?? 30);
        if ($price <= 0) {
            return ['success' => false, 'message' => 'Bu paket şu anda mağaza satışına kapalıdır.'];
        }

        $user = gperm_get_user($username);
        $currentCredits = round(floatval($user['credits'] ?? 0), 2);

        if ($currentCredits < $price) {
            $missing = number_format($price - $currentCredits, 2, '.', '');
            return [
                'success' => false,
                'code' => 'insufficient_balance',
                'credits' => $currentCredits,
                'required' => $price,
                'message' => "Yetersiz bakiye! Mevcut bakiyeniz: {$currentCredits} ₺ (Eksik: {$missing} ₺). Hediye/Bakiye kodu kullanabilir veya bakiye yükleyebilirsiniz."
            ];
        }

        $user['credits'] = round($currentCredits - $price, 2);
        gperm_save_user_record($username, $user, "wallet_debit:$username:$price", false);

        $grant = gperm_grant_user_role($username, $role['id'], $days, 'web_store', true);
        if (!$grant['success']) {
            gperm_adjust_user_credits($username, $price, 'wallet_refund');
            return $grant;
        }

        $order = gperm_log_order([
            'username' => $user['username'],
            'email' => $user['email'] ?? '',
            'type' => 'vip_purchase',
            'role_id' => $role['id'],
            'role_name' => $role['name'],
            'amount' => $price,
            'duration_days' => $days,
            'provider' => 'wallet',
            'note' => $grant['extended'] ? "{$days} gün süre uzatıldı" : "{$days} gün yeni VIP tanımlandı"
        ]);

        $updatedUser = gperm_get_user($username);

        return [
            'success' => true,
            'message' => "{$role['name']} paketiniz başarıyla hesabınıza tanımlandı! Oyun içinde otomatik aktif edildi.",
            'order' => $order,
            'credits' => $updatedUser['credits'],
            'expires_at' => $grant['expires_at'],
            'extended' => $grant['extended'],
            'role' => $role
        ];
    });
}

/**
 * GrizmPerms Teslimat Kuyruğuna Komut / Ödül Ekleme
 *
 * @param string $username Hedef oyuncu adı
 * @param string $command Çalıştırılacak Minecraft komutu (örn: anahtarver {player} siradan 3)
 * @param string $targetServer Hedef sunucu ('first', 'all', 'smp', 'skyblock', 'lobby', 'pvp')
 * @param string $label Kuyruk etiketi veya teslimat açıklaması
 * @return array ['success' => bool, 'delivery_id' => string, 'revision' => int]
 */
function gperm_enqueue_delivery($username, $command, $targetServer = 'first', $label = 'Teslimat') {
    $user = gperm_get_user($username);
    if (!$user) {
        return ['success' => false, 'message' => 'Geçersiz oyuncu adı.'];
    }

    $now = time();
    $deliveryId = 'DEL-' . strtoupper(bin2hex(random_bytes(5)));
    $srv = strtolower(trim($targetServer !== '' ? $targetServer : 'first'));
    $cleanCmd = trim($command);
    if (str_starts_with($cleanCmd, '/')) {
        $cleanCmd = substr($cleanCmd, 1);
    }

    $deliveryItem = [
        'id' => $deliveryId,
        'role_id' => '',
        'role_name' => $label,
        'duration_days' => 0,
        'created_at' => $now,
        'celebrated' => true,
        'required_servers' => in_array($srv, ['all', 'first'], true) ? [] : [$srv],
        'executed_servers' => [],
        'commands' => [
            [
                'server' => $srv,
                'command' => $cleanCmd
            ]
        ]
    ];

    if (!isset($user['pending_deliveries']) || !is_array($user['pending_deliveries'])) {
        $user['pending_deliveries'] = [];
    }
    $user['pending_deliveries'][] = $deliveryItem;

    $rev = gperm_save_user_record($username, $user, "enqueue_delivery:$username:$deliveryId", true);

    return [
        'success' => true,
        'delivery_id' => $deliveryId,
        'revision' => $rev,
        'delivery' => $deliveryItem
    ];
}

/**
 * Cüzdan Bakiyesiyle Kasa Anahtarı Satın Alımı
 *
 * @param string $buyerUsername Satın alan oturum sahibi
 * @param string $targetUsername Anahtarların teslim edileceği Minecraft oyuncusu
 * @param string $tier Kasa kademesi (siradan, gelismis, nadir, epik, mistik, efsanevi)
 * @param int $amount Adet (1, 3, 5, 10 vb.)
 * @return array
 */
function gperm_purchase_crate_keys_with_wallet($buyerUsername, $targetUsername, $tier, $amount = 1) {
    return gperm_with_lock(function () use ($buyerUsername, $targetUsername, $tier, $amount) {
        $tier = strtolower(trim($tier));
        $catalogItem = gperm_get_crate_key($tier);
        if (!$catalogItem) {
            return ['success' => false, 'message' => 'Geçersiz kasa kademesi seçildi.'];
        }

        $amount = max(1, intval($amount));
        $price = gperm_calculate_crate_key_price($tier, $amount);
        if ($price === null || $price <= 0) {
            return ['success' => false, 'message' => 'Fiyat hesaplanamadı veya ürün satışa kapalı.'];
        }

        $buyer = gperm_get_user($buyerUsername);
        if (!$buyer) {
            return ['success' => false, 'message' => 'Kullanıcı hesabı bulunamadı.'];
        }

        $currentCredits = round(floatval($buyer['credits'] ?? 0), 2);
        if ($currentCredits < $price) {
            $missing = number_format($price - $currentCredits, 2, '.', '');
            return [
                'success' => false,
                'code' => 'insufficient_balance',
                'credits' => $currentCredits,
                'required' => $price,
                'message' => "Yetersiz bakiye! Mevcut bakiyeniz: {$currentCredits} ₺ (Gereken: {$price} ₺, Eksik: {$missing} ₺). Cüzdanınıza bakiye yükleyerek hemen alabilirsiniz."
            ];
        }

        $cleanTarget = trim($targetUsername !== '' ? $targetUsername : $buyerUsername);
        if (!preg_match('/^[a-zA-Z0-9_]{3,16}$/', $cleanTarget)) {
            return ['success' => false, 'message' => 'Geçersiz Minecraft oyuncu adı (3-16 karakter).'];
        }

        // 1. Cüzdandan düş
        $buyer['credits'] = round($currentCredits - $price, 2);
        gperm_save_user_record($buyerUsername, $buyer, "crate_key_debit:$buyerUsername:$price", false);

        // 2. Teslimat komutunu kuyruğa yaz: anahtarver {player} {tier} {amount}
        // GrizmPerms WebSyncService oyunda çevrimiçi olunca veya RCON ile anında teslim eder
        $deliveryCmd = "anahtarver {$cleanTarget} {$tier} {$amount}";
        $enqueue = gperm_enqueue_delivery(
            $cleanTarget,
            $deliveryCmd,
            'first',
            "{$catalogItem['name']} (x{$amount})"
        );

        if (!$enqueue['success']) {
            // İade
            gperm_adjust_user_credits($buyerUsername, $price, 'crate_key_refund');
            return $enqueue;
        }

        // 3. Sipariş kaydı oluştur
        $isSelf = (strtolower($buyerUsername) === strtolower($cleanTarget));
        $orderNote = $isSelf
            ? "{$amount}x {$catalogItem['name']} satın alındı"
            : "{$buyerUsername} tarafından {$cleanTarget} oyuncusuna hediye edildi ({$amount}x {$catalogItem['name']})";

        $order = gperm_log_order([
            'username' => $cleanTarget,
            'email' => $buyer['email'] ?? '',
            'type' => 'crate_key_purchase',
            'role_id' => $tier,
            'role_name' => "{$catalogItem['name']} (x{$amount})",
            'amount' => $price,
            'duration_days' => 0,
            'provider' => $isSelf ? 'wallet' : 'wallet_gift',
            'note' => $orderNote
        ]);

        $updatedBuyer = gperm_get_user($buyerUsername);

        return [
            'success' => true,
            'message' => "Tebrikler! {$amount} adet {$catalogItem['name']} başarıyla teslimat kuyruğuna alındı ve oyunda tanımlandı!",
            'order' => $order,
            'credits' => $updatedBuyer['credits'],
            'tier' => $catalogItem,
            'amount' => $amount,
            'total_price' => $price,
            'target_username' => $cleanTarget,
            'delivery_id' => $enqueue['delivery_id']
        ];
    });
}

// ============================================================================
// 6. KUPON & PROMOSYON KODLARI (COUPONS.JSON)
// ============================================================================

function gperm_default_coupons_seed() {
    return [
        'GRIZM25' => [
            'code' => 'GRIZM25',
            'type' => 'credit',
            'amount' => 25.0,
            'role_id' => '',
            'duration_days' => 0,
            'max_uses' => 100,
            'used_by' => [],
            'expires_at' => 0,
            'active' => true,
            'created_at' => time()
        ]
    ];
}

function gperm_get_coupons() {
    $coupons = gperm_read_json('coupons.json', null);
    if ($coupons === null) {
        $coupons = gperm_default_coupons_seed();
        gperm_write_json('coupons.json', $coupons);
    }
    return $coupons;
}

function gperm_create_coupon($data) {
    $code = strtoupper(preg_replace('/[^A-Z0-9_-]/', '', strtoupper(trim($data['code'] ?? ''))));
    if (strlen($code) < 3) {
        return ['success' => false, 'message' => 'Kupon kodu en az 3 karakter olmalıdır.'];
    }

    $type = ($data['type'] ?? 'credit') === 'vip' ? 'vip' : 'credit';
    $amount = round(max(0.0, floatval($data['amount'] ?? 0)), 2);
    $roleId = strtolower(trim($data['role_id'] ?? ''));
    $days = max(1, intval($data['duration_days'] ?? 30));
    $maxUses = max(0, intval($data['max_uses'] ?? 1));
    $expDays = intval($data['expire_days'] ?? 0);

    if ($type === 'credit' && $amount <= 0) {
        return ['success' => false, 'message' => 'Bakiye kuponu tutarı 0\'dan büyük olmalıdır.'];
    }
    if ($type === 'vip' && !gperm_get_role($roleId)) {
        return ['success' => false, 'message' => 'Geçerli bir VIP/Rol seçilmelidir.'];
    }

    $coupons = gperm_get_coupons();
    $coupons[$code] = [
        'code' => $code,
        'type' => $type,
        'amount' => $type === 'credit' ? $amount : 0.0,
        'role_id' => $type === 'vip' ? $roleId : '',
        'duration_days' => $type === 'vip' ? $days : 0,
        'max_uses' => $maxUses,
        'used_by' => $coupons[$code]['used_by'] ?? [],
        'expires_at' => $expDays > 0 ? (time() + ($expDays * 86400)) : 0,
        'active' => true,
        'created_at' => time()
    ];

    gperm_write_json('coupons.json', $coupons);
    return ['success' => true, 'coupon' => $coupons[$code]];
}

function gperm_delete_coupon($code) {
    $key = strtoupper(trim($code));
    $coupons = gperm_get_coupons();
    if (!isset($coupons[$key])) {
        return ['success' => false, 'message' => 'Kupon bulunamadı.'];
    }
    unset($coupons[$key]);
    gperm_write_json('coupons.json', $coupons);
    return ['success' => true];
}

function gperm_redeem_coupon($username, $rawCode) {
    return gperm_with_lock(function () use ($username, $rawCode) {
        $code = strtoupper(trim($rawCode));
        if ($code === '') {
            return ['success' => false, 'message' => 'Lütfen bir kupon kodu girin.'];
        }

        $coupons = gperm_get_coupons();
        if (!isset($coupons[$code]) || empty($coupons[$code]['active'])) {
            return ['success' => false, 'message' => 'Geçersiz veya pasif kupon kodu.'];
        }

        $coupon = $coupons[$code];
        $now = time();
        if (!empty($coupon['expires_at']) && intval($coupon['expires_at']) < $now) {
            return ['success' => false, 'message' => 'Bu kupon kodunun kullanım süresi dolmuş.'];
        }

        $usedBy = $coupon['used_by'] ?? [];
        $uKey = strtolower(trim($username));
        if (in_array($uKey, $usedBy, true)) {
            return ['success' => false, 'message' => 'Bu kupon kodunu zaten kullandınız.'];
        }

        $maxUses = intval($coupon['max_uses'] ?? 0);
        if ($maxUses > 0 && count($usedBy) >= $maxUses) {
            return ['success' => false, 'message' => 'Bu kupon kodunun kullanım limiti dolmuş.'];
        }

        $coupons[$code]['used_by'][] = $uKey;
        gperm_write_json('coupons.json', $coupons);

        if ($coupon['type'] === 'vip') {
            $role = gperm_get_role($coupon['role_id']);
            $days = intval($coupon['duration_days'] ?? 30);
            $grant = gperm_grant_user_role($username, $coupon['role_id'], $days, "coupon:$code", true);
            if (!$grant['success']) {
                return $grant;
            }
            gperm_log_order([
                'username' => trim($username),
                'email' => gperm_find_email_for_username($username),
                'type' => 'coupon_vip',
                'role_id' => $coupon['role_id'],
                'role_name' => $role['name'] ?? $coupon['role_id'],
                'amount' => 0.0,
                'duration_days' => $days,
                'provider' => 'coupon',
                'note' => "Kupon Kodu: $code ({$days} Gün)"
            ]);
            $user = gperm_get_user($username);
            return [
                'success' => true,
                'type' => 'vip',
                'credits' => $user['credits'] ?? 0,
                'message' => "Tebrikler! {$code} kodu ile {$days} günlük " . ($role['name'] ?? $coupon['role_id']) . " hesabınıza tanımlandı!"
            ];
        } else {
            $amt = floatval($coupon['amount'] ?? 0);
            $adj = gperm_adjust_user_credits($username, $amt, "coupon:$code");
            gperm_log_order([
                'username' => trim($username),
                'email' => gperm_find_email_for_username($username),
                'type' => 'coupon_credit',
                'role_id' => '',
                'role_name' => 'Cüzdan Bakiyesi',
                'amount' => $amt,
                'duration_days' => 0,
                'provider' => 'coupon',
                'note' => "Kupon Kodu: $code (+{$amt} ₺)"
            ]);
            return [
                'success' => true,
                'type' => 'credit',
                'credits' => $adj['credits'] ?? 0,
                'message' => "Tebrikler! {$code} kodu ile cüzdanınıza +{$amt} ₺ bakiye eklendi!"
            ];
        }
    });
}

// ============================================================================
// 7. OYUN İÇİ TESLİMAT ONAYI (ACK) VE ANLIK RCON BİLDİRİMİ
// ============================================================================

function gperm_ack_delivery($username, $deliveryId, $serverId, $celebrated = true) {
    $uKey = strtolower(trim($username));
    $users = gperm_get_all_user_perms(false);
    if (!isset($users[$uKey]) || empty($users[$uKey]['pending_deliveries'])) {
        return ['success' => true, 'changed' => false];
    }

    $srv = strtolower(trim($serverId));
    $changed = false;
    $remaining = [];

    foreach ($users[$uKey]['pending_deliveries'] as $del) {
        if (($del['id'] ?? '') !== $deliveryId) {
            $remaining[] = $del;
            continue;
        }

        $changed = true;
        if ($celebrated) {
            $del['celebrated'] = true;
        }

        $execServers = $del['executed_servers'] ?? [];
        if ($srv !== '' && !in_array($srv, $execServers, true)) {
            $execServers[] = $srv;
        }
        if ($srv !== '' && !in_array('first', $execServers, true)) {
            $execServers[] = 'first';
        }
        $del['executed_servers'] = $execServers;

        $reqServers = $del['required_servers'] ?? [];
        $allDone = !empty($del['celebrated']);
        foreach ($reqServers as $reqSrv) {
            if ($reqSrv === 'all') {
                foreach (['lobby', 'skyblock', 'smp', 'pvp'] as $coreSrv) {
                    if (!in_array($coreSrv, $execServers, true)) {
                        $allDone = false;
                    }
                }
            } elseif (!in_array($reqSrv, $execServers, true)) {
                $allDone = false;
            }
        }

        if (!$allDone && (time() - intval($del['created_at'] ?? time())) < 604800) {
            $remaining[] = $del;
        }
    }

    if ($changed) {
        $users[$uKey]['pending_deliveries'] = $remaining;
        gperm_write_json('user_perms.json', $users);
        gperm_bump_revision("ack_delivery:$username:$deliveryId");
    }

    return ['success' => true, 'changed' => $changed];
}

function gperm_notify_servers_sync($targetUsername = '') {
    $rconFile = __DIR__ . '/rcon.php';
    $smFile = __DIR__ . '/server_manager.php';
    if (!file_exists($rconFile) || !file_exists($smFile)) {
        return;
    }
    require_once $rconFile;
    require_once $smFile;

    if (!class_exists('ServerManager') || !class_exists('MinecraftRcon')) {
        return;
    }

    $cmd = $targetUsername !== '' ? ('gperm sync ' . preg_replace('/[^a-zA-Z0-9_]/', '', $targetUsername)) : 'gperm sync';
    $servers = ServerManager::getServers();
    foreach ($servers as $srv) {
        $rconPort = intval($srv['rcon_port'] ?? 0);
        $rconPass = $srv['rcon_password'] ?? '';
        if ($rconPort <= 0 || $rconPass === '') {
            continue;
        }
        $fp = @fsockopen('127.0.0.1', $rconPort, $errno, $errstr, 0.15);
        if (!$fp) {
            continue;
        }
        fclose($fp);

        try {
            $rcon = new MinecraftRcon('127.0.0.1', $rconPort, $rconPass, 1);
            if ($rcon->connect()) {
                $rcon->sendCommand($cmd);
                $rcon->disconnect();
            }
        } catch (\Throwable $e) {
        }
    }
}

function gperm_format_remaining($expiresAt) {
    $exp = intval($expiresAt);
    if ($exp <= 0) {
        return 'Süresiz';
    }
    $diff = $exp - time();
    if ($diff <= 0) {
        return 'Süresi Doldu';
    }
    $days = floor($diff / 86400);
    $hours = floor(($diff % 86400) / 3600);
    $mins = floor(($diff % 3600) / 60);
    if ($days > 0) {
        return $days . ' gün ' . $hours . ' saat';
    }
    if ($hours > 0) {
        return $hours . ' saat ' . $mins . ' dk';
    }
    return max(1, $mins) . ' dakika';
}

// VIP Paketlerini dinamik olarak roles.json üzerinden doldur
$VIP_PACKAGES = gperm_get_store_vip_packages();
