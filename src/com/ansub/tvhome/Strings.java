package com.ansub.tvhome;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// The home screen's own labels and input presets, in the TV's language. App names, original
// HDMI names and the clock come from the TV already. A language without a table, or a label
// without a translation, stays in English. To add a language, add a table in the same order as EN.
final class Strings {
    private static final String[] EN = {
        "Settings", "Add app", "Network", "Offline", "Sound", "Picture",
        "Move", "Replace", "Remove from home", "Uninstall", "App info",
        "Use the arrows to move it, then press OK", "Every app is already here", "Add an app", "Replace %s", "Wallpaper", "TV settings", "Brightness",
        "Rename", "Show hidden inputs", "TV", "Laptop", "PC", "Blu-ray", "Soundbar", "Type a name…",
        // The Settings screen. The tables below have them machine-translated: corrections welcome.
        "The colour behind your home screen. Moving through the list shows each one; OK keeps it.", "Choose wallpaper",
        "Everything else: accounts, apps, accessibility, date and time, language, updates and more.",
        "Open Android settings",
        "To change these here, run install.sh again from your computer: it gives this home screen the permission once. Until then this page only shows them.",
        "Off", "Screensaver", "Starts after", "Start the screensaver after",
        "Sleep moved to %s, so the screensaver has time to show.", "Never", "Preview screensaver", "Sleep",
        "Sleeps after", "Put it to sleep after",
        "Times count from the last button you pressed. The screensaver shows first, then it sleeps.", "Connection",
        "Connected with", "Signal", "Band", "Link speed", "IP address", "Wi-Fi & network settings", "Bluetooth",
        "Not available", "On", "Paired devices", "None", "Connected", "Not connected",
        "Pair a remote, controller or headphones", "Resolution", "Refresh rate", "HDR", "TV speakers", "Audio goes to",
        "Surround formats", "Picture & brightness", "Resolution, HDR & sound",
        "On NVIDIA Shield these are under Device Preferences, then Display & Sound.", "This device", "Model", "Software",
        "On for", "Memory & storage", "Memory", "%s free of %s", "Storage", "Home screen version",
        "Run install.sh again from your computer to change this here", "Can't show a preview here", "1 hour",
        "%d hours", "1 minute", "%d minutes", "Screensaver & sleep", "Network & Bluetooth", "Display & sound",
        "Device info & storage", "All Android settings", "Ethernet", "Wi-Fi", "Other", "Very weak", "Weak", "Fair",
        "Good", "Excellent",
    };

    // Keyed by language ("de"), or language-country ("zh-TW") where a country writes it differently.
    private static final Map<String, String[]> LANGS = new HashMap<>();
    static {
        LANGS.put("ar", new String[] {
            "الإعدادات", "إضافة تطبيق", "الشبكة", "غير متصل", "الصوت", "الصورة",
            "نقل", "استبدال", "إزالة من الشاشة الرئيسية", "إلغاء التثبيت", "معلومات التطبيق",
            "استخدم الأسهم لنقله، ثم اضغط OK", "كل التطبيقات موجودة هنا", "إضافة تطبيق", "استبدال %s", "الخلفية", "إعدادات التلفزيون", "السطوع",
            "إعادة التسمية", "إظهار المدخلات المخفية", "تلفزيون", "حاسوب محمول", "حاسوب", "بلو راي", "مكبر صوت شريطي", "اكتب اسمًا…",
            // The Settings screen, machine-translated: corrections welcome.
            "اللون خلف الشاشة الرئيسية. يظهر كل لون عند التنقل في القائمة، ويحفظه زر OK.", "اختيار الخلفية",
            "كل ما عدا ذلك: الحسابات والتطبيقات وتسهيل الاستخدام والتاريخ والوقت واللغة والتحديثات والمزيد.", "فتح إعدادات Android",
            "لتغييرها من هنا، شغّل install.sh مرة أخرى من الكمبيوتر: يمنح هذه الشاشة الرئيسية الإذن مرة واحدة. حتى ذلك الحين تعرض هذه الصفحة القيم فقط.",
            "إيقاف", "شاشة التوقف", "تبدأ بعد", "بدء شاشة التوقف بعد", "تم نقل السكون إلى %s ليتسنى لشاشة التوقف الظهور.", "أبدًا",
            "معاينة شاشة التوقف", "السكون", "السكون بعد", "الانتقال إلى السكون بعد", "تُحسب المدد من آخر زر ضغطته. تظهر شاشة التوقف أولًا ثم السكون.",
            "الاتصال", "متصل عبر", "الإشارة", "النطاق", "سرعة الاتصال", "عنوان IP", "إعدادات Wi-Fi والشبكة", "بلوتوث", "غير متاح", "تشغيل",
            "الأجهزة المقترنة", "لا يوجد", "متصل", "غير متصل", "إقران جهاز تحكم عن بُعد أو يد تحكم أو سماعات", "الدقة", "معدل التحديث", "HDR",
            "مكبرات صوت التلفزيون", "يخرج الصوت إلى", "صيغ الصوت المحيطي", "الصورة والسطوع", "الدقة وHDR والصوت",
            "على NVIDIA Shield توجد ضمن إعدادات الجهاز المفضّلة، ثم العرض والصوت.", "هذا الجهاز", "الطراز", "البرنامج", "قيد التشغيل منذ",
            "الذاكرة والتخزين", "الذاكرة", "%s متاح من %s", "مساحة التخزين", "إصدار الشاشة الرئيسية",
            "شغّل install.sh مرة أخرى من الكمبيوتر لتغيير هذا من هنا", "لا يمكن عرض معاينة هنا", "ساعة واحدة", "%d ساعة", "دقيقة واحدة", "%d دقيقة",
            "شاشة التوقف والسكون", "الشبكة والبلوتوث", "العرض والصوت", "معلومات الجهاز والتخزين", "كل إعدادات Android", "إيثرنت", "Wi-Fi", "أخرى",
            "ضعيفة جدًا", "ضعيفة", "مقبولة", "جيدة", "ممتازة",
        });
        LANGS.put("de", new String[] {
            "Einstellungen", "App hinzufügen", "Netzwerk", "Offline", "Ton", "Bild",
            "Verschieben", "Ersetzen", "Vom Startbildschirm entfernen", "Deinstallieren", "App-Info",
            "Mit den Pfeiltasten verschieben, dann OK drücken", "Alle Apps sind schon da", "App hinzufügen", "%s ersetzen", "Hintergrund", "TV-Einstellungen", "Helligkeit",
            "Umbenennen", "Ausgeblendete Eingänge anzeigen", "TV", "Laptop", "PC", "Blu-ray", "Soundbar", "Namen eingeben…",
            // The Settings screen, machine-translated: corrections welcome.
            "Die Farbe hinter dem Startbildschirm. Beim Blättern wird jede gezeigt; OK übernimmt sie.", "Hintergrund wählen",
            "Alles andere: Konten, Apps, Bedienungshilfen, Datum und Uhrzeit, Sprache, Updates und mehr.", "Android-Einstellungen öffnen",
            "Um das hier zu ändern, install.sh noch einmal vom Computer ausführen: Es gibt diesem Startbildschirm einmalig die Berechtigung. Bis dahin werden die Werte nur angezeigt.",
            "Aus", "Bildschirmschoner", "Startet nach", "Bildschirmschoner starten nach",
            "Ruhezustand auf %s verschoben, damit der Bildschirmschoner Zeit hat.", "Nie", "Bildschirmschoner ansehen", "Ruhezustand",
            "Ruhezustand nach", "In den Ruhezustand nach",
            "Die Zeiten zählen ab dem letzten Tastendruck. Zuerst kommt der Bildschirmschoner, dann der Ruhezustand.", "Verbindung", "Verbunden über",
            "Signal", "Frequenzband", "Verbindungsgeschwindigkeit", "IP-Adresse", "WLAN- und Netzwerkeinstellungen", "Bluetooth", "Nicht verfügbar",
            "An", "Gekoppelte Geräte", "Keine", "Verbunden", "Nicht verbunden", "Fernbedienung, Controller oder Kopfhörer koppeln", "Auflösung",
            "Bildwiederholrate", "HDR", "TV-Lautsprecher", "Ton geht an", "Surround-Formate", "Bild und Helligkeit", "Auflösung, HDR und Ton",
            "Auf der NVIDIA Shield unter Geräteeinstellungen, dann Display & Ton.", "Dieses Gerät", "Modell", "Software", "An seit",
            "Arbeitsspeicher und Speicher", "Arbeitsspeicher", "%s frei von %s", "Speicher", "Version des Startbildschirms",
            "Zum Ändern install.sh noch einmal vom Computer ausführen", "Vorschau hier nicht möglich", "1 Stunde", "%d Stunden", "1 Minute",
            "%d Minuten", "Bildschirmschoner & Ruhezustand", "Netzwerk & Bluetooth", "Bild & Ton", "Geräteinfo & Speicher",
            "Alle Android-Einstellungen", "Ethernet", "WLAN", "Andere", "Sehr schwach", "Schwach", "Mittel", "Gut", "Sehr gut",
        });
        LANGS.put("es", new String[] {
            "Ajustes", "Añadir app", "Red", "Sin conexión", "Sonido", "Imagen",
            "Mover", "Reemplazar", "Quitar del inicio", "Desinstalar", "Info. de la app",
            "Usa las flechas para moverla y pulsa OK", "Todas las apps ya están aquí", "Añadir una app", "Reemplazar %s", "Fondo de pantalla", "Ajustes de la TV", "Brillo",
            "Renombrar", "Mostrar entradas ocultas", "TV", "Portátil", "PC", "Blu-ray", "Barra de sonido", "Escribe un nombre…",
            // The Settings screen, machine-translated: corrections welcome.
            "El color detrás de la pantalla de inicio. Al recorrer la lista se ve cada uno; OK lo guarda.", "Elegir fondo",
            "Todo lo demás: cuentas, apps, accesibilidad, fecha y hora, idioma, actualizaciones y más.", "Abrir ajustes de Android",
            "Para cambiarlos aquí, vuelve a ejecutar install.sh desde tu ordenador: da a esta pantalla de inicio el permiso una vez. Hasta entonces esta página solo los muestra.",
            "Desactivado", "Salvapantallas", "Empieza tras", "Iniciar el salvapantallas tras",
            "Reposo movido a %s para que el salvapantallas tenga tiempo de verse.", "Nunca", "Ver salvapantallas", "Reposo", "Reposo tras",
            "Poner en reposo tras", "Los tiempos cuentan desde la última tecla pulsada. Primero sale el salvapantallas y luego el reposo.", "Conexión",
            "Conectado por", "Señal", "Banda", "Velocidad del enlace", "Dirección IP", "Ajustes de Wi-Fi y red", "Bluetooth", "No disponible",
            "Activado", "Dispositivos vinculados", "Ninguno", "Conectado", "No conectado", "Vincular un mando, control o auriculares", "Resolución",
            "Frecuencia de actualización", "HDR", "Altavoces de la TV", "El sonido sale por", "Formatos envolventes", "Imagen y brillo",
            "Resolución, HDR y sonido", "En NVIDIA Shield están en Preferencias del dispositivo, luego Pantalla y sonido.", "Este dispositivo",
            "Modelo", "Software", "Encendido desde hace", "Memoria y almacenamiento", "Memoria", "%s libres de %s", "Almacenamiento",
            "Versión de la pantalla de inicio", "Vuelve a ejecutar install.sh desde tu ordenador para cambiarlo aquí",
            "No se puede mostrar una vista previa", "1 hora", "%d horas", "1 minuto", "%d minutos", "Salvapantallas y reposo", "Red y Bluetooth",
            "Pantalla y sonido", "Información y almacenamiento", "Todos los ajustes de Android", "Ethernet", "Wi-Fi", "Otra", "Muy débil", "Débil",
            "Regular", "Buena", "Excelente",
        });
        LANGS.put("fr", new String[] {
            "Paramètres", "Ajouter une app", "Réseau", "Hors ligne", "Son", "Image",
            "Déplacer", "Remplacer", "Retirer de l'accueil", "Désinstaller", "Infos sur l'app",
            "Utilisez les flèches pour la déplacer, puis appuyez sur OK", "Toutes les apps sont déjà là", "Ajouter une app", "Remplacer %s", "Fond d'écran", "Paramètres du téléviseur", "Luminosité",
            "Renommer", "Afficher les entrées masquées", "TV", "Ordinateur portable", "PC", "Blu-ray", "Barre de son", "Saisir un nom…",
            // The Settings screen, machine-translated: corrections welcome.
            "La couleur derrière l'écran d'accueil. Chaque couleur s'affiche en parcourant la liste ; OK la garde.", "Choisir un fond d'écran",
            "Tout le reste : comptes, apps, accessibilité, date et heure, langue, mises à jour et plus.", "Ouvrir les paramètres Android",
            "Pour les modifier ici, relancez install.sh depuis votre ordinateur : il donne une fois l'autorisation à cet écran d'accueil. D'ici là, cette page les affiche seulement.",
            "Désactivé", "Économiseur d'écran", "Démarre après", "Lancer l'économiseur d'écran après",
            "Veille déplacée à %s pour que l'économiseur d'écran ait le temps de s'afficher.", "Jamais", "Aperçu de l'économiseur", "Veille",
            "Veille après", "Mettre en veille après",
            "Les durées comptent depuis la dernière touche. L'économiseur d'écran s'affiche d'abord, puis la veille.", "Connexion", "Connecté via",
            "Signal", "Bande", "Débit de la liaison", "Adresse IP", "Paramètres Wi-Fi et réseau", "Bluetooth", "Non disponible", "Activé",
            "Appareils associés", "Aucun", "Connecté", "Non connecté", "Associer une télécommande, une manette ou un casque", "Résolution",
            "Fréquence d'affichage", "HDR", "Haut-parleurs du téléviseur", "Le son sort par", "Formats surround", "Image et luminosité",
            "Résolution, HDR et son", "Sur NVIDIA Shield, ils sont dans Préférences de l'appareil, puis Affichage et son.", "Cet appareil", "Modèle",
            "Logiciel", "Allumé depuis", "Mémoire et stockage", "Mémoire", "%s libres sur %s", "Stockage", "Version de l'écran d'accueil",
            "Relancez install.sh depuis votre ordinateur pour modifier ceci ici", "Aperçu impossible ici", "1 heure", "%d heures", "1 minute",
            "%d minutes", "Économiseur et veille", "Réseau et Bluetooth", "Affichage et son", "Infos et stockage", "Tous les paramètres Android",
            "Ethernet", "Wi-Fi", "Autre", "Très faible", "Faible", "Moyen", "Bon", "Excellent",
        });
        LANGS.put("hi", new String[] {
            "सेटिंग्स", "ऐप जोड़ें", "नेटवर्क", "ऑफ़लाइन", "ध्वनि", "चित्र",
            "ले जाएँ", "बदलें", "होम से हटाएँ", "अनइंस्टॉल करें", "ऐप की जानकारी",
            "तीर से खिसकाएँ, फिर OK दबाएँ", "सभी ऐप पहले से यहाँ हैं", "ऐप जोड़ें", "%s बदलें", "वॉलपेपर", "टीवी सेटिंग्स", "चमक",
            "नाम बदलें", "छिपे हुए इनपुट दिखाएँ", "टीवी", "लैपटॉप", "पीसी", "ब्लू-रे", "साउंडबार", "नाम लिखें…",
            // The Settings screen, machine-translated: corrections welcome.
            "होम स्क्रीन के पीछे का रंग। सूची में आगे बढ़ने पर हर रंग दिखता है; OK उसे रख लेता है।", "वॉलपेपर चुनें",
            "बाकी सब: खाते, ऐप, सुलभता, तारीख और समय, भाषा, अपडेट और बहुत कुछ।", "Android सेटिंग्स खोलें",
            "इन्हें यहाँ बदलने के लिए अपने कंप्यूटर से install.sh फिर से चलाएँ: यह इस होम स्क्रीन को एक बार अनुमति देता है। तब तक यह पेज सिर्फ़ इन्हें दिखाता है।",
            "बंद", "स्क्रीनसेवर", "इतनी देर बाद शुरू", "स्क्रीनसेवर इतनी देर बाद शुरू करें",
            "स्लीप को %s पर किया गया, ताकि स्क्रीनसेवर को दिखने का समय मिले।", "कभी नहीं", "स्क्रीनसेवर देखें", "स्लीप", "इतनी देर बाद स्लीप",
            "इतनी देर बाद स्लीप में भेजें", "समय आखिरी बार दबाए गए बटन से गिना जाता है। पहले स्क्रीनसेवर आता है, फिर स्लीप।", "कनेक्शन",
            "इससे कनेक्ट है", "सिग्नल", "बैंड", "लिंक स्पीड", "IP पता", "वाई-फ़ाई और नेटवर्क सेटिंग्स", "ब्लूटूथ", "उपलब्ध नहीं", "चालू",
            "पेयर किए गए डिवाइस", "कोई नहीं", "कनेक्ट है", "कनेक्ट नहीं है", "रिमोट, कंट्रोलर या हेडफ़ोन पेयर करें", "रिज़ॉल्यूशन", "रिफ़्रेश रेट",
            "HDR", "टीवी स्पीकर", "ध्वनि यहाँ जाती है", "सराउंड फ़ॉर्मैट", "चित्र और चमक", "रिज़ॉल्यूशन, HDR और ध्वनि",
            "NVIDIA Shield पर ये डिवाइस प्राथमिकताएँ, फिर डिस्प्ले और साउंड में हैं।", "यह डिवाइस", "मॉडल", "सॉफ़्टवेयर", "चालू है",
            "मेमोरी और स्टोरेज", "मेमोरी", "%s खाली / कुल %s", "स्टोरेज", "होम स्क्रीन वर्शन",
            "इसे यहाँ बदलने के लिए अपने कंप्यूटर से install.sh फिर से चलाएँ", "यहाँ पूर्वावलोकन नहीं दिख सकता", "1 घंटा", "%d घंटे", "1 मिनट",
            "%d मिनट", "स्क्रीनसेवर और स्लीप", "नेटवर्क और ब्लूटूथ", "डिस्प्ले और ध्वनि", "डिवाइस जानकारी और स्टोरेज", "सभी Android सेटिंग्स",
            "ईथरनेट", "वाई-फ़ाई", "अन्य", "बहुत कमज़ोर", "कमज़ोर", "ठीक-ठाक", "अच्छा", "बहुत अच्छा",
        });
        String[] id = {
            "Setelan", "Tambah aplikasi", "Jaringan", "Offline", "Suara", "Gambar",
            "Pindahkan", "Ganti", "Hapus dari beranda", "Uninstal", "Info aplikasi",
            "Gunakan tombol panah untuk memindahkan, lalu tekan OK", "Semua aplikasi sudah ada di sini", "Tambah aplikasi", "Ganti %s", "Wallpaper", "Setelan TV", "Kecerahan",
            "Ubah nama", "Tampilkan input tersembunyi", "TV", "Laptop", "PC", "Blu-ray", "Soundbar", "Ketik nama…",
            // The Settings screen, machine-translated: corrections welcome.
            "Warna di belakang beranda. Menelusuri daftar menampilkan tiap warna; OK menyimpannya.", "Pilih wallpaper",
            "Semua lainnya: akun, aplikasi, aksesibilitas, tanggal dan waktu, bahasa, update, dan lainnya.", "Buka setelan Android",
            "Untuk mengubahnya di sini, jalankan lagi install.sh dari komputer: install.sh memberi izin sekali ke beranda ini. Sampai saat itu halaman ini hanya menampilkannya.",
            "Nonaktif", "Screensaver", "Mulai setelah", "Mulai screensaver setelah", "Mode tidur dipindah ke %s agar screensaver sempat tampil.",
            "Tidak pernah", "Pratinjau screensaver", "Mode tidur", "Tidur setelah", "Tidurkan setelah",
            "Waktu dihitung dari tombol terakhir yang ditekan. Screensaver tampil lebih dulu, lalu mode tidur.", "Koneksi", "Terhubung lewat",
            "Sinyal", "Pita", "Kecepatan link", "Alamat IP", "Setelan Wi-Fi & jaringan", "Bluetooth", "Tidak tersedia", "Aktif",
            "Perangkat tersambung", "Tidak ada", "Terhubung", "Tidak terhubung", "Sambungkan remote, controller, atau headphone", "Resolusi",
            "Kecepatan refresh", "HDR", "Speaker TV", "Suara keluar ke", "Format surround", "Gambar & kecerahan", "Resolusi, HDR & suara",
            "Di NVIDIA Shield, ada di Preferensi Perangkat, lalu Layar & Suara.", "Perangkat ini", "Model", "Software", "Menyala selama",
            "Memori & penyimpanan", "Memori", "%s kosong dari %s", "Penyimpanan", "Versi beranda",
            "Jalankan lagi install.sh dari komputer untuk mengubah ini di sini", "Pratinjau tidak bisa ditampilkan di sini", "1 jam", "%d jam",
            "1 menit", "%d menit", "Screensaver & tidur", "Jaringan & Bluetooth", "Layar & suara", "Info perangkat & penyimpanan",
            "Semua setelan Android", "Ethernet", "Wi-Fi", "Lainnya", "Sangat lemah", "Lemah", "Cukup", "Bagus", "Sangat bagus",
        };
        LANGS.put("id", id);
        LANGS.put("in", id); // Java's old code for Indonesian, which Android still reports

        LANGS.put("it", new String[] {
            "Impostazioni", "Aggiungi app", "Rete", "Offline", "Audio", "Immagine",
            "Sposta", "Sostituisci", "Rimuovi dalla home", "Disinstalla", "Info app",
            "Usa le frecce per spostarla, poi premi OK", "Tutte le app sono già qui", "Aggiungi un'app", "Sostituisci %s", "Sfondo", "Impostazioni TV", "Luminosità",
            "Rinomina", "Mostra ingressi nascosti", "TV", "Portatile", "PC", "Blu-ray", "Soundbar", "Digita un nome…",
            // The Settings screen, machine-translated: corrections welcome.
            "Il colore dietro la schermata Home. Scorrendo l'elenco si vede ognuno; OK lo conferma.", "Scegli sfondo",
            "Tutto il resto: account, app, accessibilità, data e ora, lingua, aggiornamenti e altro.", "Apri impostazioni Android",
            "Per cambiarli qui, esegui di nuovo install.sh dal computer: dà una volta il permesso a questa schermata Home. Fino ad allora questa pagina li mostra soltanto.",
            "Off", "Salvaschermo", "Si avvia dopo", "Avvia il salvaschermo dopo",
            "Sospensione spostata a %s, così il salvaschermo ha tempo di comparire.", "Mai", "Anteprima salvaschermo", "Sospensione",
            "Va in sospensione dopo", "Metti in sospensione dopo",
            "I tempi contano dall'ultimo tasto premuto. Prima compare il salvaschermo, poi la sospensione.", "Connessione", "Connesso tramite",
            "Segnale", "Banda", "Velocità di collegamento", "Indirizzo IP", "Impostazioni Wi-Fi e rete", "Bluetooth", "Non disponibile", "On",
            "Dispositivi associati", "Nessuno", "Connesso", "Non connesso", "Associa telecomando, controller o cuffie", "Risoluzione",
            "Frequenza di aggiornamento", "HDR", "Altoparlanti della TV", "L'audio va a", "Formati surround", "Immagine e luminosità",
            "Risoluzione, HDR e audio", "Su NVIDIA Shield si trovano in Preferenze dispositivo, poi Display e audio.", "Questo dispositivo", "Modello",
            "Software", "Acceso da", "Memoria e archiviazione", "Memoria", "%s liberi su %s", "Archiviazione", "Versione della schermata Home",
            "Esegui di nuovo install.sh dal computer per cambiarlo qui", "Anteprima non disponibile qui", "1 ora", "%d ore", "1 minuto", "%d minuti",
            "Salvaschermo e sospensione", "Rete e Bluetooth", "Display e audio", "Info e archiviazione", "Tutte le impostazioni Android", "Ethernet",
            "Wi-Fi", "Altro", "Molto debole", "Debole", "Discreto", "Buono", "Ottimo",
        });
        LANGS.put("ja", new String[] {
            "設定", "アプリを追加", "ネットワーク", "オフライン", "サウンド", "画像",
            "移動", "置き換え", "ホームから削除", "アンインストール", "アプリ情報",
            "矢印キーで移動して、OK を押してください", "すべてのアプリが追加済みです", "アプリを追加", "%s を置き換え", "壁紙", "テレビの設定", "明るさ",
            "名前を変更", "非表示の入力を表示", "テレビ", "ノートパソコン", "パソコン", "ブルーレイ", "サウンドバー", "名前を入力…",
            // The Settings screen, machine-translated: corrections welcome.
            "ホーム画面の背景色です。リストを動かすと各色が表示され、OK で決定します。", "壁紙を選ぶ", "その他すべて：アカウント、アプリ、ユーザー補助、日付と時刻、言語、アップデートなど。", "Android の設定を開く",
            "ここで変更するには、パソコンから install.sh をもう一度実行してください。このホーム画面に一度だけ権限を与えます。それまではこのページは表示のみです。", "オフ", "スクリーンセーバー", "開始までの時間", "スクリーンセーバーを開始するまでの時間",
            "スクリーンセーバーが表示されるよう、スリープを %s に変更しました。", "なし", "スクリーンセーバーをプレビュー", "スリープ", "スリープまでの時間", "スリープするまでの時間",
            "時間は最後にボタンを押したときから数えます。先にスクリーンセーバー、その後スリープになります。", "接続", "接続方法", "信号", "周波数帯", "リンク速度", "IP アドレス", "Wi-Fi とネットワークの設定", "Bluetooth",
            "利用できません", "オン", "ペア設定済みのデバイス", "なし", "接続済み", "未接続", "リモコン、コントローラー、ヘッドホンをペア設定", "解像度", "リフレッシュレート", "HDR", "テレビのスピーカー", "音声の出力先",
            "サラウンド形式", "画質と明るさ", "解像度、HDR、音声", "NVIDIA Shield では「デバイス設定」→「ディスプレイとサウンド」にあります。", "このデバイス", "モデル", "ソフトウェア", "起動時間", "メモリとストレージ", "メモリ",
            "%s 空き（全 %s）", "ストレージ", "ホーム画面のバージョン", "ここで変更するには、パソコンから install.sh をもう一度実行してください", "ここではプレビューを表示できません", "1 時間", "%d 時間", "1 分", "%d 分",
            "スクリーンセーバーとスリープ", "ネットワークと Bluetooth", "ディスプレイとサウンド", "デバイス情報とストレージ", "すべての Android 設定", "イーサネット", "Wi-Fi", "その他", "非常に弱い", "弱い", "普通",
            "良好", "非常に良好",
        });
        LANGS.put("ko", new String[] {
            "설정", "앱 추가", "네트워크", "오프라인", "소리", "화면",
            "이동", "바꾸기", "홈에서 삭제", "제거", "앱 정보",
            "화살표로 옮긴 다음 OK를 누르세요", "모든 앱이 이미 있습니다", "앱 추가", "%s 바꾸기", "배경화면", "TV 설정", "밝기",
            "이름 변경", "숨긴 입력 표시", "TV", "노트북", "PC", "블루레이", "사운드바", "이름 입력…",
            // The Settings screen, machine-translated: corrections welcome.
            "홈 화면 뒤의 색입니다. 목록을 움직이면 색이 바뀌고, OK를 누르면 저장됩니다.", "배경화면 선택", "그 밖의 모든 것: 계정, 앱, 접근성, 날짜 및 시간, 언어, 업데이트 등.", "Android 설정 열기",
            "여기서 바꾸려면 컴퓨터에서 install.sh를 다시 실행하세요. 이 홈 화면에 권한을 한 번 줍니다. 그전까지 이 페이지는 값만 보여 줍니다.", "끄기", "화면 보호기", "시작 시간", "화면 보호기 시작 시간",
            "화면 보호기가 나올 수 있도록 절전 모드를 %s(으)로 옮겼습니다.", "사용 안함", "화면 보호기 미리 보기", "절전 모드", "절전 모드 시간", "절전 모드 전환 시간",
            "시간은 마지막으로 버튼을 누른 때부터 셉니다. 화면 보호기가 먼저 나오고 그다음 절전 모드가 됩니다.", "연결", "연결 방식", "신호", "대역", "링크 속도", "IP 주소", "Wi-Fi 및 네트워크 설정", "블루투스",
            "사용할 수 없음", "켜짐", "페어링된 기기", "없음", "연결됨", "연결 안 됨", "리모컨, 컨트롤러, 헤드폰 페어링", "해상도", "재생 빈도", "HDR", "TV 스피커", "소리 출력", "서라운드 형식", "화면 및 밝기",
            "해상도, HDR 및 소리", "NVIDIA Shield에서는 기기 환경설정 > 디스플레이 및 사운드에 있습니다.", "이 기기", "모델", "소프트웨어", "켜진 시간", "메모리 및 저장공간", "메모리", "%s 남음 / 전체 %s",
            "저장공간", "홈 화면 버전", "여기서 바꾸려면 컴퓨터에서 install.sh를 다시 실행하세요", "여기서는 미리 보기를 표시할 수 없습니다", "1시간", "%d시간", "1분", "%d분", "화면 보호기 및 절전",
            "네트워크 및 블루투스", "디스플레이 및 소리", "기기 정보 및 저장공간", "모든 Android 설정", "이더넷", "Wi-Fi", "기타", "매우 약함", "약함", "보통", "좋음", "매우 좋음",
        });
        LANGS.put("nl", new String[] {
            "Instellingen", "App toevoegen", "Netwerk", "Offline", "Geluid", "Beeld",
            "Verplaatsen", "Vervangen", "Van startscherm halen", "Verwijderen", "App-info",
            "Verplaats met de pijltjes en druk dan op OK", "Alle apps staan er al", "Een app toevoegen", "%s vervangen", "Achtergrond", "Tv-instellingen", "Helderheid",
            "Naam wijzigen", "Verborgen ingangen tonen", "TV", "Laptop", "PC", "Blu-ray", "Soundbar", "Typ een naam…",
            // The Settings screen, machine-translated: corrections welcome.
            "De kleur achter je startscherm. Door de lijst bladeren laat elke kleur zien; OK bewaart hem.", "Achtergrond kiezen",
            "Al het andere: accounts, apps, toegankelijkheid, datum en tijd, taal, updates en meer.", "Android-instellingen openen",
            "Om dit hier te wijzigen, voer je install.sh opnieuw uit vanaf je computer: dat geeft dit startscherm eenmalig toestemming. Tot die tijd toont deze pagina alleen de waarden.",
            "Uit", "Screensaver", "Start na", "Screensaver starten na",
            "Slaapstand verschoven naar %s, zodat de screensaver tijd heeft om te verschijnen.", "Nooit", "Screensaver bekijken", "Slaapstand",
            "Slaapstand na", "In slaapstand zetten na",
            "De tijden tellen vanaf de laatste knop die je indrukte. Eerst komt de screensaver, daarna de slaapstand.", "Verbinding", "Verbonden via",
            "Signaal", "Band", "Verbindingssnelheid", "IP-adres", "Wifi- en netwerkinstellingen", "Bluetooth", "Niet beschikbaar", "Aan",
            "Gekoppelde apparaten", "Geen", "Verbonden", "Niet verbonden", "Afstandsbediening, controller of koptelefoon koppelen", "Resolutie",
            "Verversingssnelheid", "HDR", "Tv-speakers", "Geluid gaat naar", "Surroundformaten", "Beeld en helderheid", "Resolutie, HDR en geluid",
            "Op de NVIDIA Shield staan deze onder Apparaatvoorkeuren, dan Scherm en geluid.", "Dit apparaat", "Model", "Software", "Looptijd",
            "Geheugen en opslag", "Geheugen", "%s vrij van %s", "Opslag", "Versie van het startscherm",
            "Voer install.sh opnieuw uit vanaf je computer om dit hier te wijzigen", "Voorbeeld kan hier niet worden getoond", "1 uur", "%d uur",
            "1 minuut", "%d minuten", "Screensaver en slaapstand", "Netwerk en bluetooth", "Beeld en geluid", "Apparaatinfo en opslag",
            "Alle Android-instellingen", "Ethernet", "Wifi", "Anders", "Zeer zwak", "Zwak", "Redelijk", "Goed", "Uitstekend",
        });
        LANGS.put("pl", new String[] {
            "Ustawienia", "Dodaj aplikację", "Sieć", "Offline", "Dźwięk", "Obraz",
            "Przenieś", "Zamień", "Usuń z ekranu głównego", "Odinstaluj", "Informacje o aplikacji",
            "Przesuń strzałkami, potem naciśnij OK", "Wszystkie aplikacje już tu są", "Dodaj aplikację", "Zamień %s", "Tapeta", "Ustawienia telewizora", "Jasność",
            "Zmień nazwę", "Pokaż ukryte wejścia", "Telewizor", "Laptop", "Komputer", "Blu-ray", "Soundbar", "Wpisz nazwę…",
            // The Settings screen, machine-translated: corrections welcome.
            "Kolor za ekranem głównym. Przewijanie listy pokazuje każdy; OK go zatwierdza.", "Wybierz tapetę",
            "Wszystko inne: konta, aplikacje, ułatwienia dostępu, data i godzina, język, aktualizacje i więcej.", "Otwórz ustawienia Androida",
            "Aby zmienić je tutaj, uruchom ponownie install.sh z komputera: jednorazowo da temu ekranowi głównemu uprawnienie. Do tego czasu ta strona tylko je pokazuje.",
            "Wył.", "Wygaszacz ekranu", "Włącza się po", "Włącz wygaszacz po", "Uśpienie przesunięte na %s, żeby wygaszacz zdążył się pokazać.",
            "Nigdy", "Podgląd wygaszacza", "Uśpienie", "Usypia się po", "Uśpij po",
            "Czas liczy się od ostatniego naciśnięcia przycisku. Najpierw pojawia się wygaszacz, potem uśpienie.", "Połączenie", "Połączono przez",
            "Sygnał", "Pasmo", "Szybkość łącza", "Adres IP", "Ustawienia Wi-Fi i sieci", "Bluetooth", "Niedostępne", "Wł.", "Sparowane urządzenia",
            "Brak", "Połączono", "Nie połączono", "Sparuj pilota, kontroler lub słuchawki", "Rozdzielczość", "Częstotliwość odświeżania", "HDR",
            "Głośniki telewizora", "Dźwięk idzie do", "Formaty przestrzenne", "Obraz i jasność", "Rozdzielczość, HDR i dźwięk",
            "Na NVIDIA Shield są w Preferencjach urządzenia, potem Wyświetlacz i dźwięk.", "To urządzenie", "Model", "Oprogramowanie", "Włączone od",
            "Pamięć i miejsce", "Pamięć", "%s wolne z %s", "Miejsce na dane", "Wersja ekranu głównego",
            "Uruchom ponownie install.sh z komputera, aby zmienić to tutaj", "Nie można tu pokazać podglądu", "1 godz.", "%d godz.", "1 min", "%d min",
            "Wygaszacz i uśpienie", "Sieć i Bluetooth", "Obraz i dźwięk", "Informacje i pamięć", "Wszystkie ustawienia Androida", "Ethernet", "Wi-Fi",
            "Inne", "Bardzo słaby", "Słaby", "Średni", "Dobry", "Doskonały",
        });
        LANGS.put("pt", new String[] { // Brazilian Portuguese
            "Configurações", "Adicionar app", "Rede", "Sem conexão", "Som", "Imagem",
            "Mover", "Substituir", "Remover do início", "Desinstalar", "Informações do app",
            "Use as setas para mover e depois aperte OK", "Todos os apps já estão aqui", "Adicionar um app", "Substituir %s", "Plano de fundo", "Configurações da TV", "Brilho",
            "Renomear", "Mostrar entradas ocultas", "TV", "Notebook", "PC", "Blu-ray", "Barra de som", "Digite um nome…",
            // The Settings screen, machine-translated: corrections welcome.
            "A cor atrás da tela inicial. Ao percorrer a lista, cada uma aparece; OK confirma.", "Escolher plano de fundo",
            "Todo o resto: contas, apps, acessibilidade, data e hora, idioma, atualizações e mais.", "Abrir configurações do Android",
            "Para mudar aqui, rode o install.sh de novo no seu computador: ele dá a permissão a esta tela inicial uma vez. Até lá, esta página só mostra os valores.",
            "Desativado", "Protetor de tela", "Começa depois de", "Iniciar o protetor de tela depois de",
            "Suspensão passou para %s, para o protetor de tela ter tempo de aparecer.", "Nunca", "Ver protetor de tela", "Suspensão",
            "Suspende depois de", "Suspender depois de",
            "Os tempos contam a partir do último botão apertado. Primeiro vem o protetor de tela, depois a suspensão.", "Conexão", "Conectado por",
            "Sinal", "Banda", "Velocidade do link", "Endereço IP", "Configurações de Wi-Fi e rede", "Bluetooth", "Indisponível", "Ativado",
            "Dispositivos pareados", "Nenhum", "Conectado", "Não conectado", "Parear controle remoto, controle de jogo ou fones", "Resolução",
            "Taxa de atualização", "HDR", "Alto-falantes da TV", "O som vai para", "Formatos surround", "Imagem e brilho", "Resolução, HDR e som",
            "No NVIDIA Shield, ficam em Preferências do dispositivo, depois Tela e som.", "Este dispositivo", "Modelo", "Software", "Ligado há",
            "Memória e armazenamento", "Memória", "%s livres de %s", "Armazenamento", "Versão da tela inicial",
            "Rode o install.sh de novo no seu computador para mudar isto aqui", "Não é possível mostrar a prévia aqui", "1 hora", "%d horas",
            "1 minuto", "%d minutos", "Protetor de tela e suspensão", "Rede e Bluetooth", "Tela e som", "Informações e armazenamento",
            "Todas as configurações do Android", "Ethernet", "Wi-Fi", "Outra", "Muito fraco", "Fraco", "Razoável", "Bom", "Excelente",
        });
        LANGS.put("ru", new String[] {
            "Настройки", "Добавить", "Сеть", "Нет сети", "Звук", "Изображение",
            "Переместить", "Заменить", "Убрать с главного экрана", "Удалить", "О приложении",
            "Перемещайте стрелками, затем нажмите OK", "Все приложения уже здесь", "Добавить приложение", "Заменить %s", "Обои", "Настройки телевизора", "Яркость",
            "Переименовать", "Показать скрытые входы", "Телевизор", "Ноутбук", "ПК", "Blu-ray", "Саундбар", "Введите название…",
            // The Settings screen, machine-translated: corrections welcome.
            "Цвет за главным экраном. При прокрутке списка показывается каждый; OK сохраняет.", "Выбрать обои",
            "Всё остальное: аккаунты, приложения, спец. возможности, дата и время, язык, обновления и другое.", "Открыть настройки Android",
            "Чтобы менять это здесь, снова запустите install.sh с компьютера: он один раз даст главному экрану разрешение. До тех пор эта страница только показывает значения.",
            "Выкл.", "Заставка", "Включается через", "Включать заставку через", "Сон перенесён на %s, чтобы заставка успела показаться.", "Никогда",
            "Показать заставку", "Сон", "Засыпает через", "Переводить в сон через",
            "Время отсчитывается от последнего нажатия кнопки. Сначала появляется заставка, потом сон.", "Подключение", "Подключено через", "Сигнал",
            "Диапазон", "Скорость соединения", "IP-адрес", "Настройки Wi-Fi и сети", "Bluetooth", "Недоступно", "Вкл.", "Сопряжённые устройства",
            "Нет", "Подключено", "Не подключено", "Подключить пульт, геймпад или наушники", "Разрешение", "Частота обновления", "HDR",
            "Динамики телевизора", "Звук выводится на", "Форматы объёмного звука", "Изображение и яркость", "Разрешение, HDR и звук",
            "На NVIDIA Shield это в разделе «Настройки устройства», затем «Экран и звук».", "Это устройство", "Модель", "ПО", "Работает",
            "Память и хранилище", "Память", "%s свободно из %s", "Хранилище", "Версия главного экрана",
            "Чтобы менять это здесь, снова запустите install.sh с компьютера", "Здесь нельзя показать заставку", "1 ч", "%d ч", "1 мин", "%d мин",
            "Заставка и сон", "Сеть и Bluetooth", "Экран и звук", "Об устройстве и память", "Все настройки Android", "Ethernet", "Wi-Fi", "Другое",
            "Очень слабый", "Слабый", "Средний", "Хороший", "Отличный",
        });
        LANGS.put("tr", new String[] {
            "Ayarlar", "Uygulama ekle", "Ağ", "Çevrimdışı", "Ses", "Görüntü",
            "Taşı", "Değiştir", "Ana ekrandan kaldır", "Kaldır", "Uygulama bilgisi",
            "Ok tuşlarıyla taşıyın, sonra OK'e basın", "Tüm uygulamalar zaten burada", "Uygulama ekle", "Değiştir: %s", "Duvar kağıdı", "TV ayarları", "Parlaklık",
            "Yeniden adlandır", "Gizli girişleri göster", "TV", "Dizüstü bilgisayar", "PC", "Blu-ray", "Soundbar", "Bir ad yazın…",
            // The Settings screen, machine-translated: corrections welcome.
            "Ana ekranın arkasındaki renk. Listede gezinirken her biri görünür; OK seçer.", "Duvar kağıdı seç",
            "Geri kalan her şey: hesaplar, uygulamalar, erişilebilirlik, tarih ve saat, dil, güncellemeler ve daha fazlası.", "Android ayarlarını aç",
            "Bunları burada değiştirmek için install.sh'i bilgisayarınızdan yeniden çalıştırın: bu ana ekrana izni bir kez verir. O zamana kadar bu sayfa yalnızca gösterir.",
            "Kapalı", "Ekran koruyucu", "Başlama süresi", "Ekran koruyucuyu başlatma süresi",
            "Uyku %s olarak ayarlandı, böylece ekran koruyucu görünebilir.", "Hiçbir zaman", "Ekran koruyucuyu önizle", "Uyku", "Uyku süresi",
            "Uyku moduna geçme süresi", "Süreler son basılan tuştan itibaren sayılır. Önce ekran koruyucu gelir, sonra uyku.", "Bağlantı",
            "Bağlantı türü", "Sinyal", "Bant", "Bağlantı hızı", "IP adresi", "Wi-Fi ve ağ ayarları", "Bluetooth", "Kullanılamıyor", "Açık",
            "Eşlenen cihazlar", "Yok", "Bağlı", "Bağlı değil", "Kumanda, oyun kolu veya kulaklık eşle", "Çözünürlük", "Yenileme hızı", "HDR",
            "TV hoparlörleri", "Ses çıkışı", "Surround biçimleri", "Görüntü ve parlaklık", "Çözünürlük, HDR ve ses",
            "NVIDIA Shield'da bunlar Cihaz Tercihleri, ardından Ekran ve Ses altındadır.", "Bu cihaz", "Model", "Yazılım", "Açık kalma süresi",
            "Bellek ve depolama", "Bellek", "%s boş / %s", "Depolama", "Ana ekran sürümü",
            "Bunu burada değiştirmek için install.sh'i bilgisayarınızdan yeniden çalıştırın", "Burada önizleme gösterilemiyor", "1 saat", "%d saat",
            "1 dakika", "%d dakika", "Ekran koruyucu ve uyku", "Ağ ve Bluetooth", "Ekran ve ses", "Cihaz bilgisi ve depolama", "Tüm Android ayarları",
            "Ethernet", "Wi-Fi", "Diğer", "Çok zayıf", "Zayıf", "Orta", "İyi", "Mükemmel",
        });
        LANGS.put("uk", new String[] {
            "Налаштування", "Додати додаток", "Мережа", "Офлайн", "Звук", "Зображення",
            "Перемістити", "Замінити", "Прибрати з головного екрана", "Видалити", "Інформація про додаток",
            "Переміщуйте стрілками, а потім натисніть OK", "Усі додатки вже тут", "Додати додаток", "Замінити %s", "Шпалери", "Налаштування телевізора", "Яскравість",
            "Перейменувати", "Показати приховані входи", "Телевізор", "Ноутбук", "ПК", "Blu-ray", "Саундбар", "Введіть назву…",
            // The Settings screen, machine-translated: corrections welcome.
            "Колір за головним екраном. Під час прокручування списку показується кожен; OK зберігає.", "Вибрати шпалери",
            "Усе інше: облікові записи, додатки, спеціальні можливості, дата й час, мова, оновлення та інше.", "Відкрити налаштування Android",
            "Щоб змінювати це тут, знову запустіть install.sh з комп'ютера: він один раз дасть головному екрану дозвіл. Доти ця сторінка лише показує значення.",
            "Вимк.", "Заставка", "Вмикається через", "Вмикати заставку через", "Сон перенесено на %s, щоб заставка встигла показатися.", "Ніколи",
            "Переглянути заставку", "Сон", "Засинає через", "Переводити в сон через",
            "Час відлічується від останнього натискання кнопки. Спершу з'являється заставка, потім сон.", "Підключення", "Підключено через", "Сигнал",
            "Діапазон", "Швидкість з'єднання", "IP-адреса", "Налаштування Wi-Fi і мережі", "Bluetooth", "Недоступно", "Увімк.", "Під'єднані пристрої",
            "Немає", "Підключено", "Не підключено", "Під'єднати пульт, геймпад або навушники", "Роздільна здатність", "Частота оновлення", "HDR",
            "Динаміки телевізора", "Звук іде на", "Формати об'ємного звуку", "Зображення і яскравість", "Роздільна здатність, HDR і звук",
            "На NVIDIA Shield це в розділі «Налаштування пристрою», потім «Екран і звук».", "Цей пристрій", "Модель", "ПЗ", "Працює",
            "Пам'ять і сховище", "Пам'ять", "%s вільно з %s", "Сховище", "Версія головного екрана",
            "Щоб змінювати це тут, знову запустіть install.sh з комп'ютера", "Тут не можна показати заставку", "1 год", "%d год", "1 хв", "%d хв",
            "Заставка і сон", "Мережа і Bluetooth", "Екран і звук", "Про пристрій і пам'ять", "Усі налаштування Android", "Ethernet", "Wi-Fi", "Інше",
            "Дуже слабкий", "Слабкий", "Середній", "Добрий", "Відмінний",
        });
        LANGS.put("zh", new String[] { // Simplified Chinese
            "设置", "添加应用", "网络", "未联网", "声音", "图像",
            "移动", "替换", "从主屏幕移除", "卸载", "应用信息",
            "用方向键移动，然后按 OK", "所有应用都已在这里", "添加应用", "替换 %s", "壁纸", "电视设置", "亮度",
            "重命名", "显示隐藏的输入", "电视", "笔记本电脑", "电脑", "蓝光", "条形音箱", "输入名称…",
            // The Settings screen, machine-translated: corrections welcome.
            "主屏幕背后的颜色。浏览列表会依次显示，按 OK 保留。", "选择壁纸", "其他所有设置：账号、应用、无障碍、日期和时间、语言、更新等。", "打开 Android 设置",
            "要在这里更改，请从电脑再次运行 install.sh：它会给这个主屏幕一次权限。在此之前，此页面只显示当前值。", "关闭", "屏幕保护程序", "启动时间", "多久后启动屏幕保护程序", "已将休眠改为 %s，让屏幕保护程序有时间显示。", "永不",
            "预览屏幕保护程序", "休眠", "休眠时间", "多久后进入休眠", "时间从最后一次按键开始计算。先显示屏幕保护程序，然后休眠。", "连接", "连接方式", "信号", "频段", "连接速度", "IP 地址", "Wi-Fi 和网络设置", "蓝牙",
            "不可用", "开启", "已配对的设备", "无", "已连接", "未连接", "配对遥控器、手柄或耳机", "分辨率", "刷新率", "HDR", "电视扬声器", "声音输出到", "环绕声格式", "图像和亮度", "分辨率、HDR 和声音",
            "在 NVIDIA Shield 上，它们位于“设备偏好设置”>“显示和声音”。", "本设备", "型号", "软件", "已运行", "内存和存储", "内存", "可用 %s，共 %s", "存储空间", "主屏幕版本",
            "要在这里更改，请从电脑再次运行 install.sh", "此处无法显示预览", "1 小时", "%d 小时", "1 分钟", "%d 分钟", "屏幕保护和休眠", "网络和蓝牙", "显示和声音", "设备信息和存储", "所有 Android 设置", "以太网",
            "Wi-Fi", "其他", "很弱", "弱", "一般", "良好", "极佳",
        });
        String[] hant = { // Traditional Chinese
            "設定", "新增應用程式", "網路", "離線", "音效", "影像",
            "移動", "取代", "從主畫面移除", "解除安裝", "應用程式資訊",
            "使用方向鍵移動，然後按 OK", "所有應用程式都已在這裡", "新增應用程式", "取代 %s", "桌布", "電視設定", "亮度",
            "重新命名", "顯示隱藏的輸入", "電視", "筆記型電腦", "電腦", "藍光", "聲霸", "輸入名稱…",
            // The Settings screen, machine-translated: corrections welcome.
            "主畫面背後的顏色。瀏覽清單會逐一顯示，按 OK 保留。", "選擇桌布", "其他所有設定：帳戶、應用程式、無障礙、日期與時間、語言、更新等。", "開啟 Android 設定",
            "如要在這裡變更，請從電腦再次執行 install.sh：它會給這個主畫面一次權限。在此之前，此頁面只會顯示目前的值。", "關閉", "螢幕保護程式", "啟動時間", "多久後啟動螢幕保護程式", "已將休眠改為 %s，讓螢幕保護程式有時間顯示。", "永不",
            "預覽螢幕保護程式", "休眠", "休眠時間", "多久後進入休眠", "時間從最後一次按鍵開始計算。先顯示螢幕保護程式，然後休眠。", "連線", "連線方式", "訊號", "頻段", "連線速度", "IP 位址", "Wi-Fi 與網路設定", "藍牙",
            "無法使用", "開啟", "已配對的裝置", "無", "已連線", "未連線", "配對遙控器、手把或耳機", "解析度", "更新率", "HDR", "電視喇叭", "聲音輸出到", "環繞音效格式", "影像與亮度", "解析度、HDR 與聲音",
            "在 NVIDIA Shield 上，它們位於「裝置偏好設定」>「顯示與音效」。", "這部裝置", "型號", "軟體", "已開機", "記憶體與儲存空間", "記憶體", "可用 %s，共 %s", "儲存空間", "主畫面版本",
            "如要在這裡變更，請從電腦再次執行 install.sh", "這裡無法顯示預覽", "1 小時", "%d 小時", "1 分鐘", "%d 分鐘", "螢幕保護與休眠", "網路與藍牙", "顯示與音效", "裝置資訊與儲存空間", "所有 Android 設定",
            "乙太網路", "Wi-Fi", "其他", "很弱", "弱", "普通", "良好", "極佳",
        };
        LANGS.put("zh-TW", hant);
        LANGS.put("zh-HK", hant);
        LANGS.put("zh-MO", hant);
    }

    private Strings() {}

    static String lang() { return Locale.getDefault().getLanguage(); }

    // The label in the TV's language, else the English one. Read on every call, so it follows the TV.
    static String t(String en) {
        Locale l = Locale.getDefault();
        String key = l.getLanguage() + "-" + l.getCountry();
        // Chinese can name its script outright (zh-Hant, zh-Hans-HK): that wins over the country.
        if ("zh".equals(l.getLanguage())) {
            if ("Hant".equals(l.getScript())) key = "zh-TW";
            else if ("Hans".equals(l.getScript())) key = "zh";
        }
        String[] table = LANGS.get(key);
        if (table == null) table = LANGS.get(l.getLanguage());
        if (table == null) return en;
        // A label added to EN but not yet to this table falls back to English.
        for (int i = 0; i < EN.length; i++)
            if (EN[i].equals(en)) return i < table.length ? table[i] : en;
        return en;
    }

    // The date above the clock: "Monday, 19 October" in English, the language's own order elsewhere.
    static String datePattern() {
        if ("en".equals(lang())) return "EEEE, d MMMM";
        return android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
    }
}
