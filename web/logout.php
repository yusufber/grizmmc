<?php
/**
 * GrizmMC Network - Çıkış Yap
 */

require_once __DIR__ . '/config.php';

unset($_SESSION['auth_user']);
$_SESSION['flash_success'] = 'Oturumunuz güvenli bir şekilde kapatıldı.';

header('Location: index.php');
exit;
