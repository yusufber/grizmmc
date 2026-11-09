<?php
/**
 * GrizmMC Network - Çıkış Yap
 */

require_once __DIR__ . '/config.php';

$_SESSION = [];
session_regenerate_id(true);
$_SESSION['csrf_token'] = bin2hex(random_bytes(32));
$_SESSION['flash_success'] = 'Oturumunuz güvenli bir şekilde kapatıldı.';

header('Location: index.php');
exit;
