# Kokpit 1. Aşama Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MT8227L / Android 8.1 multimedya cihazı için açık kaynak ana ekran uygulaması Kokpit'in 1. aşamasını (ana ekran, sistem araçları, teşhis, uyanınca otomatik davranışlar, açılış videosu, açık kaynak hazırlığı) çalışır APK olarak üretmek.

**Architecture:** Tek modüllü Android uygulaması. Her özellik kendi paketinde: saf Kotlin mantığı (JVM birim testli) + Android'e değen ince bir katman (repository/servis) + Compose ekranı. Ana ekranın durumu `HomeViewModel` içinde akışlardan (`StateFlow`) birleştirilir. Uyanma/uyuma olaylarını uzun yaşayan bir ön plan servisi (`KokpitService`) dinler ve yolculuk, Wi-Fi ve açılış videosu davranışlarını tetikler.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), kotlinx-coroutines, kotlinx-serialization-json, OkHttp, DataStore Preferences, Media3 ExoPlayer, JUnit4 + Robolectric (Compose UI testleri JVM'de), Gradle (Kotlin DSL, version catalog).

**Spec:** `docs/superpowers/specs/2026-10-27-kokpit-design.md`

## Global Constraints

- Proje kökü: `~/Projeler/Mobil/kokpit/`, kendi git deposu, branch `main`. Paket adı / `applicationId`: `io.github.yorulmazsinan.kokpit`.
- `minSdk 27`, `targetSdk 35`, `compileSdk 36`. Yerel (native) kod yok; `armeabi-v7a` ve `arm64-v8a` cihazlarda çalışmalı.
- Google Play Services kullanılmaz (konum `LocationManager` ile). Analitik, reklam, izleme yok. 1. aşamada tek dış ağ çağrısı Open-Meteo.
- Root gerektiren hiçbir özellik yok; sistem/MCU yazılımına dokunulmaz.
- Koddaki tüm tanımlayıcılar İngilizce. Kullanıcıya görünen her metin `res/values/strings.xml` (İngilizce) ve `res/values-tr/strings.xml` (Türkçe) içinde; kodda sabit metin yok.
- Okunabilirlik: en küçük metin 14sp, dokunma alanı en az 64dp. Varsayılan tema koyu.
- Süre hesapları (yolculuk, uyku süresi, video kuralı) `SystemClock.elapsedRealtime()` ile yapılır, duvar saatiyle değil (cihaz saati açılışta 2007'de başlayıp ağdan düzelebiliyor).
- Lisans Apache-2.0. Herkese açık GitHub deposu açmak ve sürüm yayımlamak **yalnız kullanıcı onayıyla** (Task 12).
- JDK: Gradle'ı Homebrew `openjdk@21` ile çalıştır (`JAVA_HOME=$(brew --prefix openjdk@21)`); sistemdeki JDK 26'ya güvenme.

## Review Focus

1. **Uyanınca internet yok** (hotspot henüz açılmadı): ana ekran beklemeden açılmalı, hava durumu önbellekteki veriyi alındığı saatle göstermeli, ağ hatası kullanıcıya hata olarak sızmamalı. → Task 5 testi `returnsCachedWeatherWhenNetworkFails`.
2. **Cihaz saati sıçraması** (2007 → bugün): yolculuk süresi/ortalama hız ve video kuralı bozulmamalı. → Task 4 ve Task 10 testleri yalnız `elapsedRealtime` verir; Task 4 `wallClockIsNeverUsed` tasarım gereği (API'de duvar saati parametresi yok).
3. **GPS gürültüsü** (tünel, kötü kilit, ani 2 km sıçrama): ortalama hız şişmemeli. → Task 4 `ignoresInaccurateAndImpossibleFixes`.
4. **Dock'a atanmış uygulama kaldırılmış**: düğmeye basınca çökmemeli, uygulama seçiciyi açmalı. → Task 7 `fallsBackWhenOverrideNotInstalled` + Task 8 `DockButton` `ActivityNotFoundException` yakalar.
5. **Açılış videosu bozuk/desteklenmeyen/silinmiş**: ana ekran gecikmeden açılmalı. → Task 10 `skipsWhenFileMissing` ve oynatıcı hata dinleyicisi `finish()`.

---

## Dosya Yapısı

```
kokpit/
├── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew…
├── LICENSE, README.md, README.tr.md, .gitignore
├── .github/workflows/build.yml
└── app/src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/io/github/yorulmazsinan/kokpit/
    │   │   ├── KokpitApp.kt                  # Application, basit servis bulucu (AppContainer)
    │   │   ├── MainActivity.kt               # HOME launcher, Compose NavHost
    │   │   ├── ui/theme/                     # Color.kt, Type.kt, Theme.kt, Dimens.kt
    │   │   ├── system/                       # SystemShortcuts.kt, DiagnosticsCollector.kt,
    │   │   │                                 # DiagnosticsReport.kt, ActivityExplorer.kt, SystemToolsScreen.kt, DiagnosticsScreen.kt
    │   │   ├── drive/                        # TripTracker.kt, CompassPoint.kt, LocationSource.kt, DriveRepository.kt
    │   │   ├── weather/                      # OpenMeteoClient.kt, WeatherModels.kt, WeatherRepository.kt, WeatherCondition.kt
    │   │   ├── calendar/                     # CalendarRepository.kt, NextEventPicker.kt
    │   │   ├── media/                        # MediaNotificationListener.kt, NowPlayingRepository.kt, SessionSelector.kt
    │   │   ├── dock/                         # DockSlot.kt, KnownPackages.kt, DockResolver.kt, DockRepository.kt, AppPickerScreen.kt
    │   │   ├── home/                         # HomeViewModel.kt, HomeUiState.kt, HomeScreen.kt, cards/*.kt, ThemeResolver.kt
    │   │   ├── drawer/                       # AppDrawerScreen.kt, InstalledApps.kt
    │   │   ├── settings/                     # SettingsRepository.kt, SettingsScreen.kt
    │   │   ├── connect/                      # KokpitService.kt, BootReceiver.kt, WakeEvents.kt, RetryPolicy.kt,
    │   │   │                                 # WifiAutoConnector.kt, UsbAttachActivity.kt
    │   │   └── intro/                        # IntroVideoLibrary.kt, IntroVideoPolicy.kt, IntroActivity.kt
    │   └── res/ (values, values-tr, xml/usb_device_filter.xml, drawable/ikonlar)
    └── test/java/io/github/yorulmazsinan/kokpit/…  # paket başına birim + Robolectric testleri
        resources/open_meteo_sample.json
```

Bağımlılık enjeksiyonu kütüphanesi yok: `KokpitApp` içinde elle kurulan bir `AppContainer` repository'leri tutar.

---

### Task 1: Proje iskeleti, tema ve launcher kaydı

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, Gradle wrapper, `.gitignore`, `LICENSE` (Apache-2.0 tam metin), `app/src/main/AndroidManifest.xml`, `KokpitApp.kt`, `MainActivity.kt`, `ui/theme/{Color,Type,Theme,Dimens}.kt`, `res/values/strings.xml`, `res/values-tr/strings.xml`
- Test: `app/src/test/.../ui/theme/DimensTest.kt`

**Interfaces:**
- Produces: `KokpitTheme(dark: Boolean, content: @Composable () -> Unit)`; `object Dimens { val MinTouch = 64.dp; val CardRadius = 12.dp; val Gap = 12.dp }`; `object KokpitType { val minBody = 14.sp }`; `class KokpitApp : Application { lateinit var container: AppContainer }`; `class AppContainer(context: Context)` (sonraki task'lar alan ekler).

- [ ] **Step 1:** `cd ~/Projeler/Mobil/kokpit && git init -b main` (docs/ zaten var). Gradle wrapper'ı Gradle 9.x ile üret; AGP ve Kotlin için o gün en son kararlı sürümleri `libs.versions.toml`'a yaz; Compose BOM en son kararlı.
- [ ] **Step 2:** Manifest: `MainActivity` için `MAIN` + `LAUNCHER` + `HOME` + `DEFAULT` kategorileri, `android:screenOrientation="sensorLandscape"`, `launchMode="singleTask"`, `stateNotNeeded="true"`, `android:resizeableActivity` varsayılan. İzinler: `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_CALENDAR`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `QUERY_ALL_PACKAGES` (launcher için gerekli; README'de gerekçesi yazılır).
- [ ] **Step 3:** Tema: koyu ve açık `ColorScheme` (koyu: arka plan `#111110`, kart `#1E1E1C`, vurgu teal `#5DCAA5`/kapalı ton `#085041`, ikincil metin `#B4B2A9`); açık tema aynı vurgu. Tipografi: saat 64sp, başlık 22sp, gövde 16sp, ikincil 14sp. Hiçbir stil 14sp altına inmez.
- [ ] **Step 4: Test yaz** — `DimensTest`: `assertTrue(Dimens.MinTouch >= 64.dp)` ve `KokpitTypography`'deki tüm `TextStyle.fontSize >= 14.sp` (saat hariç değil, hepsi).
- [ ] **Step 5:** `JAVA_HOME=$(brew --prefix openjdk@21) ./gradlew test assembleDebug` → BUILD SUCCESSFUL, test PASS. (openjdk@21 yoksa `brew install openjdk@21`.)
- [ ] **Step 6:** `./gradlew :app:processDebugMainManifest` sonrası birleşik manifestte `android.intent.category.HOME` olduğunu `grep` ile doğrula.
- [ ] **Step 7: Commit** `chore: project skeleton, theme and launcher registration`

### Task 2: Sistem araçları (ekran kilidi dahil)

**Files:**
- Create: `system/SystemShortcuts.kt`, `system/ActivityExplorer.kt`, `system/SystemToolsScreen.kt`
- Modify: `MainActivity.kt` (NavHost'a `system-tools` rotası; geçici olarak açılış ekranı bu olsun ki ilk APK işe yarasın)
- Test: `test/.../system/SystemShortcutsTest.kt` (Robolectric)

**Interfaces:**
- Produces: `data class SystemShortcut(val id: String, @StringRes val label: Int, val intent: Intent)`; `fun systemShortcuts(): List<SystemShortcut>`; `fun List<SystemShortcut>.resolvable(pm: PackageManager): List<SystemShortcut>`; `fun exportedActivities(pm: PackageManager, packageName: String): List<ComponentName>`; `fun safeStart(context: Context, intent: Intent): Boolean` (yakalar: `ActivityNotFoundException`, `SecurityException`; false döner ve Toast gösterir).

- [ ] **Step 1: Test yaz** — `SystemShortcutsTest`:
  - `screenLockShortcutComesFirst`: `systemShortcuts().first().intent.action == DevicePolicyManager.ACTION_SET_NEW_PASSWORD`.
  - `containsExpectedActions`: aksiyonlar kümesi şunları içerir: `Settings.ACTION_SETTINGS`, `ACTION_WIFI_SETTINGS`, `ACTION_BLUETOOTH_SETTINGS`, `ACTION_APPLICATION_SETTINGS`, `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`, `ACTION_APPLICATION_DEVELOPMENT_SETTINGS`, `ACTION_DATE_SETTINGS`, `ACTION_LOCALE_SETTINGS`.
  - `resolvableDropsUnresolved`: Robolectric `ShadowPackageManager`'a yalnız `ACTION_SETTINGS` için bir activity ekle → `resolvable()` yalnız onu döndürür.
  - `safeStartReturnsFalseWhenNothingHandles`.
- [ ] **Step 2:** Testi çalıştır, FAIL gör.
- [ ] **Step 3:** Uygula. Listeye ayrıca "Üretici ana ekranına dön" eklenir: `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)` sonucu Kokpit dışındaki ilk HOME etkinliği (bulunamazsa gizli). "Üretici araç/fabrika ayarları" girdileri Task 3'teki teşhisten sonra `KnownPackages`'a eklenir.
- [ ] **Step 4:** `SystemToolsScreen`: büyük satırlar (min 64dp), en üstte "Ekran kilidini değiştir/kaldır"; altta "Etkinlik gezgini": paket seç (`com.android.settings` + üretici paketleri) → dışa açık etkinlik listesi → dokununca `safeStart`.
- [ ] **Step 5:** `./gradlew test` PASS; commit `feat: system tools screen with screen-lock shortcut`.

### Task 3: Teşhis ekranı ve ilk cihaz kontrol noktası

**Files:**
- Create: `system/DiagnosticsReport.kt`, `system/DiagnosticsCollector.kt`, `system/DiagnosticsScreen.kt`
- Test: `test/.../system/DiagnosticsReportTest.kt`

**Interfaces:**
- Produces: `data class DiagnosticsReport(val sdkInt: Int, val release: String, val model: String, val hardware: String, val display: String /* Build.DISPLAY */, val widthPx: Int, val heightPx: Int, val densityDpi: Int, val wifi5GhzSupported: Boolean?, val bluetoothAdapterPresent: Boolean, val defaultHomePackage: String?, val launchablePackages: List<String>, val homePackages: List<String>)`; `fun DiagnosticsReport.toText(): String`; `class DiagnosticsCollector(context: Context) { fun collect(): DiagnosticsReport }`.

- [ ] **Step 1: Test yaz** — `toTextContainsAllFields` (örnek rapor → metinde `sdk=27`, `1024x600`, `dpi=160`, `wifi5GHz=false`, `bt=true`, paket adları satır satır); `toTextMarksUnknown5Ghz` (`null` → `wifi5GHz=unknown`).
- [ ] **Step 2:** FAIL → **Step 3:** uygula (`WifiManager.is5GHzBandSupported()`, `BluetoothAdapter.getDefaultAdapter() != null`, gerçek ekran boyutu `getRealMetrics`). Ekranda iki düğme: "Panoya kopyala" ve "Dosyaya kaydet" (`ACTION_CREATE_DOCUMENT`, `text/plain`, ad `kokpit-diagnostics.txt`, böylece USB belleğe yazılabilir).
- [ ] **Step 4:** `./gradlew test assembleDebug` PASS; commit `feat: diagnostics screen`.
- [ ] **Step 5: Cihaz kontrol noktası (engelleyici değil).** `app-debug.apk`'yı kullanıcıya ver; README taslağındaki "USB bellekle kurulum" adımlarıyla kurmasını, Sistem araçlarından ekran kilidini kaldırmasını ve teşhis dosyasını göndermesini iste. Beklerken Task 4'ten devam et. Rapor gelince: gerçek çözünürlük/dpi, 5 GHz, Bluetooth durumu ve üretici paket adları (`KnownPackages`, Task 7) bu plana not edilir.

### Task 4: Yolculuk takibi (hız, yön, ortalama hız)

**Files:**
- Create: `drive/TripTracker.kt`, `drive/CompassPoint.kt`, `drive/LocationSource.kt`, `drive/DriveRepository.kt`
- Test: `test/.../drive/TripTrackerTest.kt`, `test/.../drive/CompassPointTest.kt`

**Interfaces:**
- Produces:
  - `data class Fix(val elapsedMs: Long, val lat: Double, val lon: Double, val accuracyM: Float, val speedMps: Float?, val bearingDeg: Float?)`
  - `class TripTracker(private val resetAfterSleepMs: Long = 30 * 60_000L)` — `fun onWake(elapsedMs: Long)`, `fun onSleep(elapsedMs: Long)`, `fun onFix(fix: Fix)`, `fun snapshot(nowElapsedMs: Long): TripSnapshot`
  - `data class TripSnapshot(val speedKmh: Int?, val compass: CompassPoint?, val averageKmh: Int?, val hasFix: Boolean)`
  - `enum class CompassPoint(@StringRes val label: Int) { N, NE, E, SE, S, SW, W, NW; companion object { fun fromBearing(deg: Float): CompassPoint } }` — Türkçe etiketler K, KD, D, GD, G, GB, B, KB.
  - `class DriveRepository(locationSource: LocationSource, tracker: TripTracker) { val state: StateFlow<TripSnapshot> }` — saniyede bir `snapshot` yayar.

Kurallar (spec §5): ortalama = toplam mesafe ÷ uyanık süre; durulan süre sayılır; uykuda geçen süre sayılmaz; 30 dk'dan uzun uykudan sonra her şey sıfırlanır. Mesafe ardışık kabul edilmiş fix'ler arası haversine. Fix reddi: `accuracyM > 50` veya önceki kabul edilmiş fix'e göre ima edilen hız > 250 km/s. Hız: `speedMps` varsa o, yoksa son iki fix'ten. Son fix 5 sn'den eskiyse `hasFix=false`, `speedKmh=null`. Ortalama, uyanık süre 60 sn'den azsa `null`.

- [ ] **Step 1: Test yaz** — `TripTrackerTest`:
  - `averageCountsStoppedTimeWhileAwake`: wake t=0; 10 dk'da 10 km yol; sonra 10 dk aynı noktada fix'ler → t=20dk'da `averageKmh == 30`.
  - `sleepUnder30MinIsExcludedAndTripContinues`: 10 dk'da 10 km, sleep t=10dk, wake t=25dk, 10 dk'da 10 km daha → t=35dk'da `averageKmh == 60`.
  - `sleepOver30MinResetsTrip`: sleep t=10dk, wake t=45dk → hemen sonra `averageKmh == null`.
  - `ignoresInaccurateAndImpossibleFixes`: `accuracyM=80` fix'i ve 1 saniyede 2 km sıçrayan fix toplam mesafeyi değiştirmez.
  - `staleFixMeansNoSpeed`: son fix'ten 6 sn sonra `hasFix == false`, `speedKmh == null`.
- [ ] **Step 2:** `CompassPointTest`: 0→N, 44→NE, 46→NE, 90→E, 337.6→N, 359.9→N, 202→S, 247.5→W (sınırlar 22.5° dilimleri, `[−22.5, 22.5)` N).
- [ ] **Step 3:** FAIL → **Step 4:** uygula. `LocationSource`: `LocationManager.GPS_PROVIDER`, 1000 ms / 0 m, izin yoksa boş akış ve `permissionMissing` durumu.
- [ ] **Step 5:** `./gradlew test` PASS; commit `feat: trip tracker with speed, heading and average speed`.

### Task 5: Hava durumu (Open-Meteo)

**Files:**
- Create: `weather/WeatherModels.kt`, `weather/OpenMeteoClient.kt`, `weather/WeatherCondition.kt`, `weather/WeatherRepository.kt`
- Test: `test/.../weather/OpenMeteoParseTest.kt`, `test/.../weather/WeatherRepositoryTest.kt`, `test/resources/open_meteo_sample.json`

**Interfaces:**
- Produces:
  - `data class Weather(val temperatureC: Double, val code: Int, val isDay: Boolean, val maxC: Double, val minC: Double, val sunriseLocal: LocalTime, val sunsetLocal: LocalTime, val fetchedAtEpochMs: Long)`
  - `class OpenMeteoClient(http: OkHttpClient, baseUrl: String = "https://api.open-meteo.com") { suspend fun fetch(lat: Double, lon: Double): Weather }` — istek: `/v1/forecast?latitude=…&longitude=…&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=1`. Bağlantı/okuma zaman aşımı 10 sn.
  - `enum class WeatherCondition(@StringRes label: Int, @DrawableRes icon: Int) { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, SHOWERS, THUNDER; companion object { fun fromWmo(code: Int): WeatherCondition } }` (WMO: 0 CLEAR; 1–2 PARTLY_CLOUDY; 3 CLOUDY; 45,48 FOG; 51–57 DRIZZLE; 61–67 RAIN; 71–77,85,86 SNOW; 80–82 SHOWERS; 95–99 THUNDER; bilinmeyen → CLOUDY)
  - `sealed interface WeatherState { object NoData; data class Fresh(val weather: Weather); data class Stale(val weather: Weather) }`
  - `class WeatherRepository(client, store: DataStore<Preferences>, location: () -> Pair<Double, Double>?) { val state: StateFlow<WeatherState>; suspend fun refresh() }` — son başarılı yanıtı JSON olarak DataStore'a yazar; 30 dk'da bir ve ağ geri gelince yenilenir; 3 saatten eski veri `Stale`.

- [ ] **Step 1: Test yaz** — `OpenMeteoParseTest.parsesSample` (örnek JSON → `temperatureC=18.4`, `code=2`, `maxC=21.0`, `minC=12.1`, `sunrise=06:58`, `sunset=17:21`) ve `wmoMapping` (0→CLEAR, 2→PARTLY_CLOUDY, 63→RAIN, 96→THUNDER, 1234→CLOUDY). Örnek JSON'u Open-Meteo yanıt biçiminde elle yaz.
- [ ] **Step 2:** `WeatherRepositoryTest` (MockWebServer + test DataStore + sahte saat): `returnsCachedWeatherWhenNetworkFails` (önce 200 → `Fresh`; sonra bağlantı hatası → durum hâlâ önceki veriyle, istisna dışarı sızmaz); `marksStaleAfterThreeHours`; `noLocationKeepsNoData`.
- [ ] **Step 3:** FAIL → **Step 4:** uygula → **Step 5:** PASS; commit `feat: weather via open-meteo with offline cache`.

### Task 6: Takvim ve çalan medya

**Files:**
- Create: `calendar/NextEventPicker.kt`, `calendar/CalendarRepository.kt`, `media/SessionSelector.kt`, `media/MediaNotificationListener.kt`, `media/NowPlayingRepository.kt`
- Modify: `AndroidManifest.xml` (NotificationListenerService, `BIND_NOTIFICATION_LISTENER_SERVICE`)
- Test: `test/.../calendar/NextEventPickerTest.kt`, `test/.../media/SessionSelectorTest.kt`

**Interfaces:**
- Produces:
  - `data class EventInstance(val title: String, val beginMs: Long, val endMs: Long, val allDay: Boolean)`; `data class UpcomingEvent(val title: String, val beginMs: Long)`; `fun pickNextEvent(instances: List<EventInstance>, nowMs: Long, endOfDayMs: Long): UpcomingEvent?`
  - `class CalendarRepository(context) { val state: StateFlow<CalendarState> }` — `CalendarState = PermissionMissing | Empty | Next(UpcomingEvent)`; `CalendarContract.Instances` bugünün aralığı; değişiklikte `ContentObserver` ile yeniler.
  - `data class SessionSnapshot(val packageName: String, val title: String?, val artist: String?, val isPlaying: Boolean, val lastActiveMs: Long)`; `fun selectSession(sessions: List<SessionSnapshot>): SessionSnapshot?`
  - `data class NowPlaying(val title: String, val artist: String?, val sourceLabel: String, val art: Bitmap?, val isPlaying: Boolean)`; `class NowPlayingRepository { val state: StateFlow<NowPlayingState>; fun playPause(); fun next(); fun previous() }` — `NowPlayingState = AccessMissing | Idle | Playing(NowPlaying)`.

Kurallar: tüm gün etkinlikleri atlanır; bitmiş olanlar atlanır; şu an süren etkinlik "şimdi" olarak seçilebilir (begin ≤ now < end); en erken başlayan seçilir. Oturum seçimi: çalan varsa en son aktif olan çalan; yoksa en son aktif olan; başlığı boş olanlar elenir.

- [ ] **Step 1: Test yaz** — `NextEventPickerTest`: `skipsAllDayAndEnded`, `prefersOngoingThenEarliest`, `nullWhenNothingLeftToday`. `SessionSelectorTest`: `prefersPlayingOverPaused`, `fallsBackToMostRecent`, `ignoresBlankTitles`, `nullOnEmpty`.
- [ ] **Step 2:** FAIL → **Step 3:** uygula → **Step 4:** PASS; commit `feat: calendar next event and now-playing media`.

### Task 7: Dock eşlemeleri

**Files:**
- Create: `dock/DockSlot.kt`, `dock/KnownPackages.kt`, `dock/DockResolver.kt`, `dock/DockRepository.kt`, `dock/AppPickerScreen.kt`, `drawer/InstalledApps.kt`
- Test: `test/.../dock/DockResolverTest.kt`

**Interfaces:**
- Produces:
  - `enum class DockSlot(@StringRes label: Int, @DrawableRes icon: Int, val inDock: Boolean = true) { CARPLAY, BLUETOOTH, PHONE, RADIO, MAPS, APPS, ASSISTANT(inDock = false) }` (APPS her zaman çekmeceyi açar, eşlenmez; ASSISTANT ana ekrandaki mikrofon düğmesinin 1. aşamadaki hedefi, adayı yok, yalnız kullanıcı seçer)
  - `object KnownPackages { val candidates: Map<DockSlot, List<String>> }` — başlangıç: CARPLAY `com.zjinnova.zlink`; MAPS `com.google.android.apps.maps`, `ru.yandex.yandexnavi`, `ru.yandex.yandexmaps`, `com.waze`; diğerleri Task 3 teşhis raporundan doldurulur.
  - `data class AppEntry(val packageName: String, val label: String, val component: ComponentName)`; `class InstalledApps(pm: PackageManager) { fun launchable(): List<AppEntry> }` (alfabetik, Kokpit hariç)
  - `fun resolveDockTarget(slot: DockSlot, installed: Set<String>, override: String?): String?`
  - `class DockRepository(store: DataStore<Preferences>, apps: InstalledApps) { val targets: StateFlow<Map<DockSlot, String?>>; suspend fun setOverride(slot: DockSlot, packageName: String?) }`

- [ ] **Step 1: Test yaz** — `usesOverrideWhenInstalled`, `fallsBackWhenOverrideNotInstalled` (override kurulu değil → aday listesindeki ilk kurulu), `nullWhenNothingInstalled`, `appsSlotAlwaysNull`.
- [ ] **Step 2:** FAIL → **Step 3:** uygula. `AppPickerScreen`: `InstalledApps.launchable()` ızgarası; seçim `setOverride` yapar.
- [ ] **Step 4:** PASS; commit `feat: configurable dock targets`.

### Task 8: Ana ekran

**Files:**
- Create: `home/HomeUiState.kt`, `home/HomeViewModel.kt`, `home/ThemeResolver.kt`, `home/HomeScreen.kt`, `home/cards/{ClockCard,WeatherCard,CalendarRow,NowPlayingCard,DriveCard,Dock,AssistantButton}.kt`
- Modify: `MainActivity.kt` (başlangıç rotası artık `home`; sistem araçları ayarlardan ve dock'taki Uygulamalar'ın uzun basışından), `AppContainer`
- Test: `test/.../home/ThemeResolverTest.kt`, `test/.../home/HomeScreenTest.kt` (Robolectric + `createComposeRule`)

**Interfaces:**
- Consumes: Task 4 `DriveRepository.state`, Task 5 `WeatherRepository.state`, Task 6 `CalendarRepository.state` ve `NowPlayingRepository`, Task 7 `DockRepository.targets`.
- Produces: `data class HomeUiState(val weather: WeatherState, val calendar: CalendarState, val media: NowPlayingState, val trip: TripSnapshot, val dock: Map<DockSlot, String?>, val dark: Boolean)`; `enum class ThemeMode { DARK, LIGHT, AUTO }`; `fun resolveDark(mode: ThemeMode, now: LocalTime, sunrise: LocalTime?, sunset: LocalTime?): Boolean` (AUTO + gün doğumu/batımı yoksa koyu).

Yerleşim spec §5 taslağı: üstte iki sütun (sol ~%55 saat/hava/takvim kartı; sağda üst medya kartı, alt sürüş kartı), altta 6 eşit dock düğmesi; mikrofon `AssistantButton` saat kartının sağ üst köşesinde (1. aşamada: dokununca "yakında" + ayarlarda bir uygulama atanmışsa onu açar). Oranlar `weight` ile; sabit piksel yok. Saat `java.time` + sistem 24 saat ayarı, dakikada bir yenilenir; tarih `DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)`. Animasyon yok, geçişler 150 ms'den kısa.

- [ ] **Step 1: Test yaz** — `ThemeResolverTest`: `autoIsLightBetweenSunriseAndSunset`, `autoIsDarkWithoutSunTimes`, `fixedModesIgnoreTime`. `HomeScreenTest` (sahte `HomeUiState` ile):
  - `showsCachedWeatherWithTimestampWhenStale` (Stale → "son güncelleme 14:10" metni)
  - `showsInternetNeededWhenNoWeatherData`
  - `hidesCalendarRowWhenEmpty`, `showsPermissionPromptWhenCalendarPermissionMissing`
  - `showsGpsSearchingWithoutFix`
  - `showsNothingPlayingWhenIdle`
  - `dockShowsSixButtons` ve her düğmenin dokunma boyutu ≥ 64dp (`assertHeightIsAtLeast(64.dp)`).
- [ ] **Step 2:** FAIL → **Step 3:** uygula. Dock düğmesi `safeStart` kullanır; hedef `null` ya da başlatma başarısızsa `AppPickerScreen`'e gider; uzun basış her zaman seçiciyi açar.
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Görsel kontrol** — `sdkmanager "system-images;android-27;google_apis;arm64-v8a"`; AVD: 1024×600, 160 dpi, yatay. `installDebug`, varsayılan ana ekran yap, ekran görüntüsü al. Ayrıca 800×480 ve 1280×720 AVD'lerde taşma/kesilme olmadığını gör. Ekran görüntülerini kullanıcıya göster.
- [ ] **Step 6:** Commit `feat: home screen`.

### Task 9: Uygulama çekmecesi ve ayarlar

**Files:**
- Create: `drawer/AppDrawerScreen.kt`, `settings/SettingsRepository.kt`, `settings/SettingsScreen.kt`
- Test: `test/.../settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces: `data class KokpitSettings(val themeMode: ThemeMode, val hotspotSsid: String?, val hotspotPassword: String?, val autoWifi: Boolean, val autoCarPlayOnUsb: Boolean, val intro: IntroSettings)`; `IntroMode` ve `IntroSettings` bu task'ta `intro/IntroSettings.kt` içinde, Task 10'daki alan ve varsayılanlarla tanımlanır (Task 10 yalnız kullanır); `class SettingsRepository(store) { val settings: StateFlow<KokpitSettings>; suspend fun update(transform: (KokpitSettings) -> KokpitSettings) }`. Varsayılanlar: `themeMode=DARK`, `autoWifi=true`, `autoCarPlayOnUsb=false` (ZLink ile çakışıp seçici göstermemek için; kullanıcı açar — *plan kararı*), intro Task 10'da.
- Hotspot parolası `EncryptedSharedPreferences` yerine Android Keystore ile şifrelenip DataStore'a yazılır (`androidx.security` kullanılmaz; küçük bir `SecretBox` yardımcı: AES/GCM anahtar Keystore'da).

- [ ] **Step 1: Test yaz** — `defaultsMatchSpec`, `updatePersistsAcrossInstances` (aynı dosyayla iki repository), `passwordIsNotStoredInPlainText` (DataStore dosyasının ham baytlarında parola geçmez; Robolectric Keystore yoksa `SecretBox`'ı arayüzle soyutla ve sahte şifreleyiciyle test et).
- [ ] **Step 2:** FAIL → **Step 3:** uygula. Ayarlar bölümleri: Görünüm (tema), Dock (her slot için seçici), Bağlantı (otomatik Wi-Fi, hotspot adı/parolası + iPhone notu: "Başkalarının katılmasına izin ver" ve "Uyumluluğu en üst düzeye çıkar"; "Bluetooth otomatik bağlanma" satırı üretici Bluetooth uygulamasını açar ve oradaki ayarın yapılmasını anlatır), Asistan düğmesi (1. aşamada atanacak uygulama, ör. ChatGPT — `DockSlot.ASSISTANT`), CarPlay (USB'de otomatik aç), Açılış videosu (Task 10), Takvim kurulumu (DAVx⁵ + uygulamaya özel parola adımları, DAVx⁵ kuruluysa açma düğmesi), Sistem araçları, Teşhis, Hakkında (sürüm, lisans, GitHub bağlantısı).
- [ ] **Step 4:** Çekmece: `InstalledApps.launchable()` 5–6 sütunlu ızgara, uzun basışta `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
- [ ] **Step 5:** PASS; commit `feat: app drawer and settings`.

### Task 10: Uyanma olayları, otomatik Wi-Fi, CarPlay ve açılış videosu

**Files:**
- Create: `connect/WakeEvents.kt`, `connect/KokpitService.kt`, `connect/BootReceiver.kt`, `connect/RetryPolicy.kt`, `connect/WifiAutoConnector.kt`, `connect/UsbAttachActivity.kt`, `res/xml/usb_device_filter.xml`, `intro/IntroVideoLibrary.kt`, `intro/IntroVideoPolicy.kt`, `intro/IntroActivity.kt`
- Modify: `AndroidManifest.xml`, `settings/SettingsScreen.kt` (video kitaplığı bölümü), `AppContainer`
- Test: `test/.../connect/RetryPolicyTest.kt`, `test/.../intro/IntroVideoPolicyTest.kt`, `test/.../intro/IntroVideoLibraryTest.kt`

**Interfaces:**
- Consumes: Task 4 `TripTracker.onWake/onSleep`, Task 9 `SettingsRepository`.
- Produces:
  - `sealed interface WakeEvent { data class Wake(val elapsedMs: Long, val sleptMs: Long?); data class Sleep(val elapsedMs: Long) }`; `object WakeEvents { val flow: SharedFlow<WakeEvent> }` — `KokpitService` `ACTION_SCREEN_ON/OFF`'u dinamik kayıtla yakalayıp yayar. Servis `BOOT_COMPLETED` ve uygulama açılışında başlar; ön plan bildirimi sessiz, düşük öncelikli ("Kokpit çalışıyor"). Teşhiste üreticiye özel ACC yayınları görülürse aynı akışa eklenir.
  - `class RetryPolicy(val intervalMs: Long = 20_000, val windowMs: Long = 3 * 60_000) { fun nextAttemptDelay(startedElapsedMs: Long, nowElapsedMs: Long): Long? }` (`null` = vazgeç)
  - `class WifiAutoConnector(context, settings) { suspend fun onWake() }` — API ≤ 28: Wi-Fi kapalıysa `setWifiEnabled(true)`; SSID için `WifiConfiguration` yoksa ekle (WPA2), `enableNetwork(id, true)`; bağlanana ya da `RetryPolicy` `null` dönene kadar dene. API ≥ 29: `WifiNetworkSuggestion` ekle; Wi-Fi kapalıysa `Settings.Panel.ACTION_WIFI` açan bildirim.
  - `UsbAttachActivity`: şeffaf, `USB_DEVICE_ATTACHED` + `usb_device_filter.xml` (`vendor-id="1452"`, Apple); ayar açıksa CARPLAY dock hedefini `safeStart` ile açar, her durumda hemen `finish()`. Ayar kapalıyken manifest bileşeni `PackageManager.setComponentEnabledSetting` ile devre dışı (seçici çıkmasın).
  - `data class IntroVideo(val id: String, val displayName: String, val sizeBytes: Long)`; (Task 9'da `intro/IntroSettings.kt` içinde tanımlı) `enum class IntroMode { OFF, SELECTED, SEQUENTIAL, RANDOM }`; `data class IntroSettings(val mode: IntroMode = IntroMode.OFF, val selectedId: String? = null, val everyWake: Boolean = false, val withSound: Boolean = false, val lastPlayedIndex: Int = -1)`
  - `class IntroVideoLibrary(dir: File /* filesDir/intro */) { fun list(): List<IntroVideo>; fun import(input: InputStream, displayName: String, sizeBytes: Long): Result<IntroVideo>; fun delete(id: String); fun fileFor(id: String): File? }` — 20 MB üstü `Result.failure(TooLarge)`; uzantı `.mp4` değilse `UnsupportedType`.
  - `fun pickIntroVideo(settings: IntroSettings, videos: List<IntroVideo>, sleptMs: Long?, random: Random): IntroVideo?` — `OFF` veya liste boş → `null`; `everyWake=false` ve `sleptMs != null && sleptMs < 30 dk` → `null`; `sleptMs == null` (soğuk açılış) oynat; `SELECTED` → `selectedId` yoksa ilk video; `SEQUENTIAL` → `(lastPlayedIndex+1) % size`; `RANDOM` → `random`.
  - `IntroActivity`: tam ekran Media3 `PlayerView` (kontroller gizli), sessiz varsayılan; dokunma, `STATE_ENDED` veya `onPlayerError` → `finish()` (ana ekran altta zaten açık). Uyanışta `KokpitService`, `pickIntroVideo` sonucu varsa başlatır ve `lastPlayedIndex`'i günceller.

- [ ] **Step 1: Test yaz** — `RetryPolicyTest`: `retriesEvery20sWithinWindow`, `givesUpAfterThreeMinutes`. `IntroVideoPolicyTest`: `offReturnsNull`, `skipsAfterShortSleep`, `playsAfterLongSleep`, `everyWakeIgnoresSleepLength`, `coldBootPlays`, `sequentialAdvancesAndWraps`, `selectedFallsBackToFirstWhenMissing`, `skipsWhenFileMissing` (boş liste → `null`). `IntroVideoLibraryTest` (geçici klasör): `importCopiesAndLists`, `rejectsOver20Mb`, `rejectsNonMp4`, `deleteRemovesFile`.
- [ ] **Step 2:** FAIL → **Step 3:** uygula. Ayarlar → Açılış videosu: "USB'den video ekle" (`ACTION_OPEN_DOCUMENT`, `video/mp4`, çoklu seçim) → kitaplığa kopyala; liste (ad, boyut, önizle, sil, "bunu oynat"); mod seçimi; "her uyanışta"; "sesli". Metin: "Videolar cihaza kopyalanır, USB'yi çıkarabilirsiniz."
- [ ] **Step 4:** `./gradlew test` PASS. Emülatörde: ekranı kapat/aç (`adb shell input keyevent 26`) → video oynar, dokununca kapanır; bozuk bir `.mp4` (rastgele bayt) eklendiğinde ana ekran anında gelir.
- [ ] **Step 5:** Commit `feat: wake handling, wifi auto-connect, carplay on usb, intro videos`.

### Task 11: Release derlemesi, performans ve README

**Files:**
- Modify: `app/build.gradle.kts` (release: `isMinifyEnabled = true`, `isShrinkResources = true`, imza ayarı `keystore.properties`'ten — dosya yoksa imzasız), `proguard-rules.pro`, `.gitignore` (`keystore.properties`, `*.jks`)
- Create: `baselineprofile/` modülü (androidx.baselineprofile eklentisi; profil emülatörde API 33+ AVD ile üretilir, `app/src/main/baseline-prof.txt` depoya girer), `README.md` (İngilizce), `README.tr.md`, `.github/workflows/build.yml`

- [ ] **Step 1:** Release yapılandırması; `./gradlew assembleRelease` başarılı; R8 sonrası uygulama emülatörde açılıp ana ekran, ayarlar, video akışı çalışıyor (R8'in serileştirme sınıflarını silmediğini doğrular).
- [ ] **Step 2:** Baseline profil: ana ekranın açılışı ve çekmecenin kaydırılması senaryosu; profili üret ve commit'le.
- [ ] **Step 3:** README (iki dil): ekran görüntüleri (Task 8), desteklenen cihazlar (resmî: MT8227L/8.1/1024×600; diğerleri en iyi çaba), **USB bellekle kurulum** (APK'yı belleğe kopyala → dosya yöneticisi → bilinmeyen kaynaklara izin), varsayılan ana ekran yapma ve **geri alma** (Ayarlar → Uygulamalar → Varsayılan uygulamalar → Ana ekran; Kokpit içinden "Üretici ana ekranına dön"), ev tuşu üretici ekranına sabitse ne yapılacağı (teşhis sonucuna göre), ekran kilidi kaldırma, iCloud takvimi (DAVx⁵), iPhone hotspot ayarları, açılış videosu biçimi (MP4 H.264, 1024×600, 3–8 sn, ≤20 MB), `QUERY_ALL_PACKAGES` gerekçesi, gizlilik (tek dış istek Open-Meteo), teşhisle hata bildirme, lisans.
- [ ] **Step 4:** CI: `push`/`pull_request`'te `./gradlew test assembleRelease` (imzasız), APK'yı artifact olarak yükle.
- [ ] **Step 5:** Commit `chore: release build, baseline profile, readme and ci`.

### Task 12: Cihazda doğrulama ve yayın kararı

- [ ] **Step 1:** İmzalı release APK'yı kullanıcıya ver (imza anahtarını yerelde üret, `~/.config/kokpit/`'te sakla, depoya girmez). Kullanıcıyla cihazda kontrol listesi:
  - Kokpit varsayılan ana ekran; ev tuşu Kokpit'e dönüyor (dönmüyorsa teşhise göre çözüm).
  - Soğuk açılış ve uyanışta ana ekranın görünme süresi < 1 sn; çekmece kaydırması akıcı. **Karar noktası:** belirgin takılma varsa ana ekranın View sistemine taşınması ayrı bir plan olarak kullanıcıya sunulur.
  - Dock'taki altı düğme doğru uygulamaları açıyor; CarPlay kabloyla bağlanıyor.
  - Hava durumu, takvim (DAVx⁵ sonrası), çalan şarkı, GPS hız/yön/ortalama.
  - Kontak kapat → 30 dk'dan kısa bekle → aç: video yok, yolculuk sürüyor; 30 dk'dan uzun: video var, yolculuk sıfır.
  - Uyanınca Wi-Fi açılıp iPhone hotspot'una bağlanıyor.
  - Ekran kilidi kaldırıldı.
- [ ] **Step 2:** Bulunan hataları düzelt (her biri test + commit).
- [ ] **Step 3:** Kullanıcıdan **açık onay** al: GitHub'da `yorulmazsinan/kokpit` herkese açık depo oluşturma ve `v0.1.0` sürümünü (imzalı APK) yayımlama. Onay gelirse `gh repo create … --public`, push, `gh release create v0.1.0`. Onay yoksa depo yerelde kalır.
