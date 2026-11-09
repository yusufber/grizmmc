<?php
/**
 * GrizmMC Network - GrizmID OAuth2 Callback Handler
 * GrizmID auth sunucusundan dönen yetkilendirme kodunu doğrular ve oturum açar.
 */

require_once __DIR__ . '/config.php';

// Kullanıcı yetkilendirmeyi reddettiyse
if (isset($_GET['msg']) && $_GET['msg'] === 'auth_denied') {
    $_SESSION['flash_error'] = 'GrizmID giriş yetkilendirmesi iptal edildi.';
    header('Location: index.php');
    exit;
}

// Kod parametresi kontrolü
$code = $_GET['code'] ?? '';

if (empty($code)) {
    $_SESSION['flash_error'] = 'Yetkilendirme kodu bulunamadı. Lütfen tekrar giriş yapın.';
    header('Location: index.php');
    exit;
}

// Verify endpoint URL
$verifyUrl = rtrim(AUTH_SERVER_URL, '/') . '/verify.php';

$postFields = [
    'code' => $code,
    'client_secret' => AUTH_CLIENT_SECRET
];

$response = null;

if (function_exists('curl_init')) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $verifyUrl);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($postFields));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    $response = curl_exec($ch);
    curl_close($ch);
} else {
    $context = stream_context_create([
        'http' => [
            'method' => 'POST',
            'header' => "Content-Type: application/x-www-form-urlencoded\r\n",
            'content' => http_build_query($postFields),
            'timeout' => 10
        ]
    ]);
    $response = @file_get_contents($verifyUrl, false, $context);
}

if (!empty($response)) {
    $data = json_decode($response, true);
    if (isset($data['status']) && $data['status'] === 'success' && isset($data['user'])) {
        $email = $data['user']['email'];
        $fullName = $data['user']['full_name'];

        // Kullanıcının kayıtlı profili var mı?
        $existingProfile = get_user_profile($email);

        if ($existingProfile && !empty($existingProfile['minecraft_username'])) {
            // Daha önce kullanıcı adı belirlemiş
            $_SESSION['auth_user'] = [
                'full_name' => $fullName,
                'email' => $email,
                'minecraft_username' => $existingProfile['minecraft_username'],
                'logged_in_at' => time()
            ];
            unset($_SESSION['needs_username']);

            if (!empty($_SESSION['auth_source']) && $_SESSION['auth_source'] === 'launcher') {
                render_launcher_handoff($existingProfile['minecraft_username'], $email, $fullName);
                exit;
            }

            $_SESSION['flash_success'] = 'GrizmID ile giriş yapıldı! Tekrar hoş geldin, ' . htmlspecialchars($existingProfile['minecraft_username']) . '!';
            header('Location: profile.php');
            exit;
        } else {
            // İLK GİRİŞ! Kullanıcı adını belirlemesi gerekiyor
            $_SESSION['auth_user'] = [
                'full_name' => $fullName,
                'email' => $email,
                'minecraft_username' => null,
                'logged_in_at' => time()
            ];
            $_SESSION['needs_username'] = true;
            header('Location: set-username.php');
            exit;
        }
    } else {
        $errorMsg = $data['message'] ?? 'Kod doğrulanamadı veya süresi doldu.';
        $_SESSION['flash_error'] = 'Giriş Başarısız: ' . $errorMsg;
        header('Location: index.php');
        exit;
    }
} else {
    $_SESSION['flash_error'] = 'Kimlik doğrulama sunucusuna ulaşılamadı. Lütfen daha sonra tekrar deneyin.';
    header('Location: index.php');
    exit;
}
