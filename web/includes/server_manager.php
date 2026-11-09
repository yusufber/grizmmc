<?php
/**
 * GrizmMC Network - Sunucu Yönetim Motoru (Server Manager)
 * Windows ve Linux ortamlarında süreç, RCON, screen, TCP port ve log denetimi sağlar.
 */

require_once __DIR__ . '/rcon.php';

class ServerManager {
    // Sunucu ana dizini (web klasöründen server/SONYEDEK dizinine erişim)
    private static function getBaseServerDir() {
        $paths = [
            __DIR__ . '/../../server/SONYEDEK',
            __DIR__ . '/../../server',
            '/home/minecraft/server/SONYEDEK',
            '/home/minecraft/server'
        ];
        foreach ($paths as $path) {
            if (is_dir($path)) {
                return realpath($path);
            }
        }
        return realpath(__DIR__ . '/../../server/SONYEDEK') ?: (__DIR__ . '/../../server/SONYEDEK');
    }

    // Tanımlı sunucular ve konfigürasyonları
    public static function getServers() {
        $baseDir = self::getBaseServerDir();
        return [
            'proxy' => [
                'name' => 'Proxy (Velocity)',
                'badge' => 'Ağ Geçidi',
                'type' => 'velocity',
                'port' => 25565,
                'rcon_port' => null,
                'rcon_password' => null,
                'dir' => $baseDir . DIRECTORY_SEPARATOR . 'proxy',
                'screen_name' => 'proxy',
                'start_bat' => 'start.bat',
                'start_sh' => 'start.sh',
                'description' => 'Tüm oyuncuların ilk bağlandığı ana Velocity proxy sunucusu.'
            ],
            'lobby' => [
                'name' => 'Lobby (Hub)',
                'badge' => 'Ana Merkez',
                'type' => 'paper',
                'port' => 25567,
                'rcon_port' => 25577,
                'rcon_password' => 'GrizmRcon2026!',
                'dir' => $baseDir . DIRECTORY_SEPARATOR . 'lobby',
                'screen_name' => 'lobby',
                'start_bat' => 'start.bat',
                'start_sh' => 'start.sh',
                'description' => 'Giriş lobisi, mod seçimi ve kozmetikler.'
            ],
            'smp' => [
                'name' => 'SMP (Towny Survival)',
                'badge' => '1.21.4 SMP',
                'type' => 'paper',
                'port' => 25566,
                'rcon_port' => 25576,
                'rcon_password' => 'GrizmRcon2026!',
                'dir' => $baseDir . DIRECTORY_SEPARATOR . 'smp',
                'screen_name' => 'smp',
                'start_bat' => 'start.bat',
                'start_sh' => 'start.sh',
                'description' => 'Towny, ekonomi, özel zindanlar ve survival deneyimi.'
            ],
            'pvp' => [
                'name' => 'PVP (Arena)',
                'badge' => 'Düello / FFA',
                'type' => 'paper',
                'port' => 25568,
                'rcon_port' => 25578,
                'rcon_password' => 'GrizmRcon2026!',
                'dir' => $baseDir . DIRECTORY_SEPARATOR . 'pvp',
                'screen_name' => 'pvp',
                'start_bat' => 'start.bat',
                'start_sh' => 'start.sh',
                'description' => '1.8 / 1.21 kombo, düello ve rekabetçi arena.'
            ],
            'skyblock' => [
                'name' => 'SkyBlock (OneBlock)',
                'badge' => 'Ada Hayatı',
                'type' => 'paper',
                'port' => 25569,
                'rcon_port' => 25579,
                'rcon_password' => 'GrizmRcon2026!',
                'dir' => $baseDir . DIRECTORY_SEPARATOR . 'skyblock',
                'screen_name' => 'skyblock',
                'start_bat' => 'start.bat',
                'start_sh' => 'start.sh',
                'description' => 'Gelişmiş ada seviyeleri, minyonlar ve tek blok modu.'
            ]
        ];
    }

    public static function isWindows() {
        return strtoupper(substr(PHP_OS, 0, 3)) === 'WIN';
    }

    /**
     * TCP Port açık mı kontrol eder (En hızlı ve güvenilir canlılık testi)
     */
    public static function checkPort($host, $port, $timeout = 1.0) {
        $fp = @fsockopen($host, $port, $errno, $errstr, $timeout);
        if ($fp) {
            fclose($fp);
            return true;
        }
        return false;
    }

    /**
     * Minecraft Server List Ping (Durum, oyuncu sayısı ve motd alma)
     */
    public static function pingMinecraftServer($host, $port, $timeout = 1.2) {
        $socket = @fsockopen($host, $port, $errno, $errstr, $timeout);
        if (!$socket) {
            return null;
        }
        stream_set_timeout($socket, $timeout);

        // Handshake Packet
        $hostLen = strlen($host);
        $handshake = "\x00\x04" . chr($hostLen) . $host . pack('n', $port) . "\x01";
        $packet = chr(strlen($handshake)) . $handshake;
        @fwrite($socket, $packet);

        // Status Request
        @fwrite($socket, "\x01\x00");

        // Read Response Length & Packet ID
        $len = self::readVarInt($socket);
        if ($len <= 0) {
            @fclose($socket);
            return null;
        }

        $id = self::readVarInt($socket);
        $jsonLen = self::readVarInt($socket);
        if ($jsonLen <= 0) {
            @fclose($socket);
            return null;
        }

        $jsonStr = '';
        $read = 0;
        while ($read < $jsonLen && !feof($socket)) {
            $buffer = @fread($socket, min(1024, $jsonLen - $read));
            if ($buffer === false || strlen($buffer) === 0) break;
            $jsonStr .= $buffer;
            $read += strlen($buffer);
        }
        @fclose($socket);

        $data = json_decode($jsonStr, true);
        if (!$data) return null;

        return [
            'online' => true,
            'players' => [
                'online' => (int)($data['players']['online'] ?? 0),
                'max' => (int)($data['players']['max'] ?? 0)
            ],
            'version' => $data['version']['name'] ?? '1.21.4',
            'description' => is_array($data['description'] ?? null) ? ($data['description']['text'] ?? '') : ($data['description'] ?? '')
        ];
    }

    private static function readVarInt($socket) {
        $value = 0;
        $size = 0;
        while (true) {
            $byte = @fread($socket, 1);
            if ($byte === false || strlen($byte) === 0) return 0;
            $b = ord($byte);
            $value |= ($b & 0x7F) << ($size++ * 7);
            if ($size > 5) return 0;
            if (($b & 0x80) !== 0x80) break;
        }
        return $value;
    }

    /**
     * Tek bir sunucunun anlık durumunu döner
     */
    public static function getServerStatus($key) {
        $servers = self::getServers();
        if (!isset($servers[$key])) return null;

        $srv = $servers[$key];
        $isPortOpen = self::checkPort('127.0.0.1', $srv['port'], 0.8);
        $pingData = null;

        if ($isPortOpen) {
            $pingData = self::pingMinecraftServer('127.0.0.1', $srv['port'], 1.0);
        }

        // Screen veya PID kontrolü (Linux ve Windows için)
        $processRunning = false;
        if (!self::isWindows()) {
            $screenCheck = @shell_exec("screen -list 2>/dev/null | grep -E '\." . escapeshellarg($srv['screen_name']) . "\b'");
            $processRunning = !empty($screenCheck);
        }

        $isOnline = $isPortOpen || $processRunning;

        return [
            'key' => $key,
            'name' => $srv['name'],
            'badge' => $srv['badge'],
            'type' => $srv['type'],
            'port' => $srv['port'],
            'rcon_port' => $srv['rcon_port'],
            'rcon_available' => !empty($srv['rcon_port']),
            'is_online' => $isOnline,
            'port_open' => $isPortOpen,
            'process_running' => $processRunning,
            'players' => $pingData ? $pingData['players'] : ['online' => 0, 'max' => 0],
            'version' => $pingData ? $pingData['version'] : '1.21.4',
            'description' => $srv['description'],
            'last_check' => date('H:i:s')
        ];
    }

    /**
     * Tüm sunucuların durumunu döner
     */
    public static function getAllStatuses() {
        $servers = self::getServers();
        $statuses = [];
        $totalOnlinePlayers = 0;

        foreach ($servers as $key => $srv) {
            $status = self::getServerStatus($key);
            $statuses[$key] = $status;
            if ($status['is_online'] && $key !== 'proxy') {
                $totalOnlinePlayers += $status['players']['online'];
            }
        }

        return [
            'servers' => $statuses,
            'system' => self::getSystemResources(),
            'total_players' => $totalOnlinePlayers,
            'timestamp' => time()
        ];
    }

    /**
     * Sunucu Başlatma
     */
    public static function startServer($key) {
        $servers = self::getServers();
        if (!isset($servers[$key])) {
            return ['success' => false, 'message' => "Sunucu bulunamadı: $key"];
        }

        $srv = $servers[$key];
        $current = self::getServerStatus($key);
        if ($current['is_online']) {
            return ['success' => false, 'message' => "{$srv['name']} zaten çalışıyor!"];
        }

        $dir = $srv['dir'];
        if (!is_dir($dir)) {
            return ['success' => false, 'message' => "Sunucu dizini bulunamadı: $dir"];
        }

        if (self::isWindows()) {
            $batFile = $dir . DIRECTORY_SEPARATOR . $srv['start_bat'];
            if (!file_exists($batFile)) {
                return ['success' => false, 'message' => "Başlatma dosyası bulunamadı: $batFile"];
            }

            // Windows arka planda veya yeni cmd penceresinde başlatma
            $command = "start \"GrizmMC - {$srv['name']}\" /d \"{$dir}\" cmd /c \"{$srv['start_bat']}\"";
            pclose(popen($command, "r"));
            return ['success' => true, 'message' => "{$srv['name']} başlatıldı (Windows CMD penceresi)."];
        } else {
            // Linux screen ile başlatma
            $shFile = $dir . DIRECTORY_SEPARATOR . $srv['start_sh'];
            if (!file_exists($shFile)) {
                return ['success' => false, 'message' => "Başlatma dosyası bulunamadı: $shFile"];
            }

            @chmod($shFile, 0755);
            $screenName = escapeshellcmd($srv['screen_name']);
            $cmd = "cd " . escapeshellarg($dir) . " && screen -dmS {$screenName} bash {$srv['start_sh']}";
            exec($cmd, $out, $ret);

            return ['success' => ($ret === 0), 'message' => "{$srv['name']} screen oturumunda başlatıldı."];
        }
    }

    /**
     * Sunucu Durdurma (RCON ile güvenli save ve stop, yoksa screen/process kill)
     */
    public static function stopServer($key) {
        $servers = self::getServers();
        if (!isset($servers[$key])) {
            return ['success' => false, 'message' => "Sunucu bulunamadı: $key"];
        }

        $srv = $servers[$key];
        $stoppedViaRcon = false;

        // 1. RCON ile komut göndermeyi dene
        if (!empty($srv['rcon_port']) && !empty($srv['rcon_password'])) {
            $rcon = new MinecraftRcon('127.0.0.1', $srv['rcon_port'], $srv['rcon_password'], 2);
            if ($rcon->connect()) {
                $rcon->sendCommand('say §c[Yonetim] Sunucu 3 saniye icinde kapatiliyor...');
                $rcon->sendCommand('save-all');
                usleep(500000);
                $rcon->sendCommand('stop');
                $stoppedViaRcon = true;
            }
        } elseif ($srv['type'] === 'velocity') {
            // Velocity için RCON yoktur, Linux'ta screen stuff end gönderilir
            if (!self::isWindows()) {
                exec("screen -S {$srv['screen_name']} -X stuff \"end\n\" 2>/dev/null");
                $stoppedViaRcon = true;
            }
        }

        // 2. Linux ise screen oturumunu temizle
        if (!self::isWindows()) {
            usleep(1500000);
            exec("screen -S {$srv['screen_name']} -X quit 2>/dev/null");
            exec("pkill -f '{$srv['screen_name']}/server.jar\|{$srv['screen_name']}/proxy.jar' 2>/dev/null");
        }

        return [
            'success' => true,
            'message' => "{$srv['name']} durdurma komutu gönderildi" . ($stoppedViaRcon ? " (RCON/Screen ile)." : ".")
        ];
    }

    /**
     * Sunucu Yeniden Başlatma
     */
    public static function restartServer($key) {
        self::stopServer($key);
        sleep(2);
        return self::startServer($key);
    }

    /**
     * Tüm Sunucuları Başlatma
     */
    public static function startAll() {
        $baseDir = self::getBaseServerDir();
        $results = [];

        if (self::isWindows()) {
            $bat = $baseDir . DIRECTORY_SEPARATOR . 'start_all.bat';
            if (file_exists($bat)) {
                pclose(popen("start \"\" /d \"{$baseDir}\" cmd /c start_all.bat", "r"));
                return ['success' => true, 'message' => 'Tüm sunucular start_all.bat ile başlatıldı.'];
            }
        } else {
            $sh = $baseDir . DIRECTORY_SEPARATOR . 'start_all.sh';
            if (file_exists($sh)) {
                @chmod($sh, 0755);
                exec("cd " . escapeshellarg($baseDir) . " && bash start_all.sh > /dev/null 2>&1 &", $out, $ret);
                return ['success' => true, 'message' => 'Tüm sunucular start_all.sh ile screen ortamında başlatıldı.'];
            }
        }

        // Dosya yoksa tek tek başlat
        foreach (array_keys(self::getServers()) as $sKey) {
            $results[$sKey] = self::startServer($sKey);
            usleep(500000);
        }

        return ['success' => true, 'message' => 'Tüm sunucular sırayla başlatıldı.', 'details' => $results];
    }

    /**
     * Tüm Sunucuları Durdurma
     */
    public static function stopAll() {
        $baseDir = self::getBaseServerDir();

        if (!self::isWindows()) {
            $sh = $baseDir . DIRECTORY_SEPARATOR . 'stop_all.sh';
            if (file_exists($sh)) {
                @chmod($sh, 0755);
                exec("cd " . escapeshellarg($baseDir) . " && bash stop_all.sh > /dev/null 2>&1 &");
                return ['success' => true, 'message' => 'Tüm sunucular stop_all.sh ile güvenle durduruluyor.'];
            }
        }

        $results = [];
        foreach (array_keys(self::getServers()) as $sKey) {
            $results[$sKey] = self::stopServer($sKey);
        }

        return ['success' => true, 'message' => 'Tüm sunucular durduruldu.', 'details' => $results];
    }

    /**
     * Konsola Komut Gönderme (RCON veya Linux Screen Stuff)
     */
    public static function sendCommand($key, $command) {
        $servers = self::getServers();
        if (!isset($servers[$key])) {
            return ['success' => false, 'message' => "Sunucu bulunamadı: $key"];
        }

        $srv = $servers[$key];
        $command = trim($command);
        if (empty($command)) {
            return ['success' => false, 'message' => 'Komut boş olamaz.'];
        }

        // RCON varsa RCON ile gönder
        if (!empty($srv['rcon_port']) && !empty($srv['rcon_password'])) {
            $rcon = new MinecraftRcon('127.0.0.1', $srv['rcon_port'], $srv['rcon_password'], 3);
            if ($rcon->connect()) {
                $response = $rcon->sendCommand($command);
                if ($response !== false) {
                    $cleanResponse = self::cleanAnsi(trim($response));
                    return [
                        'success' => true,
                        'source' => 'rcon',
                        'command' => $command,
                        'output' => $cleanResponse ?: '[Komut başarıyla yürütüldü - çıktı yok]'
                    ];
                }
            }
        }

        // RCON yoksa veya Velocity ise Linux screen oturumuna yaz
        if (!self::isWindows()) {
            $screenName = escapeshellcmd($srv['screen_name']);
            $escapedCmd = addcslashes($command, '"\\$`');
            exec("screen -S {$screenName} -X stuff \"{$escapedCmd}\n\" 2>&1", $out, $ret);
            return [
                'success' => ($ret === 0),
                'source' => 'screen',
                'command' => $command,
                'output' => "Komut screen ({$srv['screen_name']}) oturumuna iletildi. Çıktıyı konsol loglarından izleyebilirsiniz."
            ];
        }

        return [
            'success' => false,
            'message' => "Bu sunucuya komut gönderilemedi (RCON bağlantısı kurulamadı)."
        ];
    }

    /**
     * Son Log Dosyası Satırlarını Okuma (latest.log)
     */
    public static function getLatestLogs($key, $lines = 80) {
        $servers = self::getServers();
        if (!isset($servers[$key])) {
            return ['success' => false, 'message' => 'Sunucu bulunamadı.'];
        }

        $srv = $servers[$key];
        $logFile = $srv['dir'] . DIRECTORY_SEPARATOR . 'logs' . DIRECTORY_SEPARATOR . 'latest.log';

        // Proxy veya normal log alternatifleri
        if (!file_exists($logFile)) {
            $logFile = $srv['dir'] . DIRECTORY_SEPARATOR . 'proxy.log';
        }

        if (!file_exists($logFile)) {
            return [
                'success' => true,
                'file_exists' => false,
                'logs' => ["[Henüz log dosyası oluşmadı: " . basename($logFile) . "]"]
            ];
        }

        $fileLines = self::tailCustom($logFile, $lines);
        $cleaned = array_map([__CLASS__, 'cleanAnsi'], $fileLines);

        return [
            'success' => true,
            'file_exists' => true,
            'file' => basename($logFile),
            'size' => filesize($logFile),
            'logs' => $cleaned
        ];
    }

    /**
     * Hızlı kuyruk okuma (tail)
     */
    private static function tailCustom($filepath, $lines = 100) {
        $f = @fopen($filepath, "rb");
        if ($f === false) return [];

        $buffer = '';
        $lineList = [];
        $chunkSize = 4096;
        $fileSize = filesize($filepath);
        $offset = $fileSize;

        while ($offset > 0 && count($lineList) <= $lines) {
            $readSize = min($chunkSize, $offset);
            $offset -= $readSize;
            fseek($f, $offset);
            $chunk = fread($f, $readSize);
            $buffer = $chunk . $buffer;
            $lineList = explode("\n", $buffer);
        }
        fclose($f);

        $lineList = array_filter(array_map('rtrim', $lineList), function($l) {
            return $l !== '';
        });

        return array_slice($lineList, -$lines);
    }

    /**
     * ANSI renk kodlarını ve garip karakterleri temizleme
     */
    private static function cleanAnsi($text) {
        $text = preg_replace('/\x1b\[[0-9;]*m/', '', $text);
        $text = preg_replace('/§[0-9a-fk-or]/i', '', $text);
        return $text;
    }

    /**
     * Sistem Kaynakları (CPU & RAM)
     */
    public static function getSystemResources() {
        $info = [
            'os' => PHP_OS,
            'cpu_usage' => 'N/A',
            'ram_usage' => 'N/A',
            'ram_total' => 'N/A',
            'ram_free' => 'N/A',
            'uptime' => 'N/A'
        ];

        if (self::isWindows()) {
            // Windows Bellek Tespiti
            @exec("wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /Value 2>nul", $wmicOut);
            $total = 0; $free = 0;
            if (!empty($wmicOut)) {
                foreach ($wmicOut as $line) {
                    if (strpos($line, 'TotalVisibleMemorySize=') === 0) {
                        $total = (float)trim(substr($line, 23)) / 1024; // MB
                    }
                    if (strpos($line, 'FreePhysicalMemory=') === 0) {
                        $free = (float)trim(substr($line, 19)) / 1024; // MB
                    }
                }
            }
            if ($total > 0) {
                $used = $total - $free;
                $pct = round(($used / $total) * 100, 1);
                $info['ram_usage'] = "{$pct}% (" . round($used / 1024, 1) . " GB / " . round($total / 1024, 1) . " GB)";
                $info['ram_pct'] = $pct;
            }

            // Windows CPU Tespiti
            @exec("wmic cpu get loadpercentage /Value 2>nul", $cpuOut);
            if (!empty($cpuOut)) {
                foreach ($cpuOut as $line) {
                    if (strpos($line, 'LoadPercentage=') === 0) {
                        $info['cpu_usage'] = trim(substr($line, 15)) . '%';
                        break;
                    }
                }
            }
        } else {
            // Linux Bellek ve CPU Tespiti
            if (file_exists('/proc/meminfo')) {
                $mem = @file_get_contents('/proc/meminfo');
                if ($mem) {
                    preg_match('/MemTotal:\s+(\d+)/', $mem, $mt);
                    preg_match('/MemAvailable:\s+(\d+)/', $mem, $ma);
                    if (!empty($mt[1]) && !empty($ma[1])) {
                        $total = (float)$mt[1] / 1024;
                        $available = (float)$ma[1] / 1024;
                        $used = $total - $available;
                        $pct = round(($used / $total) * 100, 1);
                        $info['ram_usage'] = "{$pct}% (" . round($used / 1024, 1) . " GB / " . round($total / 1024, 1) . " GB)";
                        $info['ram_pct'] = $pct;
                    }
                }
            }

            if (file_exists('/proc/loadavg')) {
                $load = @file_get_contents('/proc/loadavg');
                if ($load) {
                    $parts = explode(' ', trim($load));
                    $info['cpu_usage'] = "Yük: " . ($parts[0] ?? '0.00') . " (1dk)";
                }
            }
        }

        return $info;
    }
}

