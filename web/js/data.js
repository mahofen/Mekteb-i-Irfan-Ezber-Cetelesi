/**
 * MEKTEB-İ İRFAN EZBER ÇETELESİ - MÜFREDAT VE VERİ MODELLERİ
 * Kur'an-ı Kerim, Namaz Tesbihatı ve Risale-i Nur ezber müfredatı,
 * varsayılan talebe ve ders programı veritabanı.
 */

const CURRICULUM_DATA = {
  kuran: [
    "Duhâ Sûresi", "İnşirâh Sûresi", "Tîn Sûresi", "Alak Sûresi", "Kadir Sûresi",
    "Beyyine Sûresi", "Zilzâl Sûresi", "Âdiyât Sûresi", "Kâria Sûresi", "Tekâsür Sûresi",
    "Asr Sûresi", "Hümeze Sûresi", "Fîl Sûresi", "Kureyş Sûresi", "Mâûn Sûresi",
    "Kevser Sûresi", "Kâfirûn Sûresi", "Nasr Sûresi", "Tebbet Sûresi", "İhlâs Sûresi",
    "Felâk Sûresi", "Nâs Sûresi", "Legad Sadagallah", "İnnelil Müttekîne", "Âmenerrasûlü",
    "Lâ Yestevî", "Fâtiha Sûresi", "Elif-Lâm-Mîm (Bakara 1-5)", "Yâsîn Sûresi (1. Sayfa)",
    "Yâsîn Sûresi (2. Sayfa)", "Yâsîn Sûresi (3. Sayfa)", "Yâsîn Sûresi (4. Sayfa)",
    "Yâsîn Sûresi (5. Sayfa)", "Yâsîn Sûresi (6. Sayfa)", "Mülk Sûresi (Tebâreke)",
    "Nebe Sûresi (Amme)", "Rahmân Sûresi", "Vâkıa Sûresi", "Fetih Sûresi"
  ],
  tesbihat: [
    "Sübhâneke", "Ettehiyyâtü", "Allâhümme Salli", "Allâhümme Bârik", "Rabbenâ Âtinâ", "Rabbenagfirlî",
    "Allâhümme Lâ Tuhricnâ", "Kunût 1", "Kunût 2", "Ezan ve Kâmet", "Ezan Duası",
    "Salâten Tüncînâ", "Salât-ı Nâriye", "Âyet-el Kürsî", "Namaz Tesbihatı (33'lük Zikir)",
    "Namaz Duası", "İstiâze Duası", "Yemek Duası (Arapça & Osmanlıca)", "Tesbihat Ortak Kısım",
    "Duâ-yı Tercümân-ı İsm-i A'zam", "Duâ-yı İsm-i A'zam", "Sabah Namazı Sünnet-Farz Arası Dua",
    "Akşam ile Yatsı Arası Dua", "Akşam ile Yatsı Arası Tesbihler", "Vitir Namazından Sonra Dua",
    "Abdest Duaları", "Namaz için Arapça Niyetler", "İstiğfar Duası", "Sefer Duası",
    "Eve Girilirken Okunacak Dua", "Sabah ve Akşam Okunacak Dua", "Cenaze Duası",
    "Kabristanda Okunacak Dua", "Şifâ Âyetleri"
  ],
  risale: [
    "Birinci Söz (Bismillah her hayrın başıdır)", "Yazı Mektubu", "Onuncu Huccet-i Îmâniye",
    "Mukaddime", "Birinci Kelime", "İkinci Kelime", "Üçüncü Kelime", "Dördüncü Kelime",
    "Beşinci Kelime", "Altıncı Kelime", "Yedinci Kelime", "Sekizinci Kelime", "Dokuzuncu Kelime",
    "Onuncu Kelime", "On Birinci Kelime", "Altıncı Huccet-i Îmâniye", "Mu'cizât-ı Ahmediye (asm)",
    "Birinci Reşha", "İkinci Reşha", "Üçüncü Reşha", "Dördüncü Reşha", "Beşinci Reşha",
    "Altıncı Reşha", "Yedinci Reşha", "Sekizinci Reşha", "Dokuzuncu Reşha", "Onuncu Reşha",
    "On Birinci Reşha", "On İkinci Reşha", "On Üçüncü Reşha", "On Dördüncü Reşha",
    "İhlâs Risâlesi (Giriş)", "Birinci Düstûrunuz", "İkinci Düstûrunuz", "Üçüncü Düstûrunuz",
    "Dördüncü Düstûrunuz", "İhlâsı Kazanmanın Birinci Sebebi", "İkinci Sebeb",
    "İhlâsı Kıran Birinci Mâni'", "İkinci Mâni'", "Üçüncü Mâni'", "Tabiat Risâlesi (Mukaddime)",
    "Birinci Mes'ele", "İkinci Mes'ele", "Üçüncü Mes'ele", "Hâtime ve Duâ"
  ]
};

const DEFAULT_STUDENTS = [
  {
    id: 101,
    studentNumber: "101",
    username: "ahmet101",
    fullName: "Ahmet Yılmaz",
    grade: "7. Sınıf",
    status: "Aktif",
    avatarColorIndex: 0,
    parentPhone: "0532 100 2030",
    joinDate: "2025-09-15",
    pin: "1234",
    accessCode: "IRF-101",
    notes: "Ezber kabiliyeti çok yüksek, tecvidi iyi."
  },
  {
    id: 102,
    studentNumber: "102",
    username: "mehmet102",
    fullName: "Mehmet Akif Kaya",
    grade: "Hafızlık Grubu",
    status: "Aktif",
    avatarColorIndex: 1,
    parentPhone: "0533 200 4050",
    joinDate: "2025-09-10",
    pin: "1234",
    accessCode: "IRF-102",
    notes: "30. Cüz ezberini tamamladı, Yâsin sûresine geçti."
  },
  {
    id: 103,
    studentNumber: "103",
    username: "omer103",
    fullName: "Ömer Faruk Demir",
    grade: "8. Sınıf",
    status: "Aktif",
    avatarColorIndex: 2,
    parentPhone: "0542 300 5060",
    joinDate: "2025-10-01",
    pin: "1234",
    accessCode: "IRF-103",
    notes: "Namaz tesbihatlarını eksiksiz veriyor."
  },
  {
    id: 104,
    studentNumber: "104",
    username: "mustafa104",
    fullName: "Mustafa Enes Çelik",
    grade: "6. Sınıf",
    status: "Aktif",
    avatarColorIndex: 3,
    parentPhone: "0555 400 6070",
    joinDate: "2025-10-15",
    pin: "1234",
    accessCode: "IRF-104",
    notes: "Kısa sûreleri tamamlamak üzere."
  },
  {
    id: 105,
    studentNumber: "105",
    username: "ali105",
    fullName: "Ali Osman Şahin",
    grade: "7. Sınıf",
    status: "Aktif",
    avatarColorIndex: 4,
    parentPhone: "0505 500 7080",
    joinDate: "2025-11-01",
    pin: "1234",
    accessCode: "IRF-105",
    notes: "Risale-i Nur ezberlerinde gayretli."
  },
  {
    id: 106,
    studentNumber: "106",
    username: "hamza106",
    fullName: "Hamza Karaca",
    grade: "Hafızlık Grubu",
    status: "Aktif",
    avatarColorIndex: 5,
    parentPhone: "0536 600 8090",
    joinDate: "2025-09-01",
    pin: "1234",
    accessCode: "IRF-106",
    notes: "Günlük virdlerini intizamla takip ediyor."
  }
];

const DEFAULT_SCHEDULE = [
  {
    id: "lesson_1",
    day: "PAZARTESI",
    title: "Kur'an-ı Kerim Ezber Dersi",
    category: "KURAN",
    startTime: "09:00",
    endTime: "10:30",
    targetGrade: "Tüm Sınıflar",
    classroom: "Hafızlık Salonu 1",
    teacherName: "Ahmet Hoca",
    description: "Yeni ezber dinleme, tecvid ve mahreç kontrolü"
  },
  {
    id: "lesson_2",
    day: "PAZARTESI",
    title: "Namaz Tesbihatı ve Dua Talimi",
    category: "DUA",
    startTime: "11:00",
    endTime: "12:15",
    targetGrade: "6. Sınıf",
    classroom: "Derslik A",
    teacherName: "Mehmet Hoca",
    description: "Tercüman-ı İsm-i A'zam ve aşır talimi"
  },
  {
    id: "lesson_3",
    day: "SALI",
    title: "Risale-i Nur İman Dersi ve Ezber",
    category: "RISALE",
    startTime: "09:30",
    endTime: "11:00",
    targetGrade: "7. Sınıf",
    classroom: "Derslik B",
    teacherName: "Ali Hoca",
    description: "İhlâs Risalesi 4 düstur ezber mütalaası"
  },
  {
    id: "lesson_4",
    day: "CARSAMBA",
    title: "Hafızlık Has ve Çiğ Tekrarı",
    category: "KURAN",
    startTime: "14:00",
    endTime: "16:00",
    targetGrade: "Hafızlık Grubu",
    classroom: "Hafızlık Salonu 2",
    teacherName: "Hafız Hasan Hoca",
    description: "Geçmiş cüzlerin sağlamlaştırılması"
  },
  {
    id: "lesson_5",
    day: "PERSEMBE",
    title: "Arapça Kaideler ve Osmanlıca Metin",
    category: "DIGER",
    startTime: "10:00",
    endTime: "11:30",
    targetGrade: "8. Sınıf",
    classroom: "Derslik C",
    teacherName: "Ahmet Hoca",
    description: "Temel sarf nahiv ve hat okumaları"
  },
  {
    id: "lesson_6",
    day: "CUMA",
    title: "Cuma Sohbeti ve Haftalık Değerlendirme",
    category: "AHLAK",
    startTime: "14:30",
    endTime: "16:00",
    targetGrade: "Tüm Sınıflar",
    classroom: "Büyük Mescid",
    teacherName: "Müdür Bey",
    description: "Haftalık başarı karneleri ve rozet takdimi"
  }
];

const DEFAULT_MEMORIZATION_RECORDS = [
  {
    id: 1,
    studentId: 101,
    title: "Duhâ Sûresi",
    category: "Kur'an",
    status: "TAMAMLANDI",
    score: 5,
    rating: 5,
    repeatCount: 15,
    teacherNotes: "Mahreçleri ve tecvidi kusursuz, maşallah!",
    assignedDate: "2026-02-10",
    completedDate: "2026-02-18",
    teacherName: "Ahmet Hoca"
  },
  {
    id: 2,
    studentId: 101,
    title: "İnşirâh Sûresi",
    category: "Kur'an",
    status: "TAMAMLANDI",
    score: 5,
    rating: 5,
    repeatCount: 12,
    teacherNotes: "Çok akıcı okundu.",
    assignedDate: "2026-02-19",
    completedDate: "2026-02-25",
    teacherName: "Ahmet Hoca"
  },
  {
    id: 3,
    studentId: 101,
    title: "Tîn Sûresi",
    category: "Kur'an",
    status: "DEVAM_EDIYOR",
    score: 4,
    rating: 4,
    repeatCount: 8,
    teacherNotes: "Son 2 ayet tekrar edilecek.",
    assignedDate: "2026-02-26",
    completedDate: null,
    teacherName: "Ahmet Hoca"
  },
  {
    id: 4,
    studentId: 102,
    title: "Mülk Sûresi (Tebâreke)",
    category: "Kur'an",
    status: "TAMAMLANDI",
    score: 5,
    rating: 5,
    repeatCount: 30,
    teacherNotes: "Tam ezberlendi, hıfzı kuvvetli.",
    assignedDate: "2026-01-15",
    completedDate: "2026-02-20",
    teacherName: "Hafız Hasan Hoca"
  },
  {
    id: 5,
    studentId: 102,
    title: "Nebe Sûresi (Amme)",
    category: "Kur'an",
    status: "DEVAM_EDIYOR",
    score: 4,
    rating: 4,
    repeatCount: 14,
    teacherNotes: "1. sayfa tamamlandı, 2. sayfa çalışılıyor.",
    assignedDate: "2026-02-22",
    completedDate: null,
    teacherName: "Hafız Hasan Hoca"
  },
  {
    id: 6,
    studentId: 103,
    title: "Duâ-yı Tercümân-ı İsm-i A'zam",
    category: "Tesbihat",
    status: "TAMAMLANDI",
    score: 5,
    rating: 5,
    repeatCount: 20,
    teacherNotes: "Eksiksiz ve çok güzel ezberlendi.",
    assignedDate: "2026-02-01",
    completedDate: "2026-02-15",
    teacherName: "Mehmet Hoca"
  }
];

const DEFAULT_BADGES = [
  { id: "b1", title: "Hafızlık Yolcusu", icon: "fa-quran", desc: "10'dan fazla sûre tamamlandı", color: "#8B5CF6" },
  { id: "b2", title: "Namaz Muhafızı", icon: "fa-mosque", desc: "Haftalık 5 vakit tam kılındı", color: "#10B981" },
  { id: "b3", title: "Vird Ehli", icon: "fa-hands-praying", desc: "Günlük tesbihat ve cevşen tamamlandı", color: "#D4AF37" },
  { id: "b4", title: "İlim Talebesi", icon: "fa-book-open", desc: "Risale-i Nur ezberleri tamamlandı", color: "#06B6D4" },
  { id: "b5", title: "İntizam Öncüsü", icon: "fa-star", desc: "30 gün kesintisiz yoklama tam", color: "#F59E0B" }
];
