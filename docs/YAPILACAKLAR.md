# Yapılacaklar (sıradaki oturum)

Kullanıcı geri bildirimi — sürüm 12 sonrası. Sırayla yapılacak.

## A. Hatalar
1. **Sunum modundaki panel ekran dışına atılabiliyor ve geri gelmiyor.** Sürüklenebilir panellerin (araç menüsü, sayfa gezgini, sunum paneli) konumu ekranın içinde kalacak şekilde sınırlanmalı. Panelin gerçek boyutu ölçülüp kenarlara göre kısıtlanmalı (şu an sadece kaba bir sınır var). Ekran döndürülünce de içeri çekilmeli.
2. **Xiaomi Focus Pen Pro çift dokunuşu çalışmıyor.**
   - Kalemden gelen tüm olayları (tuş kodu, MotionEvent buton durumu, genel hareket olayları) ekranda listeleyen bir **"Kalem testi" ekranı** eklenmeli. Kullanıcı çift dokunup ne geldiğini görebilsin.
   - `onGenericMotionEvent`, `ACTION_BUTTON_PRESS` ve `KEYCODE_*` olayları dinlenmeli.
   - HyperOS çift dokunuşu sistem düzeyinde kullanıyor olabilir. Gerçekten uygulamaya hiçbir olay gelmiyorsa bunu kullanıcıya açıkça söylemek gerekir.
3. **Yeni sayfa ve hızlı not kara tahta açılıyor.** Yeni defter, hızlı not ve yeni sayfa her zaman **düz beyaz** olmalı. Eski `newDark` / `newTemplate` ayarları yok sayılmalı.

## B. Ana sayfa
4. Yeni defter (ve yeni sayfa) oluştururken **ad sorulmasın**: hemen "Adsız defter" açılsın, ad defterin içinde değiştirilsin.
5. **Toplu seçim**: uzun bas → seçim modu → çoklu seçip toplu sil, toplu klasöre taşı, toplu favori.
6. **Görünüm seçenekleri**: üstte küçük bir simge ile küçük ızgara, orta ızgara, büyük ızgara ve liste görünümü. Varsayılan kapaklar şimdikinden daha küçük olmalı.
7. **Klasör renkleri**: her klasöre kullanıcının seçtiği renk (simge renklendirilir).
8. Editörden ana sayfaya dönünce **"Kaydedildi" bildirimi** (kısa toast).

## C. Editör
9. **Üstteki renk/kalınlık çubuğunu kaldır.** Ekranda yer kaplamasın. Araç menüsünde **kaleme (veya başka bir araca) çift dokununca** o araca ait seçenekler (renk, kalınlık, kayıtlı kalemler, palet düzenleme) küçük bir açılır pencerede çıksın.
10. **Menü küçültülünce** kalem ve silgi görünsün (ikisi arasında hızlı geçiş), sadece seçili araç değil.
11. **Sunum panelini dikey yapabilme**: panel ekranın sol veya sağ kenarına taşınınca otomatik olarak dikey dursun, alt veya üst kenarda yatay.
12. **Sayfa boyutu seçeneği** ("Sayfa" panelinde): A4 dikey, A4 yatay, 16:9 ve **"Tam ekran"** (tabletin ekran oranında sayfa). Mevcut sayfaya ve yeni sayfalara uygulanabilsin.
13. **Kağıt arka planı** daha esnek olsun: kağıt rengi için hazır 5 rengin yanında **özel renk seçici** (HSV), ayrıca masa (sayfa dışı) rengi.

## D. Yeni bölüm
14. **Sol kenarda "Yapılacaklar" sekmesi** (ana sayfa ve editörde açılır kapanır panel):
    - Görev ekle, işaretle, sil, sürükleyerek sırala.
    - İsteğe bağlı tarih, öncelik rengi.
    - Görevi bir deftere veya sayfaya bağlama ("şu deftere git").
    - Kalemle yazılan görev de olabilir (küçük el yazısı alanı). İlk sürümde klavyeyle olur.
    - Veriler cihazda ayrı bir dosyada tutulsun ve yedeğe dahil edilsin.

## E. Temalar
15. **Hazır temalar**: Varsayılan (indigo), Okyanus (mavi-turkuaz), Orman (yeşil), Gün batımı (turuncu-mor), Kağıt (krem-kahve), Gece (koyu), Yüksek kontrast. Her tema ana renk, yüzeyler, masa rengi ve kapak renklerini değiştirir. Ayarlar › Görünüm'de önizlemeli seçim.

## F. Onaylanan ek özellikler (el yazısını yazıya çevirme hariç hepsi)
- Sayfa içi **arama** (metin kutuları, formüller, PDF metni)
- **Ders programı**: haftalık program, derse göre defter açma
- **Şablon defterler**: sık kullanılan sayfa düzenlerini kaydetme
- **Soru kutuları**: "Soru 1, 2, 3" alanları ekleme
- **Sayfa numarası ve tarih**: köşeye otomatik
- **Ders videoları listesi**: uygulama içinden açma, paylaşma
- **Çözüm katmanı**: çözümü gizle/göster; soruları boş + çözümler ayrı PDF
- **Kalem basıncı eğrisi** ayarı

## G. PDF düzenleme
- **Birleştirme**: birden fazla PDF'i (veya defteri) tek PDF/defter yapma
- **Ayırma**: sayfa aralığını ayrı PDF/deftere çıkarma ("3-7, 12")
- **Başka PDF'ten sayfa ekleme** (araya/sona)
- **Sayfa sırası**: sürükle-bırak ile sıralama (küçük resimlerde)
- **Toplu işlem**: seçili sayfaları döndür / sil / çoğalt / dışa aktar
- **Boş sayfa ekleme** seçili desenle (mevcut)
- **PDF sıkıştırma** (görselleri küçülterek dosya boyutu düşürme)
- **PDF'te metin vurgulama**: metni seçip vurgulama/altını çizme (PDF metin katmanı varsa)
- **Görsellerden PDF**: fotoğraflardan (ör. çözülmüş kağıt) PDF oluşturma, kenar kırpma

## H. Hazır kütüphane (tek dokunuşla eklenen hazır çizimler)
Kenarda açılır "Kütüphane" paneli, kategorilere ayrılmış, aranabilir; eklenen her şey vektör ve düzenlenebilir (taşı, ölçekle, renk).
- **Trigonometri**: birim çember (açılar + sin/cos değerleri), 30-60-90 ve 45-45-90 üçgenleri (kenar oranlarıyla), trigonometrik oranlar tablosu, sin/cos/tan grafikleri
- **Geometri**: dik üçgen (Pisagor), ikizkenar/eşkenar üçgen, açıortay/kenarortay/yükseklik gösterimli üçgen, çemberde açılar (merkez/çevre açı), teğet-kiriş, paralel doğrular ve kesen (iç ters/yöndeş açılar), dörtgen çeşitleri, düzgün çokgenler
- **Analitik**: koordinat ekseni (ayarlanabilir aralık), doğru/parabol/çember denklemleri şablonları, sayı doğrusu (tam sayı / kesir / ondalık), aralık gösterimi
- **Katı cisimler**: küp, prizma, piramit, silindir, koni, küre (açınımlarıyla birlikte)
- **Olasılık-istatistik**: olasılık ağacı, Venn şemaları (2-3 küme), zar/para/kart görselleri, normal dağılım eğrisi, kutu grafiği, sütun/pasta grafik şablonu
- **Genel**: tablo (satır-sütun), ok ve süslü parantez işaretleri, kavram haritası kutuları, zaman çizelgesi
- **Formül kartları**: sık formüller (alan/hacim, türev/integral kuralları, logaritma özellikleri, özdeşlikler)
- **Kullanıcı kütüphanesi**: kendi seçtiğin çizimi "Kütüphaneye kaydet" ile ekleme

## I. Onaylanan ders/çalışma özellikleri
1. **Hareketli geometri**: köşeleri sürüklenebilen şekiller; açı ölçüleri, kenar uzunlukları, alan/çevre canlı güncellenir. Noktalar arası bağlar korunur (ör. üçgenin köşesini çekince açıortay da güncellenir). Önce üçgen, dörtgen, çember + açı/uzunluk/alan etiketleri; sonra paralel, dik, orta nokta, açıortay gibi bağlı yapılar.
2. **Bilgi kutuları (Markdown benzeri bloklar)**: kement ile bir bölgeyi seçip "Kutuya çevir" → Kural / Tanım / Örnek / Dikkat / Formül / Özet başlıklı, renkli çerçeveli bir blok olur (el yazısı aynen kalır, düzenli bir kutuya alınır). Bloklar taşınabilir, daraltılabilir.
   - **Konu özeti**: defterdeki bütün kutular tek tıkla bir "Özet" sayfasında toplanır (kaynak sayfaya bağlantılı); PDF olarak paylaşılabilir.
   - El yazısını yazıya çevirme istenmediği için kutu içeriği el yazısı olarak kalır; istenirse kutuya klavyeyle başlık/alt yazı eklenir.
3. **Yanlış defteri**: PDF'ten (veya herhangi bir sayfadan) soru çözerken "Yanlış defterine ekle" aracıyla soruyu **dikdörtgen içine al → otomatik eklenir** (o bölgenin görüntüsü + üzerine yazılanlar, kaynak defter/sayfa, tarih). Ayrı bir "Yanlış defteri" defterinde her soru bir kart olarak durur: konu etiketi, "çözüldü / tekrar" işareti, kaynağa git, **sil**, tekrar modu (çözüm gizli, boş alanda yeniden çöz).
4. **Kişisel formül kağıdı**: kütüphanedeki formül kartlarından seçerek düzenli bir formül sayfası oluşturma; kendi formüllerini ekleme; tek sayfa PDF.

5. **Cebir karoları**: x², x, 1 (ve y, xy) karoları; sürükle-bırak, döndür, negatif (kırmızı) karolar; dikdörtgen oluşturup çarpanlara ayırma / özdeşlik gösterimi.
6. **Kesir modelleri**: pasta ve çubuk modelleri (pay/payda ayarlanır, parçalar boyanır), sayı doğrusunda kesir; denk kesirleri yan yana gösterme.
7. **Kavram haritası**: kutular (el yazısı veya klavye) ve aralarında oklar; kutular taşınınca oklar takip eder, oka etiket yazılabilir.
8. **Değer tablosundan grafik**: x-y tablosu gir → noktalar koordinat düzlemine işlenir, istenirse noktalar birleştirilir veya doğru çizilir; tablo sayfada düzenlenebilir kalır.

## İlke
- **Yapay zeka desteği yok.** Uygulama sade kalacak; öğrenci ve öğretmen dikkat dağıtmayan, temiz bir alanda çalışacak.
- Kullanıcının onaylamadığı öneriler listeden çıkarıldı (sınav kağıdı oluşturucu, adım adım çözüm, kaydırıcılı grafik, bölünmüş ekran, hesap makinesi, sınıf listesi, takvim, bilgi kartları, pomodoro, örnekten alıştırma, adım kontrolü, animasyonlar vb.). "F. Onaylanan ek özellikler" bölümündekiler (arama, ders programı, şablon defterler, soru kutuları, sayfa no/tarih, video listesi, çözüm katmanı, basınç eğrisi) ile G (PDF düzenleme) ve H (hazır kütüphane) geçerli.

## M. Rahat çalışma ortamı (onaylandı)
1. **Kaldığın yerden devam**: defter son bakılan sayfa ve yakınlaştırmada açılır (defter başına kaydedilir).
2. **Çöp kutusu**: silinen defterler ve sayfalar 30 gün boyunca geri getirilebilir; ana sayfada "Çöp kutusu" bölümü, kalıcı silme ve boşaltma.
3. **Yazarken otomatik yeni sayfa**: son sayfanın alt kısmına yazınca yeni (düz beyaz) sayfa eklenir. Ayarlardan kapatılabilir.
4. **Yakınlaştırma kilidi + "sayfaya sığdır"**: kilitliyken parmak hareketi yakınlaştırmaz (sadece kaydırır); tek dokunuşla sayfa genişliğine dönme.
5. **Arayüz boyutu**: küçük / normal / büyük (düğmeler, yazılar, menüler).
6. **Silgiden otomatik dönüş**: silgiyle sildikten sonra kaleme kendiliğinden döner; Ayarlar'dan kapatılabilir.
7. **Odak modu (sunum moduyla birleşik)**: tek dokunuşla bütün menüler gizlenir; kenara alınabilen küçük panelde sadece kalem, silgi, geri al (+ lazer, renkler). Mevcut "sunum modu" bu modla değiştirildi (kullanıcı onayladı, yapıldı).
8. **Göz dostu ayarlar**: gece kağıdı + sıcak tonlu masa, koyu kağıtta okunaklı kalem renk seti, koyu temayla birlikte otomatik.

9. **Sağ kenarda sayfa çubuğu (otomatik gizlenen)**: normalde görünmez; sayfa kaydırılmaya başlanınca sağ kenarda belirir, sürüklenerek hızlı gezinilir (sayfa numarası balonuyla), kaydırma bitince birkaç saniyede kaybolur.

(N bölümündeki diğer öneriler kullanıcı tarafından istenmedi.)

## O. Temel eksikler (onaylandı)
1. **Yazdır**: Android yazdırma ekranıyla doğrudan yazıcıya (sayfa aralığı seçerek).
2. **Depolama yönetimi**: defter başına kaplanan alan; kullanılmayan görselleri, eski yedekleri, çöp kutusunu temizleme.
3. **Çökme kurtarma**: beklenmedik kapanmada son açık defter ve yarım kalan çizim korunur; hata kaydı cihazda dosyaya yazılır, kullanıcı isterse paylaşır (sunucu yok).

(Güncelleme bildirimi Play Store'a geçince gereksiz; ilk açılış tanıtımı, kapak ve ayar yedeği P bölümünde mağaza açısından yeniden değerlendirildi.)

## P. Güvenlik gözden geçirmesi (yapılacak)
- **İmza anahtarı**: depodaki `app.keystore` herkese açık → mağaza için yeni, gizli bir yükleme anahtarı (Play App Signing); depodaki anahtar sadece test sürümleri için kalır.
- **İnternet izni yok**: uygulama hiçbir sunucuya bağlanmıyor ve INTERNET izni istemiyor; bu korunacak (mağazada "veri toplanmıyor" beyanı bunun üzerine kurulu).
- **FileProvider yolları** daraltılacak (şu an tüm cache ve files klasörü paylaşılabilir durumda → sadece dışa aktarma ve kamera klasörü).
- **Dışarıdan gelen dosyalar** (PDF açma, paylaş, yedekten geri yükleme): boyut sınırı, dosya türü kontrolü, zip içinde yol atlama (`..`) ve "zip bombası" koruması, bozuk PDF'te çökmeme.
- **Defter kilidi**: PIN deneme sınırı / bekleme süresi; kilit sadece uygulama içi olduğu kullanıcıya açıkça söylenecek.
- **Android otomatik yedekleme kuralları** (`dataExtractionRules`): kilitli defterler ve gereksiz önbellek cihaz dışı yedeğe girmesin.
- **İzinler**: yalnızca mikrofon (ders kaydı, isteğe bağlı); kamera izin istemeden sistem kamerasıyla.
- Bağımlılıkların güncel ve bilinen açığı olmayan sürümleri.

## Q. Play Store'a hazırlık
- **AAB paketi** ve güncel `targetSdk` (mağazanın o yılki zorunluluğu).
- **İngilizce dil desteği** (onaylandı): tüm metinler `strings.xml`'e; Türkçe + İngilizce, telefon diline göre.
- **Gizlilik politikası sayfası** (onaylandı): "Hiçbir veri toplanmaz, her şey cihazda kalır" (uygulama içinde + GitHub Pages).
- **Hakkında sayfası** (onaylandı): sürüm, kısa tanıtım, gizlilik politikası, "Sorun bildir" (e-posta uygulamasıyla, istenirse hata kaydı ekli), en altta **açık kaynak lisansları** (PDFBox — Apache 2.0 vb.).
- **Ayarların yedeğe girmesi** (onaylandı): paletler, kayıtlı kalemler, klasör renkleri, ayarlar.
- **Veri güvenliği formu**: veri toplanmıyor / paylaşılmıyor.
- **Telefon ekranlarına uyum**: dar ekranda menülerin sığması.
- **Erişilebilirlik modu** (onaylandı, Ayarlar'dan açılır): açılınca düğmeler ve dokunma alanları büyür, yazılar büyür, kontrast artar (M5 "arayüz boyutu" ile birlikte tasarlanacak). Ayrıca her düğmeye arka planda anlamlı bir ad verilir; Android'in kendi ekran okuyucusu **TalkBack** açıksa bunları sesli okur (uygulama kendisi ses çıkarmaz).
- **EN SON yapılacaklar** (özellikler bittikten sonra): **ilk açılış tanıtımı** (kesin yapılacak), **mağaza görselleri** (simge, tanıtım görseli, ekran görüntüleri, TR+EN açıklama).
- **Kategori**: Eğitim; "çocuklara yönelik" işaretlenmeyecek, herkes için uygun derecelendirme.
- **Sürüm numaralandırma**: 1.0, 1.1... ve uygulama içinde "Yenilikler" notu.

## R. Defter kilidi — şifre unutulursa (yapılacak)
- **Cihaz kilidiyle açma (öncelikli yöntem)**: defter kilidi tabletin kendi ekran kilidini kullanır (parmak izi / yüz / cihaz PIN'i). Kullanıcı ayrı bir şifre ezberlemez; tableti açabilen defteri de açar. Ek izin gerekmez (Android'in biyometrik/cihaz kimlik doğrulama ekranı).
- **Ayrı PIN seçilirse**: kilitlerken bir kez **kurtarma kodu** gösterilir (ör. 8 karakter), "not edin / yazdırın" uyarısıyla. PIN unutulursa kurtarma kodu veya cihaz kilidiyle açılır, yeni PIN belirlenir.
- Hiçbiri yoksa: veriler şifrelenmediği için kaybolmaz; "kilidi sıfırla" yalnızca cihaz kilidi doğrulamasıyla mümkün olur (tableti açamayan kişi sıfırlayamaz).
- Kullanıcıya açıkça söylenecek: kilit, tableti başkasına verdiğinizde meraklı gözlere karşıdır; dosyaları şifrelemez.

## S. Kontrol listesi (onaylandı)
- **Döndürme ve bölünmüş ekran**: yatay/dikey geçişte ve başka uygulamayla yan yana açıkken hiçbir çizim kaybolmasın, menüler sığsın.
- **Klavye kısayolları**: Ctrl+Z / Ctrl+Y, Ctrl+C / Ctrl+V / Ctrl+X, Sil, sayfa ileri/geri, Ctrl+S yok (otomatik kayıt).
- **Pil**: ekran açık tutma yalnızca defter içinde; arka planda iş yok (otomatik yedek uygulama açılışında, kısa sürer).
- **Eski defterler**: her sürüm eski defterleri açabilmeli; her format değişikliği için birim testi zorunlu.
- **Gerçek cihazda performans**: 300-500 sayfalık PDF, 45+ dakikalık ders kaydı.

## T. Farklı tabletlerde çalışma (onaylandı)
- **Kalemler**: kalem desteği Android'in standart kalem olaylarıyla yapılıyor (kalem/parmak ayrımı, basınç, eğim, silgi ucu); marka kütüphanesi kullanılmıyor. Bu yüzden Samsung S Pen, Xiaomi, Lenovo, Huawei, Honor, USI kalemler gibi standart olay gönderen kalemlerin hepsinde yazma çalışır. Farklılık sadece **kalem tuşlarında** olur: bazı markalar tuşu sistem için ayırır. Bunun için "Kalem testi" ekranı ve "Tuşu öğret" var; basınç hissi için "basınç eğrisi" ayarı her kaleme uyarlanabilir.
- **Ekran boyutları**: arayüz sabit piksel değil, ekrana göre yerleşen (Compose) düzenle yapılıyor. Kontrol için CI'da **birden fazla emülatörde** test ve ekran görüntüsü: 8" küçük tablet, 11" tablet (Pixel Tablet), 12-13" büyük tablet, telefon, katlanabilir; dikey ve yatay. Dar ekranda menüler kaydırılabilir/daraltılabilir olmalı.
- **Android sürümleri**: en az Android 10 (piyasadaki cihazların büyük çoğunluğu). CI'da Android 10 ve en yeni Android emülatöründe test.
- **Düşük donanım**: az RAM'li ucuz tabletlerde büyük PDF'te bellek sınırı, ders kaydında düşük çözünürlüğe otomatik geçiş; düşük bellekli emülatör testi.
- **Ekran yenileme hızları**: 60 / 90 / 120 / 144 Hz ekranlarda kalem gecikmesi kontrolü.

## U. Beklemede
- **Ders kaydı (video)**: geliştirilmeyecek; ileride tamamen kaldırılabilir (Android'in kendi ekran kaydı aracı kullanılabilir). Karar sonra.

## Bu oturumda yapılanlar (sürüm 13+)
- A1 panellerin ekran dışına kaçması düzeltildi (gerçek boyuta göre sınır; eski kayıtlı konum ekran dışındaysa içeri çekilir)
- A3 yeni defter / hızlı not / yeni sayfa her zaman düz beyaz
- B4 yeni defter ad sormadan açılır
- B8 "Kaydedildi" bildirimi
- B5 toplu seçim (uzun bas veya "Seç": toplu sil / klasöre taşı / favori)
- B6 görünüm seçenekleri (küçük / orta / büyük ızgara, liste)
- B7 klasör renkleri
- C9 üstteki renk çubuğu kaldırıldı; seçili araca tekrar dokununca seçenekler penceresi açılır
- C11 + M7 odak modu (sunum modunun yerini aldı); panel sağ/sol kenarda dikey durur
- C12 sayfa boyutu (A4 dikey/yatay, 16:9, tam ekran)
- C13'ün bir kısmı: özel kağıt rengi (masa rengi hâlâ yapılacak)
- M3 yazarken otomatik yeni sayfa (Ayarlar › Yazı'dan kapatılabilir; PDF sayfalarında çalışmaz)
- M4 yakınlaştırma kilidi + "Sayfaya sığdır" (⋮ menüsü ve Ayarlar)
- C13 masa rengi (Sayfa paneli)
- M9 sağ kenarda otomatik gizlenen sayfa çubuğu
- M2 çöp kutusu (defterler 30 gün; sayfa silme zaten Geri al ile geri alınıyor)
- E renk temaları: Varsayılan, Okyanus, Orman, Gün batımı, Kağıt, Gece, Yüksek kontrast
- D Yapılacaklar paneli (ana sayfa ve defter içi; renk, sıralama, deftere/sayfaya bağlama, yedeğe dahil). Tarih ve sürükleyerek sıralama sonra eklenebilir.
- H hazır kütüphane (~80 çizim: trigonometri, geometri, analitik, katı cisimler + açınımlar, olasılık-istatistik, cebir karoları, formül kartları, genel; arama; "Benim çizimlerim")
- I3 yanlış defteri (dikdörtgenle seç → ayrı "Yanlış defteri"ne kart; "Kaynağa git"). Etiketle filtreleme ve tekrar modu sonra.
- I4 formül kartları (kütüphanede; kişisel formül kağıdı bunlarla sayfaya dizilerek yapılır)
- I5 cebir karoları, I6 kesir modelleri (pasta, çubuk, sayı doğrusu, denk kesir), I8 değer tablosundan grafik
- G birleştirme, ayırma ("3-7, 12"), başka PDF'ten sayfa ekleme, fotoğraflardan defter. Sıkıştırma, PDF metni vurgulama, sürükle-bırak sıralama sonra.
- F arama (metin kutuları, formüller, PDF yazısı); soru alanları kütüphanede (Genel)
- O1 yazdır, O2 depolama yönetimi, O3 hata kaydı (cihazda; paylaşmak isteğe bağlı)
- Seçim çubuğunda renkler ve "Kütüphaneye kaydet"
- Yanlış defteri sol menüde; Ders programı; Şablon defterler
- I1 hareketli geometri (seçili şeklin kenar/açı/alan/çevre canlı ölçümü), I2 bilgi kutuları + konu özeti
- F sayfa no + tarih, çözüm katmanı (renge göre gizle; soru kağıdı olarak dışa aktar), basınç eğrisi
- M5 arayüz boyutu (Çok büyük = erişilebilirlik), M8 göz dostu mod
- P güvenlik: internet izni kesin kapalı, Android bulut yedeği kapalı, FileProvider yalnız geçici dosyalar, yedek geri yüklemede klasör dışı/zip bombası koruması, PDF boyut sınırı, PIN deneme sınırı, file:// açma kapalı
- R kilit kurtarma: cihazın ekran kilidiyle
- Q hakkında, gizlilik politikası (docs/GIZLILIK.md), lisanslar, ayarların yedeğe girmesi, AAB (CI'da), telefonda daraltılabilir sol panel
- S klavye kısayolları; T elle çalıştırılan "Cihaz uyumluluğu" iş akışı (Android 10 telefon, 12 ve 14 tablet)
- Kontrol listesi: docs/KONTROL_LISTESI.md
- A2'nin ilk adımı: Ayarlar › Kalem ve el › "Kalem testi" ekranı. Kullanıcı Focus Pen Pro ile deneyip sonucu bildirecek; sonuca göre çift dokunuş bağlanacak.
- C10 küçük menüde kalem + silgi
- M1 kaldığın sayfadan açılma
- M6 silgiden otomatik dönüş (Ayarlar'dan kapatılabilir)

## L. Kontrol
- Her değişiklikten sonra emülatör testi ve ekran görüntüsü incelemesi.
- Yeni özellikler için duman testleri.
