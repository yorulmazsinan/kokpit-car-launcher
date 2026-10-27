# Kokpit

**Android araç multimedya cihazları için açık kaynak ana ekran.** "Android CarPlay'li
multimedya" diye satılan ucuz MediaTek cihazlar (MT8227L, Android 8.1) için yazıldı; bu
cihazların ana ekranı eski görünüyor ve bazı sistem ayarları gizleniyor.

[English README](README.md)

![1024×600 ana ekran](docs/screenshots/home-1024x600.png)

## Neler yapar

- **Ana ekran** – büyük saat ve tarih, hava durumu (Open-Meteo, anahtar gerekmez),
  takvimdeki sıradaki etkinlik, çalan müzik ve kontrolleri, GPS hızı, yön ve yolculuk
  ortalama hızı; altta altı büyük düğme: CarPlay, Bluetooth, Telefon, Radyo, Harita,
  Uygulamalar.
- **Gizlenen ayarlar geri gelir** – ekran kilidini değiştirme veya kaldırma, tüm Android
  ayarları, geliştirici seçenekleri, varsayılan uygulamalar, tarih ve saat, dil ve
  cihazın gizli ekranları için etkinlik gezgini.
- **Uyanınca** (kontak açılınca) – Wi-Fi'ı açıp telefonunuzun erişim noktasına bağlanır;
  iPhone takılınca CarPlay'i açabilir; kendi açılış videonuzu oynatır.
- **Teşhis** – cihazın gerçek Android sürümü, ekran ölçüsü, Wi-Fi bantları ve üretici
  paketleri tek ekranda; hata bildirimine yapıştırmak için.

Kokpit bir ana ekran uygulamasıdır, cihaz yazılımı değildir. Radyo, Bluetooth ile arama,
geri görüş kamerası, direksiyon tuşları ve CarPlay cihazın kendi uygulamalarında kalır;
Kokpit onları açar. Root gerekmez; sistem veya MCU yazılımına dokunulmaz.

## Desteklenen cihazlar

| | |
|---|---|
| Test edilen | MT8227L, Android 8.1 ("Android 11" diye satılır), 1024×600 |
| En iyi çaba | Diğer yatay Android 8.1–13 multimedya cihazları, 800×480 ile 1920×720 arası |

Android 10 ve sonrasında uygulamalar Wi-Fi'ı kendisi açamıyor: Kokpit erişim noktanızı
önerilen ağ olarak ekler, Wi-Fi kapalıysa bildirim gösterir.

## USB bellekle kurulum

1. [Releases](https://github.com/yorulmazsinan/kokpit/releases) sayfasından
   `kokpit-<sürüm>.apk` dosyasını indirip USB belleğe kopyalayın.
2. Belleği cihaza takın, dosya yöneticisinden APK'ya dokunun; sorulursa "bilinmeyen
   kaynaklara" izin verin.
3. Kokpit'i bir kez açıp konum ve takvim iznini verin.

### Ana ekran yapmak — ve geri almak

- Ev tuşuna basıp **Kokpit → Her zaman** seçin ya da Android ayarları → Uygulamalar →
  Varsayılan uygulamalar → Ana ekran uygulaması.
- Geri almak için: Kokpit → **Uygulamalar**'a uzun basın → Ayarlar → Sistem Araçları →
  **Üretici Ana Ekranına Dön**; sonra aynı yoldan üretici ekranını varsayılan seçin.
- Bazı cihazlarda ev tuşu üreticinin ekranına sabitlenmiştir. Sizinki öyleyse
  Ayarlar → Teşhis'ten raporu kaydedip onunla bir issue açın.

## Kurulum ayarları

- **Ekran kilidini kaldırma:** Ayarlar → Sistem Araçları → *Ekran Kilidini Değiştir veya
  Kaldır*, mevcut PIN'i girin, *Yok*'u seçin.
- **Ana ekranda müzik:** müzik kartına dokunup Kokpit'e bildirim erişimi verin (Android
  çalan şarkıyı yalnız bu izni olan uygulamalarla paylaşır).
- **iCloud takvimi:** account.apple.com'dan uygulamaya özel parola oluşturun,
  [DAVx⁵](https://www.davx5.com/)'i kurun, `https://caldav.icloud.com` adresi, Apple
  Kimliği e-postanız ve bu parolayla hesap ekleyip takvimlerinizi seçin. Google Takvim için
  ek adım yok.
- **iPhone erişim noktası:** *Başkalarının Katılmasına İzin Ver* ve *Uyumluluğu En Üst
  Düzeye Çıkar* açık olsun (bu cihazların çoğu yalnız 2.4 GHz görür); sonra erişim noktası
  adı ve parolasını Ayarlar → Bağlantı'ya girin.
- **Açılış videosu:** Ayarlar → Açılış Videosu → *USB'den Video Ekle*. Videolar cihaza
  kopyalanır, USB çıkarılabilir. MP4 (H.264), en fazla 20 MB; 1024×600 ve 3–8 saniye en
  iyi sonucu verir. Birini seçin, sırayla ya da rastgele oynatın veya kapatın.
- **Asistan düğmesi:** şimdilik ona atadığınız uygulamayı açar (örneğin ChatGPT).
  Yerleşik sesli asistan planlanıyor.

## Gizlilik

Kokpit internete tek tür istek gönderir: hava durumu için yaklaşık konumunuzu
[Open-Meteo](https://open-meteo.com/)'ya. Analitik, reklam ve hesap yok. Erişim noktası
parolası Android Keystore ile şifrelenir.

`QUERY_ALL_PACKAGES` izni, ana ekranın tüm uygulamaları listeleyip açabilmesi için gerekir.

## Derleme

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

JDK 17–21. `keystore.properties` (veya `~/.config/kokpit/keystore.properties`) varsa
yayın derlemesi imzalanır.

## Lisans

[Apache Lisansı 2.0](LICENSE)
