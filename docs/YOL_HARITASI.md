# Araştırma, Plan ve Yol Haritası

> Amaç: Tablette kalemle ders anlatırken kullanılacak, ücretsiz, reklamsız, abonelik istemeyen kişisel bir yazı uygulaması.
> Referans alınan uygulama türleri: GoodNotes, Notability, Noteshelf, Samsung Notes, Xodo, Flexcil, OneNote, Xournal++ (açık kaynak).

---

## 1. Piyasadaki uygulamalarda ne var, biz neyi alacağız?

| Özellik | Ücretli uygulamalarda | Bizde |
|---|---|---|
| Kalemle yazma, basınç duyarlılığı | Var | **MVP** |
| Avuç reddi / "sadece kalem" modu | Var | **MVP** |
| Boş defter + sayfa şablonları | Var | **MVP** |
| PDF üzerine yazma + dışa aktarma | Var | **MVP** |
| Silgi (vuruş / kısmi), geri al / yinele | Var | **MVP** |
| Fosforlu kalem, renk/kalınlık hazır ayarları | Var | **MVP** |
| Kement (lasso) ile seçip taşıma/boyutlandırma | Var | Sürüm 1 |
| Şekil tanıma (düz çizgi, daire, kare) | Var | Sürüm 1 |
| Lazer işaretçi, sunum modu | Bazılarında, genelde ücretli | Sürüm 1 (ders için kritik) |
| Cetvel / iletki / grafik kağıdı | Bazılarında | Sürüm 1 |
| Sonsuz tuval (beyaz tahta) | Bazılarında | Sürüm 2 |
| Ses kaydı + yazıyla senkron | Notability (ücretli) | Sürüm 2 |
| El yazısı tanıma / arama, bulut senkron | Ücretli | Sürüm 3 (isteğe bağlı) |

**Bilinçli olarak yapmayacaklarımız (en azından başta):** hesap/üyelik sistemi, kendi bulut sunucumuz, yapay zekâ özellikleri, çoklu kullanıcı işbirliği. Bunlar ücretli uygulamaları ücretli yapan şeyler; ders anlatmak için gerekmiyor.

---

## 2. Özellik listesi (ayrıntılı)

### 2.1 Yazma motoru (uygulamanın kalbi)
- **Basınç duyarlı kalem**: Kalem basıncına göre çizgi kalınlığı değişir (ayarlardan kapatılabilir).
- **Düşük gecikme**: Kalem ucu ile çizgi arasındaki gecikme hissedilmemeli. (Tahmini noktalar, birleşik olaylar, sadece değişen bölgeyi yeniden çizme.)
- **Çizgi yumuşatma**: Titrek el yazısını hafif düzeltme (ayarlanabilir seviye).
- **Vektör kayıt**: Her vuruş nokta listesi olarak (x, y, basınç, zaman) saklanır → sonsuz yakınlaştırmada bile net, dosya küçük, sonradan düzenlenebilir.
- Araçlar: **Kalem, Fosforlu kalem** (yarı saydam, yazının altında kalır), **Silgi** (vuruş silgisi + kısmi silgi), **Kement seçim**, **Metin kutusu**, **Şekil**, **Lazer**.
- **Geri al / Yinele** (sınırsız; ayrıca iki parmakla dokunma = geri al, üç parmak = yinele).

### 2.2 Kalem / el ayrımı (istenen ana ayar)
Ayarlar → Giriş:
- **Sadece kalemle çiz** (varsayılan AÇIK): Parmak/avuç dokunuşu asla çizmez. Parmaklar yalnızca kaydırma, iki parmak yakınlaştırma yapar.
- **Parmakla çizime izin ver** (kalemi olmayan durumlar için).
- **Parmak hareketlerini tamamen kapat** (sayfa kaymasın istiyorsanız).
- **Kalem yan tuşu** → silgi / seçim / lazer olarak atanabilir.
- **Kalemin arka ucu (silgi ucu)** varsa otomatik silgi.
- **Avuç koruma şeridi**: Kalem ekrana yaklaştığında (hover) dokunmaları yok sayma.
- Solak / sağlak seçeneği (araç çubuğu konumu).

> Teknik not: Tüm modern platformlar dokunmanın **kalem mi, parmak mı** olduğunu bildirir (Web: `pointerType === "pen"`, Android: `TOOL_TYPE_STYLUS`, iPad: `UITouch.type == .pencil`, Flutter: `PointerDeviceKind.stylus`). Avuç reddinin en güvenilir yolu budur; tahmine dayalı algoritmaya gerek kalmaz.

### 2.3 Defterler ve sayfalar
- Yeni defter: **boş, çizgili, kareli, noktalı, müzik, koordinat düzlemi** şablonları; A4 / 16:9 (projeksiyon için) boyut.
- Sayfa ekle / sil / çoğalt / sırala, sayfa küçük resimleri (thumbnail) paneli.
- Koyu arka plan seçeneği (siyah tahta + tebeşir renkleri — projeksiyonda göz yormaz).
- Klasörler, defter kapağı/rengi, son açılanlar, arama (defter adına göre).
- **Otomatik kaydetme** (her vuruştan sonra), kapanmalara karşı kayıp yok.

### 2.4 PDF
- PDF içe aktar (dosyadan / paylaş menüsünden).
- Üzerine yazma; PDF'in **arasına boş sayfa ekleme** (soru çözmek için çok kullanışlı).
- Sayfa atlama, küçük resimler, yakınlaştırma.
- **Dışa aktarma**: yazılar vektör olarak orijinal PDF'in üzerine gömülü yeni bir PDF (öğrencilere gönderilebilir). Ayrıca PNG olarak tek sayfa dışa aktarma.
- Görsel (fotoğraf, ekran görüntüsü) ekleme.

### 2.5 Ders anlatımına özel özellikler
- **Lazer işaretçi**: Kalemle çizilen iz birkaç saniye sonra kaybolur; hiçbir şey kalıcı olarak yazılmaz.
- **Geçici kalem**: Yazdığı şey birkaç saniye sonra silinen kalem (vurgulamak için).
- **Sunum modu**: Araç çubukları gizlenir, tam ekran; ekran paylaşımında/HDMI'da temiz görüntü.
- **Hızlı renk çubuğu**: 4–6 sabit renk + 3 sabit kalınlık, tek dokunuşla geçiş.
- **Şekil tanıma**: Çizip bekleyince düz çizgi / daire / üçgen / dikdörtgen / ok'a dönüşür.
- **Cetvel, iletki, gönye** (geometri dersleri için) ve ızgaraya yapıştırma.
- **Sayfayı temizle** (tek dokunuş, geri alınabilir).
- **Yakınlaştırma penceresi**: Küçük alanda büyük yazıp sayfaya küçük aktarma (uzun yazılar için).
- İleride: ekran + ses kaydı ile **ders videosu** oluşturma, sayfa bazlı kronometre.

### 2.6 Ayarlar ekranı (özet)
- Giriş: sadece kalem, parmak hareketleri, yan tuş ataması, solak/sağlak
- Kalem: basınç duyarlılığı, yumuşatma seviyesi, varsayılan renk/kalınlıklar
- Görünüm: açık/koyu tema, varsayılan şablon, sayfa boyutu
- Şekil tanıma: açık/kapalı, bekleme süresi
- Lazer/geçici kalem: kaybolma süresi
- Yedekleme: tüm defterleri tek dosya olarak dışa/içe aktar

---

## 3. Teknoloji seçimi

Tabletin türü (iPad / Android / Windows) seçimi doğrudan etkiliyor.

| Seçenek | Artıları | Eksileri |
|---|---|---|
| **A) Web uygulaması (PWA)** — TypeScript + Canvas, PDF.js, pdf-lib | iPad, Android, Windows, bilgisayar — hepsinde çalışır. **Mağaza ve geliştirici ücreti yok** (Apple'ın yıllık 99$ ücreti dahil). Ana ekrana eklenip çevrimdışı çalışır. Güncellemesi anında. | Yerel uygulamaya göre gecikme biraz daha fazla olabilir (iyi optimizasyonla fark edilmez). Çok büyük PDF'lerde bellek sınırı. |
| **B) Flutter** — tek kod, Android + iPad + Windows | Yerel performans, dosya sistemine tam erişim, iyi kalem desteği. | iPad'e kurmak için ücretli Apple geliştirici hesabı gerekir (ücretsiz hesapla kurulan uygulama 7 günde bir yenilenmeli). |
| **C) Tamamen yerel** (Swift/PencilKit veya Kotlin) | En iyi gecikme ve platforma özel özellikler. | Tek platform; iki tablet için iki ayrı uygulama. |

**Önerim: A) Web uygulaması (PWA) ile başlamak.**
Ücretsiz olma amacına en uygun yol bu; her cihazda çalışır, mağazaya bağımlı değil. Yazma motoru platformdan bağımsız tasarlanırsa, ileride gerekirse Flutter/yerel sürüme geçmek mümkün.

### Önerilen yığın (PWA)
- **Dil / derleme**: TypeScript + Vite
- **Arayüz**: Hafif bir çerçeve (Svelte veya React) — çizim kısmı çerçeveden bağımsız, saf Canvas
- **Çizim**: HTML Canvas 2D, Pointer Events (`pointerType`, `pressure`, `getCoalescedEvents`, `getPredictedEvents`), düşük gecikmeli canvas (`desynchronized: true`)
- **Vuruş görünümü**: `perfect-freehand` kütüphanesi (basınca duyarlı, doğal görünen çizgiler)
- **PDF görüntüleme**: `pdf.js` (Mozilla)
- **PDF dışa aktarma**: `pdf-lib` (yazıları orijinal PDF'e vektör olarak ekler)
- **Depolama**: IndexedDB (defterler, vuruşlar, PDF dosyaları) — tamamen cihazda
- **Çevrimdışı**: Service Worker (PWA)
- **Test**: Vitest (birim), Playwright (uçtan uca)
- **Barındırma**: GitHub Pages (ücretsiz)

### Veri modeli (taslak)
```
Defter { id, ad, klasör, oluşturma/güncelleme tarihi, kaynakPdfId? }
  └─ Sayfa { id, sıra, şablon | pdfSayfaNo, genişlik, yükseklik }
       └─ Öğe (Vuruş | Metin | Görsel | Şekil)
            Vuruş { araç, renk, kalınlık, opaklık, noktalar: [x, y, basınç, t][] }
```
Sayfa başına öğeler ayrı saklanır; sadece değişen sayfa kaydedilir (büyük defterlerde hız için).

### Çizim mimarisi
- **3 katman**: (1) arka plan — şablon veya PDF sayfası (önbelleğe alınmış görüntü), (2) tamamlanmış vuruşlar, (3) "canlı" katman — o an çizilen vuruş. Böylece her kalem hareketinde sadece en üst katman çizilir → düşük gecikme.
- Yakınlaştırmada PDF sayfası yüksek çözünürlükte yeniden işlenir.

---

## 4. Yol haritası

Her aşamanın sonunda tablette gerçekten **derste kullanılabilir** bir sürüm olması hedeflenir.

### Aşama 0 — Hazırlık (1–2 gün)
- [ ] Proje iskeleti (Vite + TypeScript), kod biçimlendirme, test altyapısı
- [ ] GitHub Pages'e otomatik yayın (her push'ta tabletten açılabilir adres)
- [ ] **Tablet testi**: kalem olayları, basınç değerleri, kalem/parmak ayrımı tablette doğrulanır (küçük bir deneme sayfası)

### Aşama 1 — MVP: "Boş sayfaya kalemle yazabiliyorum" (1–2 hafta)
- [ ] Canvas çizim motoru, basınç duyarlı kalem, yumuşatma
- [ ] **Sadece kalem modu** + parmakla kaydırma / iki parmakla yakınlaştırma
- [ ] Kalem, fosforlu kalem, vuruş silgisi
- [ ] Renk ve kalınlık seçimi (hızlı çubuk)
- [ ] Geri al / yinele
- [ ] Çok sayfalı defter, sayfa ekle/sil, şablonlar (boş, çizgili, kareli, noktalı)
- [ ] IndexedDB'ye otomatik kayıt, defter listesi ekranı
- [ ] PWA: ana ekrana ekleme, çevrimdışı çalışma

**Çıktı:** Derste beyaz tahta yerine kullanılabilir.

### Aşama 2 — PDF desteği (1–2 hafta)
- [ ] PDF içe aktarma ve görüntüleme (pdf.js)
- [ ] PDF sayfalarının üzerine yazma
- [ ] PDF arasına boş sayfa ekleme
- [ ] Sayfa küçük resimleri paneli, sayfaya atlama
- [ ] **Yazılı PDF olarak dışa aktarma** (pdf-lib) ve paylaşma
- [ ] Büyük PDF'lerde performans (sadece görünen sayfaları işleme)

**Çıktı:** Ders notu / soru PDF'i açıp üzerinde çözüm yapılabilir, öğrencilere gönderilebilir.

### Aşama 3 — Ders anlatımı araçları (Sürüm 1, 2–3 hafta)
- [ ] Lazer işaretçi ve geçici kalem
- [ ] Sunum modu (tam ekran, araçlar gizli)
- [ ] Kısmi silgi, kement seçim (taşı / boyutlandır / renk değiştir / kopyala)
- [ ] Şekil tanıma (çizgi, daire, üçgen, dikdörtgen, ok)
- [ ] Cetvel ve iletki
- [ ] Kalem yan tuşu ataması, iki parmak dokunma = geri al
- [ ] Ayarlar ekranının tamamı
- [ ] Koyu (kara tahta) sayfa arka planı, 16:9 sayfa boyutu
- [ ] Metin kutusu ve görsel ekleme

### Aşama 4 — Düzen ve güvence (1 hafta)
- [ ] Klasörler, defter arama, sıralama
- [ ] Tüm verinin yedeğini tek dosya olarak alma / geri yükleme
- [ ] Tarayıcı depolamasının silinmemesi için kalıcı depolama izni (`navigator.storage.persist`)
- [ ] Hata durumlarında veri kaybını önleme testleri

### Aşama 5 — Sürüm 2 (isteğe bağlı, ihtiyaca göre)
- [ ] Sonsuz tuval (sınırsız beyaz tahta)
- [ ] Ses kaydı ve yazıyla senkron oynatma
- [ ] Ders kaydı: sayfa + ses → video dışa aktarma
- [ ] Katmanlar
- [ ] Yakınlaştırma penceresiyle yazma
- [ ] Yerel klasöre / Google Drive'a otomatik yedekleme
- [ ] Gerekirse Flutter veya yerel uygulamaya taşıma (gecikme yetersiz kalırsa)

---

## 5. Riskler ve önlemler

| Risk | Önlem |
|---|---|
| Kalem gecikmesi hissedilir olabilir | Aşama 0'da tablette ölçülür; 3 katmanlı çizim, tahmini noktalar, `desynchronized` canvas. Yetersizse yazma motoru Flutter'a taşınır. |
| Tarayıcı verileri silebilir | Kalıcı depolama izni + düzenli yedek dışa aktarma hatırlatması. |
| Büyük PDF'lerde bellek | Sadece ekrandaki ve komşu sayfaları işleme, diğerlerini bellekten atma. |
| iPad Safari kısıtları | Her aşamada iPad'de test; Safari'de olmayan API'ler için yedek yol. |

---

## 6. Netleştirilmesi gerekenler
1. **Tablet modeli ve işletim sistemi** (iPad / Samsung-Android / Windows) ve kalem modeli (Apple Pencil, S Pen vb.)
2. Dersleri nasıl gösteriyorsunuz? (Projeksiyon/HDMI, ekran paylaşımı — Zoom/Meet, kayıt alıp video olarak)
3. Hangi ders? (Matematik/geometri ise cetvel, iletki, koordinat şablonları öne alınır)
4. PDF'ler genelde kaç sayfa? (Performans hedefi için)
