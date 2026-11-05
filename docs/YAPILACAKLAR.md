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
4. Yeni defter oluştururken **ad sorulmasın**: hemen "Adsız defter" açılsın, ad defterin içinde değiştirilsin.
5. **Toplu seçim**: uzun bas → seçim modu → çoklu seçip toplu sil, toplu klasöre taşı, toplu favori.
6. **Görünüm seçenekleri**: üstte küçük bir simge ile küçük ızgara, orta ızgara, büyük ızgara ve liste görünümü. Varsayılan kapaklar şimdikinden daha küçük olmalı.
7. **Klasör renkleri**: her klasöre kullanıcının seçtiği renk (simge renklendirilir).
8. Editörden ana sayfaya dönünce **"Kaydedildi" bildirimi** (kısa toast).

## C. Editör
9. **Üstteki renk/kalınlık çubuğunu kaldır.** Kalem seçenekleri, seçili araca tekrar dokununca açılan küçük bir açılır pencerede olsun. Ekranda yer kaplamasın.
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

## F. Önerilen ek özellikler (kullanıcıya listelendi, onay bekliyor)
- Sayfa içi **arama** (metin kutuları, formüller ve PDF metni içinde)
- **Ders planı / zaman çizelgesi**: haftalık ders programı ve hangi derste hangi defterin açılacağı
- **Şablon defterler**: sık kullanılan sayfa düzenlerini (ör. "konu anlatımı + 3 soru") şablon olarak kaydedip yeni deftere uygulama
- **Hazır soru alanı**: sayfaya "Soru 1, 2, 3" kutuları ekleme
- **Kesir / ondalık sayı doğrusu**, **olasılık ağacı**, **Venn şeması** hazır şekilleri
- **Sayfa numarası ve tarih**: sayfa köşesine otomatik
- **Kayıtlı ders videolarının listesi**: uygulama içinden açma, paylaşma
- **Öğrenci çıktısı**: soruları boş, çözümleri ayrı sayfada PDF olarak dışa aktarma (çözüm katmanını gizleme)
- **Katmanlar**: "çözüm" katmanını gizle/göster (soruyu boş gösterip sonra çözümü açma)
- **El yazısını yazıya çevirme** (cihaz üstü, internetsiz; büyük iş)
- **Kalem basıncı eğrisi** ayarı (hafif / sert yazanlar için)

## G. Kontrol
- Her değişiklikten sonra emülatör testi ve ekran görüntüsü incelemesi.
- Yeni özellikler için duman testleri.
