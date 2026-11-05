# Araştırma, Plan ve Yol Haritası

> Amaç: Tablette kalemle **matematik ve geometri** anlatırken kullanılacak, ücretsiz, reklamsız, abonelik istemeyen kişisel bir yazı uygulaması.
> Hedef cihaz: **Xiaomi Pad 7** (HyperOS / Android) + kalem; genel olarak **tüm Android tabletlerle** uyumlu.
> Kullanım şekilleri: projeksiyon/HDMI, çevrimiçi derste ekran paylaşımı (Zoom/Meet), dersi kaydedip video yapma.
> Referans alınan uygulama türleri: GoodNotes, Notability, Noteshelf, Samsung Notes, Xodo, Flexcil, OneNote, Xournal++ (açık kaynak).

---

## 1. Piyasadaki uygulamalarda ne var, biz neyi alacağız?

| Özellik | Ücretli uygulamalarda | Bizde |
|---|---|---|
| Kalemle yazma, basınç duyarlılığı | Var | **MVP** |
| Avuç reddi / "sadece kalem" modu | Var | **MVP** |
| Boş defter + sayfa şablonları (kareli, koordinat) | Var | **MVP** |
| PDF üzerine yazma + dışa aktarma | Var | **MVP** |
| **PDF'te sayfa numarasına doğrudan gitme** | Var | **MVP** |
| Silgi (vuruş / kısmi), geri al / yinele | Var | **MVP** |
| Kement (lasso) ile seçip taşıma/boyutlandırma | Var | Sürüm 1 |
| Şekil tanıma (düz çizgi, daire, üçgen, kare) | Var | Sürüm 1 |
| Cetvel / iletki / gönye / pergel | Az uygulamada | **Sürüm 1 (matematik için öncelikli)** |
| Lazer işaretçi, sunum modu, ikinci ekran | Bazılarında, genelde ücretli | Sürüm 1 |
| Uygulama içinden ders kaydı (ekran + ses → video) | Ücretli | Sürüm 1.5 |
| Sonsuz tuval (beyaz tahta) | Bazılarında | Sürüm 2 |
| El yazısı tanıma / arama, bulut senkron | Ücretli | Sürüm 3 (isteğe bağlı) |

**Bilinçli olarak yapmayacaklarımız (en azından başta):** hesap/üyelik sistemi, kendi bulut sunucumuz, yapay zekâ özellikleri, çoklu kullanıcı işbirliği.

---

## 2. Özellik listesi (ayrıntılı)

### 2.1 Yazma motoru (uygulamanın kalbi)
- **Basınç duyarlı kalem**: Kalem basıncına göre çizgi kalınlığı değişir (ayarlardan kapatılabilir).
- **Çok düşük gecikme**: Kalem ucu ile çizgi arasında gecikme hissedilmemeli. Android'in düşük gecikmeli çizim altyapısı (front-buffer rendering) kullanılır.
- **Çizgi yumuşatma**: Titrek el yazısını hafif düzeltme (ayarlanabilir seviye).
- **Vektör kayıt**: Her vuruş nokta listesi olarak (x, y, basınç, zaman) saklanır → sonsuz yakınlaştırmada bile net, dosya küçük, sonradan düzenlenebilir.
- Araçlar: **Kalem, Fosforlu kalem** (yarı saydam, yazının altında kalır), **Silgi** (vuruş silgisi + kısmi silgi), **Kement seçim**, **Metin kutusu**, **Şekil**, **Lazer**.
- **Geri al / Yinele** (sınırsız; ayrıca iki parmakla dokunma = geri al, üç parmak = yinele).

### 2.2 Kalem / el ayrımı (istenen ana ayar)
Ayarlar → Giriş:
- **Sadece kalemle çiz** (varsayılan AÇIK): Parmak/avuç dokunuşu asla çizmez. Parmaklar yalnızca kaydırma, iki parmak yakınlaştırma yapar.
- **Parmakla çizime izin ver** (kalem yanında değilse).
- **Parmak hareketlerini tamamen kapat** (sayfa kaymasın istiyorsanız).
- **Kalem tuşu** → silgi / seçim / lazer / geri al olarak atanabilir (Android'in bildirdiği kalem tuşu olaylarıyla; Xiaomi kalemin tuşlarının nasıl bildirildiği Aşama 0'da test edilecek).
- **Kalem yaklaştığında (hover) dokunmaları yok sayma** — kalem ekrana değmeden avuç dokunursa bile sayfa kaymaz.
- Solak / sağlak seçeneği (araç çubuğu konumu).

> Teknik not: Android her dokunuşun türünü bildirir (`MotionEvent.TOOL_TYPE_STYLUS` / `TOOL_TYPE_FINGER` / `TOOL_TYPE_ERASER`). Avuç reddi bu bilgiyle yapılır; tahmine dayalı algoritmaya gerek kalmaz. Ayrıca sistem iptal ettiği avuç dokunuşlarını `FLAG_CANCELED` ile bildirir.

### 2.3 Defterler ve sayfalar
- Yeni defter: **boş, çizgili, kareli, noktalı, izometrik (3B geometri için), koordinat düzlemi, milimetrik** şablonlar; A4 / 16:9 (projeksiyon ve video için) boyut.
- Sayfa ekle / sil / çoğalt / sırala, sayfa küçük resimleri paneli.
- Koyu arka plan seçeneği (kara tahta + tebeşir renkleri — projeksiyonda göz yormaz).
- Klasörler, defter kapağı/rengi, son açılanlar, defter adına göre arama.
- **Otomatik kaydetme** (her vuruştan sonra), kapanmalara karşı kayıp yok.

### 2.4 PDF
- PDF içe aktarma (dosya seçici ve başka uygulamalardan "Paylaş → Note" ile).
- Üzerine yazma; PDF'in **arasına boş/kareli sayfa ekleme** (soru çözmek için).
- **Sayfa numarasına doğrudan gitme**:
  - Ekrandaki sayfa göstergesine (`12 / 87`) dokun → sayı tuş takımı açılır → sayıyı yaz → o sayfaya atla.
  - **Kitap sayfa numarası farkı (ofset)**: PDF'in 1. sayfası kitabın 1. sayfası olmayabilir (kapak, önsöz). "Kitaptaki sayfa no = PDF sayfası − 8" gibi bir fark defter başına kaydedilir; böylece öğrencinin söylediği "sayfa 45" doğrudan açılır.
  - Kaydırma çubuğunu sürüklerken sayfa numarası balonu.
  - PDF'in kendi **içindekiler** listesi (varsa) ve kendi **yer imleriniz** (ör. "Türev konusu", "Test 3").
  - **Geri dön** düğmesi: atladıktan sonra önceki sayfaya tek dokunuşla dönüş.
  - Aynı özellikler normal defterlerde de çalışır.
- Sayfa küçük resimleri, yakınlaştırma, büyük PDF'lerde akıcı kaydırma.
- **Dışa aktarma**: yazılar orijinal PDF'in üzerine vektör olarak eklenmiş yeni bir PDF (öğrencilere gönderilebilir). Seçili sayfaları dışa aktarma. Tek sayfayı PNG olarak paylaşma.
- Görsel (fotoğraf, ekran görüntüsü) ekleme; kameradan soru fotoğrafı çekip sayfaya ekleme.

### 2.5 Matematik ve geometri araçları
- **Cetvel**: Ekranda iki parmakla döndürülüp taşınır; kalem cetvel kenarına yapışarak düz çizgi çizer. Uzunluk ve açı gösterir.
- **İletki**: Açı ölçme ve belirli açıda çizgi çizme.
- **Gönye** (30-60-90 ve 45-45-90).
- **Pergel**: Merkez ve yarıçap seçip daire / yay çizme.
- **Şekil tanıma**: Çizip kalemi bir an bekletince düz çizgi / daire / elips / üçgen / dikdörtgen / çokgen / ok'a dönüşür.
- **Düzenli çokgen** ekleme (eşkenar üçgen, kare, beşgen, altıgen…).
- **Izgaraya ve noktalara yapışma**: Şekillerin köşeleri kareli sayfanın kesişimlerine ve diğer şekillerin köşelerine/orta noktalarına yapışır.
- **Açı yapışması**: Çizgiler 15°'nin katlarına yapışabilir (açık/kapalı).
- **Koordinat düzlemi**: Ayarlanabilir eksen aralığı ve birim; ekrana yerleştirilebilir koordinat sistemi.
- **Fonksiyon grafiği** (Sürüm 2): `y = x² - 3` yazınca grafiğini sayfaya çizme.
- Geometri işaretleri: eşit uzunluk çentikleri, dik açı işareti, açı yayı, paralel oklar.

### 2.6 Ders anlatımı ve sunum
- **Lazer işaretçi**: Kalemle çizilen iz birkaç saniye sonra kaybolur.
- **Geçici kalem**: Yazdığı şey birkaç saniye sonra silinen kalem (vurgulamak için).
- **Sunum modu**: Araç çubukları gizlenir, tam ekran — ekran paylaşımında temiz görüntü.
- **İkinci ekran modu (projeksiyon/HDMI/kablosuz yansıtma)**: Tablet harici ekrana bağlıyken projeksiyonda sadece temiz sayfa görünür, araç çubukları ve menüler yalnız tablette kalır.
  (Not: Xiaomi Pad 7'nin USB-C'den görüntü verip vermediği Aşama 0'da kontrol edilecek; vermiyorsa kablosuz yansıtma / Miracast kullanılır.)
- **Ders kaydı (uygulama içinden)**: Sayfa + mikrofon sesi → MP4 video. Sadece sayfa kaydedilir (bildirimler, menüler görünmez). Duraklat / devam et. Kayıt sırasında sayfa geçişleri de videoya girer.
- **Hızlı renk çubuğu**: 4–6 sabit renk + 3 sabit kalınlık, tek dokunuşla geçiş.
- **Sayfayı temizle** (tek dokunuş, geri alınabilir).
- **Yakınlaştırma penceresi** (Sürüm 2): Küçük alanda büyük yazıp sayfaya küçük aktarma.

### 2.7 Ayarlar ekranı (özet)
- Giriş: sadece kalem, parmak hareketleri, kalem tuşu ataması, solak/sağlak
- Kalem: basınç duyarlılığı, yumuşatma seviyesi, varsayılan renk/kalınlıklar
- Görünüm: açık/koyu tema, varsayılan şablon, sayfa boyutu
- Geometri: şekil tanıma açık/kapalı, bekleme süresi, ızgara/açı yapışması
- Lazer/geçici kalem: kaybolma süresi
- Kayıt: ses kaynağı, video çözünürlüğü
- Yedekleme: tüm defterleri tek dosya olarak dışa/içe aktarma

---

## 3. Teknoloji seçimi

Hedef sadece Android olduğu için ilk plandaki "web uygulaması" önerisini güncelledim. iPad desteği gerekmediği için web uygulamasının asıl avantajı (Apple ücretinden kaçınmak) ortadan kalkıyor. Android'de normal (yerel) uygulama yapmak ücretsiz: APK dosyası doğrudan tablete kurulabilir. Google Play'e koymak istenirse tek seferlik 25$ ödeniyor, bu isteğe bağlı.

| Seçenek | Artıları | Eksileri |
|---|---|---|
| **A) Yerel Android — Kotlin + Jetpack Compose + Jetpack Ink** | **En düşük kalem gecikmesi** (Google'ın düşük gecikmeli çizim kütüphanesi). Kalem tuşu, hover, harici ekran ve video kaydı gibi tüm Android özelliklerine tam erişim. Tüm Android tabletlerde çalışır. | Sadece Android (bu proje için sorun değil). |
| B) Flutter | Tek kodla ileride iPad/Windows'a da geçilebilir. | Düşük gecikmeli çizim (front-buffer) desteği yok, kalem gecikmesi daha fazla. Harici ekran ve video kaydı için yine Android koduna ihtiyaç var. |
| C) Web uygulaması (PWA) | Bilgisayarda da açılır. | Gecikme en fazla; ikinci ekran ve kayıt kısıtlı; büyük PDF'lerde bellek sınırı. |

**Önerim: A) Yerel Android uygulaması (Kotlin).** Bu uygulamanın kalitesini kalemin ne kadar akıcı yazdığı belirliyor; en iyi sonucu bu seçenek verir.

### Önerilen yığın
- **Dil / arayüz**: Kotlin, Jetpack Compose
- **Çizim**: `androidx.ink` (Jetpack Ink: vuruş oluşturma, fırçalar, silgi, çizim) + `androidx.graphics:graphics-core` (düşük gecikmeli front-buffer çizim)
- **Kalem girişi**: `MotionEvent` (tür, basınç, eğim, hover, kalem tuşları), geçmiş noktalar (`getHistorical*`) ve tahmin (`MotionEventPredictor`)
- **PDF görüntüleme**: Android `PdfRenderer` (sistemde hazır, ücretsiz)
- **PDF dışa aktarma**: `PdfBox-Android` (yazıları orijinal PDF'e vektör olarak ekler; içindekiler listesini de okur)
- **Depolama**: Room (SQLite) — defterler, sayfalar, vuruşlar; PDF dosyaları uygulamanın kendi klasöründe
- **Harici ekran**: Android `Presentation` / `DisplayManager` API
- **Video kaydı**: `MediaCodec` + `MediaMuxer` (sayfa görüntüsü + mikrofon → MP4)
- **Test**: JUnit (birim), Compose UI testleri
- **Dağıtım**: GitHub Actions ile her sürümde otomatik APK üretimi → GitHub Releases'tan tablete indirip kurma
- **Hedef sürüm**: Android 10 ve üzeri (düşük gecikmeli çizim için gerekli; Xiaomi Pad 7 bunu fazlasıyla karşılıyor)

### Veri modeli (taslak)
```
Defter { id, ad, klasör, oluşturma/güncelleme tarihi, kaynakPdf?, sayfaNoFarkı, yerImleri[] }
  └─ Sayfa { id, sıra, şablon | pdfSayfaNo, genişlik, yükseklik }
       └─ Öğe (Vuruş | Metin | Görsel | Şekil | GeometriAracıİzi)
            Vuruş { araç, renk, kalınlık, opaklık, noktalar: [x, y, basınç, eğim, t][] }
            Şekil { tür (çizgi|daire|çokgen|yay…), geometri verisi, stil }
```
Şekiller vuruş olarak değil **gerçek geometri** olarak saklanır: daire = merkez + yarıçap. Böylece sonradan düzgünce taşınır, büyütülür ve köşelere yapıştırılabilir.

### Çizim mimarisi
- **3 katman**: (1) arka plan — şablon veya PDF sayfası (önbelleğe alınmış görüntü), (2) tamamlanmış vuruşlar, (3) "canlı" katman — o an çizilen vuruş, düşük gecikmeli ön tamponda.
- Yakınlaştırmada PDF sayfası yüksek çözünürlükte yeniden işlenir; ekranda olmayan sayfalar bellekten atılır.
- Harici ekran ve video kaydı aynı sayfa çizim kodunu kullanır (tek çizim motoru, birden fazla çıktı).

---

## 4. Yol haritası

> **Durum (5 Kasım 2026):** Aşama 0–4 kodlandı. Her push'ta GitHub Actions APK üretir (Releases sayfası) ve bir tablet emülatöründe uçtan uca duman testi çalıştırır. Gerçek cihazda (Xiaomi Pad 7) toplu kontrol bekleniyor. Kullanım: [KULLANIM.md](KULLANIM.md)
>
> Plandan sapmalar:
> - Çizimde Jetpack Ink yerine kendi çizim motorumuz kullanıldı (RenderNode + tamponsuz kalem olayları). Gecikme cihazda yetersiz görülürse ön tampon (front-buffer) çizimi eklenecek.
> - Room yerine sade, sürümlü ikili dosya formatı kullanıldı (ek derleme bağımlılığı yok, birim testli).

Her aşamanın sonunda tablette gerçekten **derste kullanılabilir** bir APK olması hedeflenir.

### Aşama 0 — Hazırlık ve cihaz testi (2–3 gün)
- [ ] Android projesi iskeleti (Kotlin, Compose), GitHub Actions ile otomatik APK üretimi
- [ ] **Xiaomi Pad 7 kalem testi**: basınç, eğim, hover, kalem/parmak ayrımı, kalem tuşlarının nasıl bildirildiği
- [ ] Düşük gecikmeli çizim denemesi (Jetpack Ink + front-buffer), gecikmenin gözle kontrolü
- [ ] Harici ekran (USB-C/HDMI veya kablosuz yansıtma) desteğinin kontrolü

### Aşama 1 — MVP: "Boş sayfaya kalemle yazabiliyorum" (1–2 hafta)
- [ ] Düşük gecikmeli çizim motoru, basınç duyarlı kalem, yumuşatma
- [ ] **Sadece kalem modu** + parmakla kaydırma / iki parmakla yakınlaştırma
- [ ] Kalem, fosforlu kalem, vuruş silgisi
- [ ] Renk ve kalınlık seçimi (hızlı çubuk)
- [ ] Geri al / yinele
- [ ] Çok sayfalı defter, sayfa ekle/sil, şablonlar (boş, çizgili, kareli, noktalı, koordinat)
- [ ] Otomatik kayıt, defter listesi ekranı
- [ ] Sayfa numarasına gitme (defterler için)

**Çıktı:** Derste beyaz tahta yerine kullanılabilir.

### Aşama 2 — PDF desteği (1–2 hafta)
- [ ] PDF içe aktarma ("Paylaş → Note" dahil) ve görüntüleme
- [ ] PDF sayfalarının üzerine yazma
- [ ] PDF arasına boş/kareli sayfa ekleme
- [ ] **Sayfa numarasına doğrudan gitme**, kitap sayfa no farkı (ofset), geri dön düğmesi
- [ ] Sayfa küçük resimleri paneli, PDF içindekiler listesi, kendi yer imleri
- [ ] **Yazılı PDF olarak dışa aktarma** ve paylaşma
- [ ] Büyük PDF'lerde performans (sadece görünen sayfaları işleme)

**Çıktı:** Kitap/soru PDF'i açıp istenen sayfaya hemen gidilir, üzerinde çözüm yapılır, öğrencilere gönderilebilir.

### Aşama 3 — Geometri araçları ve düzenleme (2–3 hafta)
- [ ] Şekil tanıma (çizgi, daire, elips, üçgen, dikdörtgen, çokgen, ok)
- [ ] Cetvel, iletki, gönye, pergel
- [ ] Izgaraya, köşelere ve açılara yapışma
- [ ] Düzenli çokgen ekleme, geometri işaretleri (dik açı, eşit uzunluk, açı yayı)
- [ ] Kısmi silgi, kement seçim (taşı / boyutlandır / döndür / renk değiştir / kopyala)
- [ ] Metin kutusu, görsel ve kameradan fotoğraf ekleme

### Aşama 4 — Sunum ve kayıt (1–2 hafta)
- [ ] Lazer işaretçi ve geçici kalem
- [ ] Sunum modu (tam ekran, araçlar gizli)
- [ ] **İkinci ekran modu**: projeksiyonda temiz sayfa, tablette araçlar
- [ ] **Ders kaydı**: sayfa + mikrofon → MP4, duraklat/devam
- [ ] Kara tahta sayfa arka planı, 16:9 sayfa boyutu
- [ ] Kalem tuşu ataması, iki parmak dokunma = geri al
- [ ] Ayarlar ekranının tamamı

### Aşama 5 — Düzen ve güvence (1 hafta)
- [ ] Klasörler, defter arama, sıralama
- [ ] Tüm verinin yedeğini tek dosya olarak alma / geri yükleme (ör. Google Drive'a kaydetme)
- [ ] Veri kaybını önleme testleri (uygulama kapanırken, pil biterken)

### Aşama 6 — Sürüm 2 (isteğe bağlı, ihtiyaca göre)
- [ ] Fonksiyon grafiği çizme (`y = f(x)`)
- [ ] Sonsuz tuval (sınırsız beyaz tahta)
- [ ] Katmanlar
- [ ] Yakınlaştırma penceresiyle yazma
- [ ] Ses kaydı + yazıyla senkron oynatma (videodan bağımsız)
- [ ] Otomatik yedekleme (Google Drive / yerel klasör)

---

## 5. Riskler ve önlemler

| Risk | Önlem |
|---|---|
| Xiaomi'nin HyperOS'u kalem olaylarını farklı bildirebilir | Aşama 0'da cihazda test; kalem tuşları ayarlardan yeniden atanabilir. |
| Xiaomi'nin pil tasarrufu uygulamayı arka planda kapatabilir (kayıt sırasında) | Kayıt sırasında ön plan servisi; kurulumda "pil kısıtlaması yok" ayarı için yönlendirme. |
| Farklı Android tabletlerde farklı davranış | Sadece standart Android API'leri; Samsung'a özel S Pen kütüphanesi gibi markaya özel bağımlılık yok. |
| Büyük PDF'lerde bellek | Sadece ekrandaki ve komşu sayfaları işleme, diğerlerini bellekten atma. |
| Veri kaybı | Her vuruştan sonra kayıt, veritabanı işlemleri, yedek dışa aktarma. |
| Harici ekran çıkışı cihazda yoksa | Kablosuz yansıtma veya ekran paylaşımı (sunum modu) ile aynı temiz görüntü. |
