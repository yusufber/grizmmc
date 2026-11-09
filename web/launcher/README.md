# 🚀 GrizmMC Web Launcher (Three.js 3D & Otomatik Bağlantı)

Bu dizin, **GrizmMC Network** için geliştirilmiş **Three.js** tabanlı yeni nesil Web Minecraft Launcher'ını barındırır.

## ✨ Öne Çıkan Özellikler

1. **Three.js 3D İnteraktif Ortam:**
   * Prosedürel olarak oluşturulan 16x16 piksel Minecraft dokularına (Çimen, Taş, Elmas Cevheri, Obsidyen vb.) sahip yüzen 3D ada.
   * Canlı animasyonlu Nether Portalı ve partikül girdabı.
   * Nefes alan, kollarını sallayan 3D Steve oyuncu modeli.
   * Mouse hareketine duyarlı paralaks ve süzülme animasyonları.

2. **Otomatik Sunucu Bağlantısı (Auto-Connect):**
   * Sayfa açıldığında 5 saniyelik görsel geri sayım başlar.
   * Süre bittiğinde veya *"Hemen Bağlan"* tıklandığında:
     - `grizmmc://` ve `minecraft://` URI şemalarıyla yüklü istemciyi otomatik tetikler.
     - Sunucu adresini (`play-mc.grizm.tech:25565`) tek tıkla panoya kopyalar.
     - Oyuncuyu bilgilendiren bağlantı arayüzünü açar.
   * Geri sayımı dilediğiniz zaman durdurup devam ettirebilirsiniz.

3. **Tarayıcı İçi 3D Voxel Keşif Modu (In-Browser Play Mode):**
   * *"Tarayıcıda 3D Keşfet"* tıklandığında tam ekran birinci şahıs (FPS) Minecraft moduna geçer.
   * **W, A, S, D:** Hareket
   * **Boşluk:** Zıplama & yerçekimi fiziği
   * **Sol Tık:** Blok kırma
   * **Sağ Tık:** Blok yerleştirme
   * **1-5 Tuşları:** Hotbar'dan blok seçimi (Çimen, Taş, Toprak, Elmas, Obsidyen)

4. **Çoklu Alt Sunucu Desteği:**
   * **Ana Ağ (Proxy):** Port `25565`
   * **Merkez Lobi:** Port `25566`
   * **Grizm Towny:** Port `25567`
   * **Skyblock Void:** Port `25568`
   * **PvP Duels:** Port `25569`

5. **GrizmID & Web Audio Desteği:**
   * PHP oturumu açıksa oyuncu adını otomatik alır, yoksa `localStorage` üzerinde saklar.
   * Gerçek zamanlı oyuncu kafa avatarı entegrasyonu.
   * Dış ses dosyasına ihtiyaç duymayan sentetik Web Audio API ses efektleri (tıklama, geri sayım bipleri, fırlatma melodisi, blok kırma/koyma).

