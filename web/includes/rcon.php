<?php
/**
 * GrizmMC Network - PHP Source RCON Client
 * Minecraft sunucularına RCON protokolü üzerinden komut gönderme ve çıktı alma.
 */

class MinecraftRcon {
    private $host;
    private $port;
    private $password;
    private $timeout;
    private $socket = null;
    private $authorized = false;
    private $lastError = '';

    const SERVERDATA_AUTH = 3;
    const SERVERDATA_AUTH_RESPONSE = 2;
    const SERVERDATA_EXECCOMMAND = 2;
    const SERVERDATA_RESPONSE_VALUE = 0;

    public function __construct($host = '127.0.0.1', $port = 25575, $password = '', $timeout = 3) {
        $this->host = $host;
        $this->port = (int)$port;
        $this->password = $password;
        $this->timeout = $timeout;
    }

    public function __destruct() {
        $this->disconnect();
    }

    public function connect() {
        if ($this->socket) {
            return true;
        }

        $this->socket = @fsockopen($this->host, $this->port, $errno, $errstr, $this->timeout);
        if (!$this->socket) {
            $this->lastError = "Bağlantı hatası: $errstr ($errno)";
            return false;
        }

        stream_set_timeout($this->socket, $this->timeout);
        return $this->authorize();
    }

    public function disconnect() {
        if ($this->socket) {
            @fclose($this->socket);
            $this->socket = null;
        }
        $this->authorized = false;
    }

    private function authorize() {
        if (!$this->socket) return false;

        $id = 1;
        $this->writePacket($id, self::SERVERDATA_AUTH, $this->password);
        $packet = $this->readPacket();

        if ($packet && $packet['id'] === $id && $packet['type'] === self::SERVERDATA_AUTH_RESPONSE) {
            $this->authorized = true;
            return true;
        }

        $this->lastError = "RCON yetkilendirme reddedildi veya şifre hatalı.";
        $this->disconnect();
        return false;
    }

    public function sendCommand($command) {
        if (!$this->authorized) {
            if (!$this->connect()) {
                return false;
            }
        }

        $id = rand(10, 9999);
        $this->writePacket($id, self::SERVERDATA_EXECCOMMAND, $command);
        $packet = $this->readPacket();

        if ($packet && $packet['id'] === $id) {
            return $packet['body'];
        }

        return false;
    }

    public function getLastError() {
        return $this->lastError;
    }

    public function isConnected() {
        return $this->socket !== null && $this->authorized;
    }

    private function writePacket($id, $type, $body) {
        $data = pack("VV", $id, $type) . $body . "\x00\x00";
        $length = strlen($data);
        $packet = pack("V", $length) . $data;
        return @fwrite($this->socket, $packet, strlen($packet));
    }

    private function readPacket() {
        if (!$this->socket) return null;

        $sizeData = @fread($this->socket, 4);
        if (strlen($sizeData) < 4) return null;

        $size = unpack("V1size", $sizeData)['size'];
        if ($size <= 0 || $size > 40960) return null;

        $packetData = '';
        $remaining = $size;
        while ($remaining > 0 && !feof($this->socket)) {
            $chunk = @fread($this->socket, min($remaining, 4096));
            if ($chunk === false || strlen($chunk) === 0) break;
            $packetData .= $chunk;
            $remaining -= strlen($chunk);
        }

        if (strlen($packetData) < 8) return null;

        $header = unpack("V1id/V1type", substr($packetData, 0, 8));
        $body = substr($packetData, 8, -2);

        return [
            'id' => $header['id'],
            'type' => $header['type'],
            'body' => $body
        ];
    }
}

