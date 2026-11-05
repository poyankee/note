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
7. **Odak modu (sunum moduyla birleşik)**: tek dokunuşla bütün menüler gizlenir; kenara alınabilen küçük panelde sadece kalem, silgi, geri al (+ lazer, renkler). Mevcut "sunum modu" bu modla değiştirilir; projeksiyonda zaten ikinci ekran temiz görünüyor. (Öneri: birleştirilsin — kullanıcı onayı bekleniyor.)
8. **Göz dostu ayarlar**: gece kağıdı + sıcak tonlu masa, koyu kağıtta okunaklı kalem renk seti, koyu temayla birlikte otomatik.

9. **Sağ kenarda sayfa çubuğu (otomatik gizlenen)**: normalde görünmez; sayfa kaydırılmaya başlanınca sağ kenarda belirir, sürüklenerek hızlı gezinilir (sayfa numarası balonuyla), kaydırma bitince birkaç saniyede kaybolur.

(N bölümündeki diğer öneriler kullanıcı tarafından istenmedi.)

## O. Bu tür bir uygulamada olması gereken temel eksikler (kullanıcıya listelendi, onay bekliyor)
- **Yazdır**: Android yazdırma ekranıyla doğrudan yazıcıya gönderme (sayfa aralığı seçerek)
- **Güncelleme bildirimi**: uygulama mağazadan değil GitHub'dan kurulduğu için açılışta yeni sürüm var mı kontrol edip "Yeni sürüm hazır" deme
- **Çökme kurtarma**: uygulama beklenmedik kapanırsa son açık defteri ve yarım kalan çizimi koruma; hata kaydını dosyaya yazıp paylaşılabilir yapma (sorun bildirmek için)
- **Depolama yönetimi**: hangi defter ne kadar yer kaplıyor; kullanılmayan görselleri ve eski yedekleri temizleme
- **İlk açılış tanıtımı**: 3-4 ekranlık kısa "nasıl kullanılır" (kalem/el ayrımı, araç menüsü, sayfa paneli) ve Ayarlar'dan tekrar açabilme
- **Defter kapağı rengi/görseli**: kapakta ilk sayfa yerine seçilen renk veya resim
- **Ayarları yedekle**: ayarlar, paletler ve kayıtlı kalemler de yedeğe dahil edilsin

## L. Kontrol
- Her değişiklikten sonra emülatör testi ve ekran görüntüsü incelemesi.
- Yeni özellikler için duman testleri.
