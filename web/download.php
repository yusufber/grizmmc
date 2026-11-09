<?php
/**
 * GrizmMC Network - Güvenli Launcher İndirme Yönlendiricisi
 * Tarayıcı MIME tipi engellerine takılmadan GrizmMcLauncher.exe dosyasını doğrudan indirir.
 */
require_once __DIR__ . '/config.php';

$filename = defined('LAUNCHER_FILENAME') ? LAUNCHER_FILENAME : 'GrizmMcLauncher_Setup_v1.0.1.exe';
$file = __DIR__ . '/downloads/' . $filename;

if (!file_exists($file)) {
    $altFile = __DIR__ . '/downloads/GrizmMcLauncher.exe';
    if (file_exists($altFile)) {
        $file = $altFile;
    } else {
        http_response_code(404);
        die('İndirme dosyası bulunamadı. Lütfen daha sonra tekrar deneyin.');
    }
}
header('Location: ' . (defined('LAUNCHER_DOWNLOAD_URL') ? LAUNCHER_DOWNLOAD_URL : 'downloads/' . $filename), true, 302);
exit;
