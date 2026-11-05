# Note — Kurulum ve Kullanım

## Tablete kurulum (Xiaomi Pad 7 ve diğer Android tabletler)

1. Tabletten GitHub'daki depo sayfasını açın → **Releases** (Sürümler) bölümü → en üstteki **Note sürüm N** → `Note-N.apk` dosyasını indirin.
2. İndirilen dosyaya dokunun. "Bilinmeyen kaynaklardan yükleme" izni sorulursa tarayıcıya (Chrome) izin verin.
3. Yeni sürüm çıktığında aynı şekilde indirip kurun: **eskisinin üzerine kurulur, defterleriniz silinmez.**

> HyperOS ilk kurulumda bir güvenlik taraması yapabilir; "Yine de yükle" deyin.

## Defterler

- **Yeni defter**: Ad, şablon (boş, çizgili, kareli, noktalı, izometrik, koordinat düzlemi, milimetrik), boyut (A4 dikey/yatay, 16:9) ve "kara tahta" (koyu sayfa) seçilir.
- **PDF aç**: PDF kopyalanıp yeni bir deftere dönüştürülür; orijinal dosyaya dokunulmaz. Başka bir uygulamadan "Paylaş → Note" ile de açılabilir.
- Deftere uzun basınca: yeniden adlandır, sil.
- Her çizgi anında kaydedilir; "kaydet" düğmesi yoktur.

## Kalem ve el

| Ayar | Ne yapar |
|---|---|
| Sadece kalemle çiz (varsayılan açık) | El/avuç asla çizmez. Parmakla sayfa kaydırılır, iki parmakla yakınlaştırılır. |
| Kalem yakınken el yok sayılır | Kalem ekrana değerken ya da ekranın hemen üzerindeyken avuç dokunuşları tamamen görmezden gelinir. |
| İki parmakla dokun | Geri al. Üç parmakla dokun: Yinele. |
| Kalem tuşu | Yan tuşa basılıyken: silgi / kement seçim / lazer (Ayarlar'dan seçilir). |

## Araç çubuğu (solda; solak modunda sağda)

- **Kalem**: basınca duyarlı. Tekrar dokununca: basınç ve şekil ayarları.
- **Fosforlu kalem**: yarı saydam, her zaman yazının altında kalır.
- **Silgi**: tekrar dokununca "bütün çizgiyi sil / kısmi sil" ve boyut seçilir.
- **Seçim (kement)**: yazıların etrafını çizin → taşıyın, sağ alttaki noktadan büyütün/küçültün, üstteki noktadan döndürün. Alttaki çubuktan sil/çoğalt; renge dokunarak rengini değiştirin.
- **Geometri** (tekrar dokununca liste açılır):
  - *Şekil kalemi*: her çizimi düzgün şekle çevirir.
  - *Doğru parçası*: uzunluğu (cm) ve açıyı gösterir; köşelere, ızgaraya ve 15°'lik açılara yapışır.
  - *Ok*, *Düzgün çokgen* (3-10 kenar), *Pergel*, *Geometri işareti* (dik açı karesi, açı yayı, eşit kenar çentiği, paralel oku).
- **Metin**: sayfaya dokunun, yazın. Var olan metne dokunarak düzenleyin.
- **Lazer**: iz birkaç saniyede kaybolur; kalem ekrana değmeden üzerinde tutulduğunda da kırmızı nokta görünür.
- **Geçici kalem**: yazdığınız birkaç saniye sonra silinir (vurgulamak için).
- **Araçlar**: cetvel, iletki, gönye (45° ve 30°-60°). Parmakla taşıyın, iki parmakla döndürün (15°'lik açılara yapışır). Kalemi kenara yaklaştırınca düz çizer ve uzunluğu gösterir. İletkide kalemin açısı canlı gösterilir.
- **Renkler**: dokun = seç, uzun bas = o rengi değiştir.
- **Kalınlıklar**: 3 hazır kalınlık.

### Çizip bekle → şekil
Kalemle bir çizgi/çember/üçgen çizip kalemi kaldırmadan bir an bekleyin: düzgün şekle dönüşür (titreşim hissedersiniz). Düz çizgide kalemi hareket ettirmeye devam ederek ucunu ayarlayabilirsiniz.

### Pergel
1. Kalemi merkeze koyup yarıçap kadar çekin, bırakın.
2. Kalemi merkez etrafında döndürerek yayı çizin. Tam çember için bir kez dokunun.

## Sayfalar ve PDF

- Üstteki **`12 / 87`** göstergesine dokunun → sayfa numarasını yazın → **Git**.
- **Kitap sayfa numarası**: PDF'in 1. sayfası kitabın 1. sayfası değilse (kapak, önsöz), aynı pencerede "Kitap sayfa numarasını ayarla" → "Şu an açık sayfa kitapta kaçıncı sayfa?" sorusunu bir kez cevaplayın. Sonra "Kitap sayfası" seçeneğiyle öğrencinin söylediği sayfaya doğrudan gidersiniz.
- Bir sayfaya atladıktan sonra üstteki **‹ Geri dön** ile önceki sayfaya dönersiniz.
- **+**: bulunduğunuz sayfanın arkasına boş sayfa ekler (PDF'in arasına soru çözmek için).
- **Sayfalar** (ızgara simgesi): küçük resimler. Uzun bas: ekle, çoğalt, yukarı/aşağı taşı, sil.
- **Yer imleri**: sayfalara ad verin ("Türev", "Test 3"). PDF'in kendi içindekiler listesi de buradadır.
- **Diğer (⋮) → Sayfa şablonu**: sayfanın şablonunu değiştirin veya farklı şablonla yeni sayfa ekleyin.

## Ders anlatımı

- **Sunum modu** (tam ekran simgesi): araç çubukları ve sistem çubukları gizlenir, köşede küçük bir araç paneli kalır.
- **Projeksiyon / HDMI**: tablet harici bir ekrana bağlandığında projeksiyonda **yalnızca sayfa** görünür, menüler sadece tablette kalır (Ayarlar → İkinci ekran). Kablosuz yansıtma tüm ekranı gösterir; o durumda sunum modunu kullanın.
- **Ders kaydı** (kırmızı daire): sayfa görünümü + mikrofon sesi MP4 olarak kaydedilir. Duraklatılabilir. Bitince video **Galeri › Movies/Note** klasörüne kaydedilir.

## Dışa aktarma

**Paylaş** simgesi → Tüm defter (PDF) / Bu sayfa (PDF) / Bu sayfa (PNG) → **Paylaş** (WhatsApp, Drive...) veya **Dosyaya kaydet**.
Yazılar PDF'e vektör olarak eklenir; yakınlaştırınca bulanıklaşmaz.

## Bilinen sınırlar (şimdilik)

- Klasörler ve tüm verinin yedeğini alma Aşama 5'te.
- Kalem tuşlarının Xiaomi kalemde nasıl bildirildiği cihazda denenmeli; çalışmazsa Ayarlar'dan değiştirilebilir veya bildirin.
- Koordinat düzlemi şablonundaki sayılar PDF dışa aktarmada yer almaz (ızgara ve eksenler yer alır).
