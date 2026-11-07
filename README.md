# Note+

**Kalemle ders anlatmak ve not almak için ücretsiz, reklamsız ve internete bağlanmayan Android tablet uygulaması.**

Matematik ve geometri dersleri düşünülerek hazırlandı; Xiaomi Pad 7 + Focus Pen Pro üzerinde denenir, diğer Android tabletlerde de çalışır (Android 10 ve üzeri).

> ⬇️ **İndir:** [Sürümler (Releases)](../../releases) sayfasındaki en üstteki `Note-N.apk` dosyasını tablete indirip açın. Yeni sürüm eskisinin üzerine kurulur, defterleriniz silinmez.

---

## Neler yapabilir?

| | |
|---|---|
| ✍️ **Kalemle yazma** | Boş, çizgili, kareli, noktalı, izometrik, koordinat ve milimetrik sayfalar · basınç, yumuşatma · "sadece kalemle çiz" (avuç ve el yazmaz) |
| 📄 **PDF** | PDF'i açıp üzerine yazma, sayfa numarasına gitme, sayfa ekleme/çıkarma/döndürme/kırpma, yazılmış hâliyle PDF olarak dışa aktarma ve yazdırma |
| 📐 **Geometri** | Çizip bekleyince düzgün şekle dönüşme, doğru parçası, çokgen, pergel, işaretler, cetvel / iletki / gönye, ızgaraya ve açılara yapışma, ölçüler, 3B cisimler, fonksiyon grafiği, formül |
| ❌ **Yanlış defteri** | Bir soruyu seçip yanlış defterine ekleme; sorular galeri olarak görünür, çözüldü / tekrar takibi |
| 🧑‍🏫 **Ders anlatımı** | Lazer işaretçi, geçici kalem, odak modu, projeksiyon için ikinci ekran, uygulama içinden ders kaydı |
| 🗂️ **Düzen** | Klasörler, favoriler, sekmeler, yer imleri, yapılacaklar, şablonlar, defter kilidi (PIN), çöp kutusu |
| 🎨 **Görünüm** | Uygulama rengi, masa rengi, sol menü görseli, araç listesini düzenleme, koyu tema, göz dostu mod, büyük arayüz, Türkçe / English |
| 💾 **Yedek** | Tek dosyalık .zip yedek, otomatik yedek klasörü; bulut yok, her şey tablette |

## Gizlilik

Uygulamanın **internet izni yoktur**: hiçbir veri hiçbir yere gönderilmez, reklam ve yapay zekâ yoktur. Ayrıntılar: [Gizlilik politikası](docs/GIZLILIK.md)

## Belgeler

- [Kurulum ve kullanım kılavuzu](docs/KULLANIM.md)
- [Kontrol listesi](docs/KONTROL_LISTESI.md) — yeni sürümleri denerken sırayla bakılacaklar
- [Yapılacaklar](docs/YAPILACAKLAR.md) · [Yol haritası](docs/YOL_HARITASI.md)

## Geliştirme

- Kotlin + Jetpack Compose, minSdk 29, targetSdk 35
- Her gönderimde GitHub Actions: birim testleri → APK/AAB → `surum-N` sürümü; ayrıca emülatörde arayüz testleri ve ekran görüntüleri (`screenshots` dalı)
- Yerelde derleme: `./gradlew assembleDebug`

Geliştiren: **Poyraz DOĞAN**
