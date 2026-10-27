# Kokpit — Android multimedya cihazı için açık kaynak ana ekran

Tarih: 2026-10-27 · Durum: taslak, onay bekliyor

## 1. Amaç ve neden

Chevrolet Captiva 2011'e takılmış Çin üretimi bir Android multimedya cihazı var. Cihazla gelen
ana ekran kalitesiz görünüyor, üretici bazı sistem ayarlarını gizlemiş (ör. konulan ekran
kilidi kaldırılamıyor) ve günlük kullanımda gereken şeyler (CarPlay, Bluetooth, radyo, hava
durumu) tek bakışta ve tek dokunuşta değil.

Kokpit, bu cihazın ana ekranının (launcher) yerine geçen, sıfırdan yazılmış, açık kaynak bir
Android uygulamasıdır. Hedef: arabaya binince ekrana bakıldığında ihtiyaç duyulan her şeyin
temiz, okunaklı, hızlı bir arayüzde olması; üreticinin sakladığı ayarlara yeniden erişim.

Proje GitHub'da açık kaynak yayımlanacak; aynı platformdaki (MT8227L ve benzeri Android
multimedya cihazları) başka kullanıcılar da kurabilmeli.

## 2. Hedef cihaz (fotoğraflardan tespit edildi)

| Özellik | Değer |
|---|---|
| İşlemci | MediaTek MT8227L (4 çekirdek Cortex-A7, 32-bit, `armeabi-v7a`) |
| Gerçek Android | **8.1 Oreo (API 27)** — derleme `alps-mp-o1.mp5`, çekirdek 3.18.79. Ayarlarda "Android 11" yazıyor ama sahte. |
| RAM / depolama | 2 GB / ~16 GB (10,5 GB boş) |
| MCU | `TS907.190518(H)`, HMI `TSKJ.D.O.F3.C04.2021.06.01` |
| Bluetooth | Ayrı modül, `BT.21.05.27.1500` |
| Ekran | Muhtemelen 1024×600 yatay (cihazda doğrulanacak) |
| Wi-Fi | Muhtemelen yalnız 2.4 GHz (cihazda doğrulanacak) |
| Play Store | Var |
| CarPlay | Yalnız kablolu, üretici uygulamasıyla (muhtemelen ZLink) |
| Araç | Captiva 2011; aracın kendi göstergesi ortalama yakıtı zaten gösteriyor |

Kullanıcının telefonu iPhone.

## 3. Hedefler

1. **Ana ekran** (onaylı taslak, aşağıda §5): saat + tarih, hava durumu, takvimdeki sıradaki
   etkinlik, çalan medya ve kontrolleri, GPS hız / yön / ortalama hız kartı, altta altı büyük
   kısayol düğmesi.
2. **Gizli ayarlara erişim**: ekran kilidini kaldırma başta olmak üzere üreticinin menüden
   sakladığı sistem ekranlarını açan bir bölüm.
3. **Arabaya binince otomatik bağlanma**: cihaz uyandığında Wi-Fi'ı açıp kullanıcının telefon
   hotspot'una bağlanmak; telefon USB'ye takılınca CarPlay'in açılması.
4. **Uygulama çekmecesi**: kurulu tüm uygulamalar, temiz bir ızgarada.
5. **Cihaz teşhis ekranı**: ekran çözünürlüğü/yoğunluğu, Wi-Fi'ın 5 GHz desteği, Bluetooth'un
   Android'e açık olup olmadığı, kurulu üretici paketleri. Hem geliştirme sırasında keşif için
   hem de başka cihazlardaki kullanıcıların hata bildirimi için.
6. **Açık kaynak yayın**: GitHub'da kaynak kod + Releases'te imzalı APK, Türkçe ve İngilizce
   arayüz, README'de USB bellekle kurulum anlatımı.

## 4. Hedef dışı ve kaçınılacaklar

**Hedef dışı (yapılmayacak):**
- OBD / anlık yakıt tüketimi. (Kullanıcı vazgeçti; adaptör sürekli takılı kalmayacak.)
- Radyo, Bluetooth telefon/müzik, geri görüş kamerası, direksiyon tuşları, EQ, CarPlay'in
  yeniden yazılması. Bunlar üretici uygulamalarında ve MCU'da kalır; Kokpit yalnız onları açar.
- Cihaz yazılımı (ROM) veya MCU yazılımı değiştirmek. Root gerektiren hiçbir özellik.
- Kablosuz CarPlay. Cihazda 5 GHz Wi-Fi yoksa yazılımla mümkün değil; harici adaptör
  (ör. Carlinkit Autokit) alınırsa Kokpit yalnızca onun uygulamasını açacak şekilde
  yapılandırılabilir olmalı.
- Video oynatıcı ve sürüşte video kısıtlamasını (el freni sinyali) aşmaya yönelik herhangi bir şey.

**Kaçınılacaklar (çalışsa bile başarısızlık sayılır):**
- Takılan, gecikmeli açılan arayüz. Cihaz zayıf; ana ekran anında tepki vermeli.
- Sürüş sırasında okunamayan küçük yazılar, küçük dokunma alanları, dikkat dağıtan animasyonlar.
- Cihazı açılmaz hale getirebilecek ya da kullanıcıyı ana ekransız bırakabilecek bir durum.
  Kokpit çökse bile kullanıcı üretici ana ekranına dönebilmeli.
- Kişisel veriyi dışarı gönderen herhangi bir şey (analitik, reklam, izleme yok).

## 5. Ana ekran (kullanıcının onayladığı taslak)

Yatay, koyu tema, 1024×600 hedef. Üstte cihazın kendi sistem çubuğu kalıyor (ev, saat,
parlaklık, geri); Kokpit onun altındaki alanı kullanır.

```
┌────────────────────────────────┬──────────────────────────┐
│ 15:02                          │ [kapak] Şarkı adı        │
│ Salı, 27 Ekim                  │         Sanatçı · Kaynak │
│                                │ ─────────────            │
│ ☁ 18°  Parçalı bulutlu         │   ⏮      ⏯      ⏭       │
│   En yüksek 21° / en düşük 12° ├──────────────────────────┤
│ ────────────────────────────── │ 62 km/s · GPS     ↖ KB   │
│ 📅 16:30 Müşteri toplantısı    │ Ortalama 48 km/s         │
├──────┬──────┬──────┬──────┬────┴──┬────────────┬─────────┤
│CarPlay│Bluetooth│Telefon│Radyo│ Harita │ Uygulamalar       │
└───────────────────────────────────────────────────────────┘
```

- **Saat kartı**: büyük saat, Türkçe tarih (cihaz dili İngilizceyse İngilizce).
- **Hava durumu**: anlık sıcaklık, durum, günün en yüksek/en düşük değeri. Kaynak Open-Meteo
  (ücretsiz, anahtarsız). Konum cihaz GPS'inden. İnternet yoksa son alınan veri, alındığı
  saatle birlikte gösterilir; hiç veri yoksa kart sade bir "Hava durumu için internet gerekli"
  durumuna geçer.
- **Takvim**: cihazdaki Android takvim sağlayıcısından (`CalendarContract`) bugünün sıradaki
  etkinliği ve kalan süre. Etkinlik yoksa satır gizlenir. İzin verilmemişse izin isteyen bir
  satır. Kokpit hiçbir takvim hesabına kendisi bağlanmaz; cihazda hangi takvim eşitleniyorsa onu
  okur. Kullanıcı iCloud kullanıyor ve cihazda hesap açmayı kabul ediyor: iCloud takvimi cihaza
  **DAVx⁵** uygulaması ve Apple'ın "uygulamaya özel parola"sıyla eşitlenir. Kokpit'in ayarlarında
  ve README'de bu kurulum adım adım anlatılır (Google Takvim kullananlar için ek adım yok).
  *(Benim kararım: iCloud'a Kokpit içinden bağlanmak yerine hazır eşitleme uygulaması.)*
- **Çalan medya**: aktif medya oturumunun başlığı, sanatçısı, kapağı, kaynağı; önceki /
  oynat-duraklat / sonraki. Medya oturumu yayınlamayan üretici uygulamalarında kart boş
  durumda "Müzik çalmıyor" gösterir ve dokununca Bluetooth müzik uygulamasını açar.
- **Sürüş kartı** (taslaktaki "yol süresi" kaldırıldı, kullanıcı isteği):
  - GPS hızı (km/s) ve gidiş yönü (K, KD, D, GD, G, GB, B, KB).
  - **Ortalama hız**: bu yolculukta katedilen mesafe ÷ cihazın uyanık geçirdiği süre. Yolculuk,
    cihaz uyandığında başlar; cihaz 30 dakikadan uzun uyursa sıfırlanır. Kırmızı ışık ve
    trafikte durulan süre sayılır (aracın yol bilgisayarı gibi); cihazın uykuda geçirdiği süre
    (30 dakikadan kısa molalar) sayılmaz. *(Benim kararım.)* GPS kilidi yokken "GPS aranıyor".
  - Yakıt gösterilmez (aracın kendi ekranında var).
- **Alt sıra (dock)**: CarPlay (vurgulu), Bluetooth, Telefon, Radyo, Harita, Uygulamalar.
  Her düğmenin açtığı uygulama uzun basılarak değiştirilebilir (ör. CarPlay → Autokit,
  Harita → Yandex/Google Maps). İlk açılışta bilinen üretici paket adlarından tahmin edilir,
  bulunamazsa kullanıcıya seçtirilir.
- **Asistan düğmesi**: ana ekranda sabit, kolay ulaşılır bir mikrofon düğmesi (saat kartının
  köşesinde ya da dock'un yanında; yerleşim builder'ın). 1. aşamada düğme ayarlanmamışsa
  "Asistanı kurmak için dokun" der ya da kullanıcı isterse ona bir uygulama (ör. ChatGPT)
  atanabilir. Asistan §7a'da.
- **Tema**: varsayılan koyu. İsteğe bağlı "otomatik": gün doğumu/batımına göre açık/koyu
  (gün doğumu/batımı Open-Meteo yanıtından).

Okunabilirlik: kol mesafesinden okunacak; en küçük metin ~14sp karşılığı, dokunma alanları en
az 64dp. Görsel kalite çıtası: CarPlay Dashboard ve Android Auto'nun yeni ana ekranı — sakin,
düz yüzeyler, tek vurgu rengi.

## 6. Diğer ekranlar

- **Uygulama çekmecesi**: kurulu, başlatılabilir uygulamalar; alfabetik ızgara. Uzun basınca
  "uygulama bilgisi".
- **Ayarlar (Kokpit'in kendi ayarları)**: dock eşlemeleri, tema, otomatik bağlanma ayarları
  (§7), dil, hakkında.
- **Sistem araçları (gizli ayarlar)**:
  - Ekran kilidini değiştir/kaldır (`DevicePolicyManager.ACTION_SET_NEW_PASSWORD` — mevcut PIN
    sorulur, "Yok" seçilebilir).
  - Android ayarlarının tamamı, Wi-Fi, Bluetooth, uygulamalar, varsayılan uygulamalar,
    geliştirici seçenekleri, tarih ve saat, dil.
  - Üreticinin araç ayarları ve fabrika ayarları uygulaması (paket adı teşhisle bulunur).
  - "Etkinlik gezgini": `com.android.settings` ve üretici paketlerinin dışa açık
    etkinliklerini listeleyip açabilme (keşif ve ileri kullanıcılar için).
- **Teşhis**: §3.5'teki bilgiler + "panoya kopyala / dosyaya kaydet" ki kullanıcılar GitHub
  issue'suna yapıştırabilsin.

## 7. Arabaya binince otomatik davranışlar

Bu cihazlar kontak kapanınca tamamen kapanmaz, uykuya geçer; kontak açılınca uyanır.
Kokpit "uyandı" anını yakalar (ekran açılma yayını; üretici bir kontak/ACC yayını gönderiyorsa
o da kullanılır — teşhis sırasında bakılacak) ve şunları yapar:

1. **Wi-Fi → telefon hotspot'u**: Wi-Fi kapalıysa açar; ayarlarda seçilmiş hotspot ağına
   bağlanmaya çalışır, bağlanamazsa birkaç dakika boyunca belirli aralıklarla yeniden dener.
   (Android 8.1'de uygulamalar Wi-Fi'ı açıp kayıtlı ağa bağlanabiliyor.) Ayarlar ekranında
   iPhone için not: Kişisel Erişim Noktası'nda "Başkalarının katılmasına izin ver" açık ve
   cihaz 2.4 GHz ise "Uyumluluğu en üst düzeye çıkar" açık olmalı.
2. **CarPlay**: iPhone USB'ye takılınca CarPlay uygulaması kendisi açılmıyorsa Kokpit açar.
   (Üretici uygulaması zaten açıyorsa Kokpit karışmaz.) Ayarlardan kapatılabilir.
3. **Bluetooth telefona otomatik bağlanma**: Bluetooth ayrı bir modülde ve üretici
   uygulamasında yönetiliyor. Kokpit bu bağlantıyı doğrudan kontrol etmeyi **denemez**;
   üretici uygulamasındaki "otomatik bağlan" ayarına giden kısayolu sunar. Teşhis, modülün
   Android Bluetooth API'sine açık olduğunu gösterirse bu ileride ele alınır.

## 7b. Açılış videosu (1. aşama)

Kullanıcı aracının yapay zekâyla üretilmiş kısa bir videosunu açılışta görmek istiyor.

- Android'in gerçek açılış animasyonu (`bootanimation.zip`) sistem bölümünde; değiştirmek root
  ister, **yapılmaz**. Üreticinin fabrika ayarlarındaki açılış logosu ayrıca seçilebilir; Kokpit
  ona giden kısayolu verir.
- Bunun yerine Kokpit, **cihaz uyandığında** (kontak açılınca; bu cihazlar çoğunlukla soğuk
  açılmaz, uykudan uyanır) tam ekran, sessiz ya da sesli seçilebilen kısa bir video oynatır,
  bitince ana ekrana yumuşak geçer. Dokununca anında atlanır.
- Cihaz 30 dakikadan kısa uyuduysa (kısa mola) video oynatılmaz; ayarlardan "her uyanışta"
  yapılabilir. Ayarlardan tamamen kapatılabilir.
- Videolar USB bellekten **bir kez** Kokpit'in kendi depolamasına kopyalanır; USB sonra
  çıkarılır, takılı kalması gerekmez. Birden fazla video eklenebilir (kitaplık). Ayarlardan:
  hangi videonun oynatılacağı seçilir ya da "her uyanışta sırayla" / "rastgele"; tek tek
  silinebilir; özellik tamamen kapatılabilir. *(Kullanıcı isteği.)* Önerilen
  biçim README'de: MP4, H.264, 1024×600 (veya ekran oranına uygun), 3–8 saniye, en fazla ~20 MB.
  Biçim desteklenmezse video atlanır, ana ekran gecikmeden açılır.
- Geri vites kamerası MCU/üretici tarafından öne alınır; video bunu engellememeli. Oynatma
  `ExoPlayer` (Media3) ile, donanım çözücüyle.
- Videonun üretimi Kokpit'in işi değil; kullanıcı ayrıca üretecek.

## 7a. Sesli yapay zekâ asistanı (2. aşama)

Ana ekran (1. aşama) bitip cihazda doğrulandıktan sonra yapılır.

**Kullanım**: Mikrofon düğmesine basılır, kullanıcı Türkçe konuşur, soru seçilen yapay zekâya
gider, cevap Türkçe sesli okunur ve ekranda kısa metin olarak da görünür. Bir kez basınca bir
soru-cevap; cevaptan sonra birkaç saniye "devam etmek ister misin" diye dinlemeye devam edebilir.
Sürekli dinleyen uyandırma kelimesi **yok** (işlemciyi yorar, gizlilik).

**Sağlayıcılar**: OpenAI, Anthropic (Claude) ve Google (Gemini). Kullanıcı ayarlardan her biri
için kendi API anahtarını girer ve hangisinin kullanılacağını seçer. Anahtarlar cihazda Android
Keystore ile şifreli saklanır, hiçbir yere gönderilmez (yalnız ilgili sağlayıcının API'sine).
Model seçimi ayarlardan yapılır; liste her sağlayıcının model listeleme uç noktasından alınır.
Abonelik (ChatGPT Plus vb.) API'yi kapsamaz; README bunu açıkça yazar.

**Her sağlayıcı kendi resmî SDK'sıyla** çağrılır: Anthropic Java SDK (`com.anthropic`),
OpenAI Java SDK, Google Gen AI Java SDK. Ortak bir `AssistantProvider` arayüzü arkasında. Bir
SDK Android 8.1 / 32-bit'te çalışmazsa (ilk iş olarak doğrulanır) o sağlayıcı için doğrudan
REST çağrısına düşülür ve bu dokümana not edilir.

**Claude ayrıntıları**: varsayılan model `claude-opus-5-5`, `thinking: {type: "adaptive"}`,
`output_config.effort: "low"` (kısa sesli cevaplar için gecikmeyi düşük tutar). Web araması
sunucu aracı `web_search_20260209`. Reddedilme durumu için `fallbacks: "default"` (beta
`server-side-fallback-2026-07-01`) açık; `stop_reason` her zaman kontrol edilir. Zorunlu araç
seçimi (`tool_choice: any/tool`) kullanılmaz. Kullanıcı ayarlardan daha hızlı/ucuz bir Claude
modeli seçebilir.
OpenAI ve Gemini'de de varsayılan, o sağlayıcının o günkü önerilen genel modeli ve kendi
sunucu taraflı web arama aracı (builder sağlayıcı dokümanından güncelini alır).

**Araçlar (asistanın cihazda yapabildikleri)** — her biri üç sağlayıcıya aynı şemayla tanımlanır:
- Uygulama aç (CarPlay, radyo, Bluetooth müzik, harita, kurulu herhangi bir uygulama adıyla).
- Medya: oynat, duraklat, sonraki, önceki.
- Navigasyon başlat: bir adres/yer adına `geo:` / harita uygulamasının yol tarifi intent'i.
- Hava durumu ve sıradaki takvim etkinliğini söyle (Kokpit'in zaten sahip olduğu veriden).
- Wi-Fi'ı aç/kapat, hotspot'a bağlan.
- Saat/tarih, GPS hızı ve ortalama hız gibi ekrandaki bilgileri söyle.

Telefon araması, mesaj gönderme, ayar değiştirme gibi geri alınması zor işler **yapılmaz**
(arama Bluetooth modülünde, bize kapalı). Yapılan her işlem kısa sesli onayla bildirilir.

**Ses**: konuşmayı yazıya Android `SpeechRecognizer` (cihazdaki Google uygulaması), cevabı sese
`TextToSpeech` (Türkçe ses paketi). Cihazda Türkçe tanıma/ses paketi yoksa Ayarlar ekranı bunu
söyler ve kurulum sayfasını açar.

**Sistem talimatı**: araçta, sürüş halinde, sesle cevap verildiği; cevapların bir iki cümle,
madde işaretsiz, okunması kolay Türkçe olması gerektiği.

**İleride (kapsam dışı ama tasarım engellememeli)**: kullanıcının kendi uzak MCP sunucularını
(ör. Enmera sitelerinin MCP'si) asistana bağlamak; direksiyondaki ses tuşunu asistana atamak.

**Gizlilik notu**: §4'teki "veri dışarı gitmez" kuralının tek istisnası budur: yalnızca düğmeye
basıldığında, yalnızca söylenen soru ve gerekli bağlam, yalnızca kullanıcının seçtiği sağlayıcıya
gider. Ayarlarda bu açıkça yazar.

## 8. Teknik kararlar (benim kararlarım)

- **Dil ve arayüz**: Kotlin + Jetpack Compose. Tek modüllü Android uygulaması.
- **minSdk 27** (cihaz Android 8.1), **targetSdk 35**, `armeabi-v7a` ve `arm64-v8a` uyumlu
  (saf Kotlin, yerel kütüphane yok).
- **Performans**: release derlemesi R8 ile küçültülür; baseline profile eklenir. İlk sürümün
  ilk işi cihazda açılış ve kaydırma akıcılığını ölçmek. Compose bu işlemcide yetersiz kalırsa
  ana ekran Android View sistemiyle yeniden yazılır (bu bir karar noktası; kullanıcıya
  bildirilir).
- **Bağımlılıklar az**: Compose, kotlinx-coroutines, kotlinx-serialization, OkHttp (hava
  durumu), Media3 ExoPlayer (açılış videosu); 2. aşamada üç sağlayıcının SDK'ları. Google Play Services kullanılmaz (konum için `LocationManager`), böylece
  F-Droid'e de uygun kalır.
- **Medya**: `NotificationListenerService` izni + `MediaSessionManager.getActiveSessions`.
- **Kalıcı ayarlar**: DataStore.
- **Kod dili**: tüm tanımlayıcılar İngilizce; kullanıcıya görünen metinler `strings.xml`'de
  Türkçe (`values-tr`) ve İngilizce (`values`).
- **Lisans**: Apache-2.0.
- **Paket adı**: `io.github.yorulmazsinan.kokpit`. İsim "Kokpit" geçici; kullanıcı değiştirebilir.
- **Konum**: `~/Projeler/Mobil/kokpit/`, kendi git deposu.

### Desteklenen cihazlar *(benim kararım)*

- **Resmî destek**: yazarın cihazı (MT8227L, Android 8.1, 1024×600). Her sürüm bunun üzerinde
  doğrulanır.
- **En iyi çabayla destek**: Android 8.1–13 çalıştıran diğer yatay ekranlı multimedya cihazları.
  Bunun için ana ekran sabit piksel ızgarası yerine oranlarla yerleşir; 800×480 ile 1280×720 ve
  1920×720 arası yatay ekranlarda düzgün görünmelidir. Dikey ekranlar kapsam dışı.
- **Wi-Fi otomatik bağlanma**: Android 8.1–9'da Kokpit Wi-Fi'ı kendisi açar ve hotspot'a
  bağlanır. Android 10 ve üstünde uygulamalar bunu yapamıyor; orada ağ "öneri" olarak eklenir
  (`WifiNetworkSuggestion`) ve Wi-Fi kapalıysa kullanıcıya Wi-Fi panelini açan bir bildirim
  gösterilir. README bu farkı açıkça yazar.

## 9. Kurulum, geliştirme döngüsü ve güvenlik ağı

- Cihazın USB'si "host" modunda; Mac'ten USB ile ADB büyük ihtimalle çalışmaz. Kurulum
  **USB belleğe APK kopyalayıp** cihazdaki dosya yöneticisinden açarak yapılır. Fabrika
  ayarlarında ağ üzerinden ADB seçeneği varsa geliştirme o yolla hızlanır (teşhiste bakılacak).
- Arayüz geliştirme Mac'te 1024×600, 160 dpi bir Android 8.1 emülatöründe yapılır; cihazda
  yalnızca doğrulama ve donanıma bağlı özellikler denenir.
- **Güvenlik ağı**: Kokpit varsayılan ana ekran yapılmadan önce de normal uygulama olarak
  açılabilir. Sistem araçlarında "üretici ana ekranına dön" kısayolu bulunur. README, Kokpit'i
  varsayılandan kaldırma adımlarını anlatır. Cihazın ev tuşu üretici ana ekranına sabitse
  bu durum teşhiste görünür ve README'de geçici çözüm anlatılır.

## 10. Test

- Saf mantık için birim testler: ortalama hız/yolculuk sıfırlama, yön hesabı, hava durumu
  yanıtı ayrıştırma, gün doğumu/batımına göre tema seçimi, dock paket tahmini.
- Emülatörde Compose arayüz testleri: ana ekranın internet yok / GPS yok / medya yok
  durumlarında doğru boş hâlleri göstermesi.
- Cihazda elle kontrol listesi: açılış süresi, kaydırma, CarPlay ve diğer düğmeler, ekran
  kilidini kaldırma, uyanınca hotspot'a bağlanma.

## 11. Yayın

- Herkese açık GitHub deposu ve Releases'te imzalı APK. **Depoyu herkese açmak ve ilk sürümü
  yayımlamak kullanıcının onayıyla yapılır.**
- README: ekran görüntüleri, desteklenen cihazlar, USB bellekle kurulum, varsayılan ana ekran
  yapma/geri alma, teşhis çıktısıyla hata bildirme.

## 12. Builder'a bırakılanlar

- Ekranların görsel ayrıntıları (renk tonları, ikon seti, boşluklar) — §5'teki taslağın
  ruhuna ve okunabilirlik kurallarına uyarak.
- Hava durumu yenileme aralığı, Wi-Fi yeniden deneme zamanlaması, GPS örnekleme sıklığı.
- Bilinen üretici paket adları listesi (ZLink, Autokit, Bluetooth müzik, radyo, araç ayarları)
  ve tahmin mantığı.
- Proje içi paket/dosya düzeni.
