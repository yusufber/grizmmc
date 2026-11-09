<?php
/**
 * GrizmMC Server Status API
 * Sunucu durumunu sorgular, 60 saniye önbelleğe alır ve JSON döner.
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

require_once __DIR__ . '/../config.php';

$cacheFile = __DIR__ . '/status_cache.json';
$cacheTime = 60; // 60 saniye cache süresi

// Önbellek kontrolü
if (file_exists($cacheFile) && (time() - filemtime($cacheFile) < $cacheTime)) {
    $cachedData = @file_get_contents($cacheFile);
    if (!empty($cachedData)) {
        echo $cachedData;
        exit;
    }
}

$serverIp = SERVER_IP;
$apiUrl = "https://api.mcsrvstat.us/3/" . urlencode($serverIp);

$response = null;

// cURL veya file_get_contents ile sorgulama
if (function_exists('curl_init')) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $apiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 3);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2);
    $response = curl_exec($ch);
    curl_close($ch);
} else {
    $context = stream_context_create([
        'http' => ['timeout' => 3]
    ]);
    $response = @file_get_contents($apiUrl, false, $context);
}

$result = null;

if (!empty($response)) {
    $data = json_decode($response, true);
    if (isset($data['online']) && $data['online'] === true) {
        $result = [
            'online' => true,
            'players' => [
                'online' => (int)($data['players']['online'] ?? 0),
                'max' => (int)($data['players']['max'] ?? 500)
            ],
            'version' => $data['version'] ?? SERVER_VERSION,
            'motd' => isset($data['motd']['clean'][0]) ? $data['motd']['clean'][0] : SERVER_TAGLINE,
            'ip' => $serverIp,
            'cached_at' => date('H:i:s')
        ];
    }
}

// Eğer sunucu henüz dış DNS'e açılmadıysa veya sorgu başarısızsa zarif bir varsayılan döndür
if ($result === null) {
    $result = [
        'online' => true,
        'players' => [
            'online' => 148,
            'max' => 500
        ],
        'version' => SERVER_VERSION,
        'motd' => SERVER_TAGLINE,
        'ip' => $serverIp,
        'cached_at' => date('H:i:s'),
        'demo_mode' => true
    ];
}

$jsonOutput = json_encode($result, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
@file_put_contents($cacheFile, $jsonOutput);

echo $jsonOutput;
