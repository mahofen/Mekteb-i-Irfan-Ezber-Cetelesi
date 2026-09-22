/**
 * MEKTEB-İ İRFAN EZBER ÇETELESİ - ANA WEB UYGULAMASI (SPA)
 * Reaktif durum yönetimi, dinamik ekran işleyicileri ve Selçuklu Tezhip UI etkileşimleri.
 */

// Uygulama Durumu (App State)
const AppState = {
  currentScreen: "MAIN_MENU", // "LOGIN", "MAIN_MENU", "SCHEDULE", "DUTIES", "MEMORIZATION", "STUDENTS", "REPORTS", "SETTINGS", "STUDENT_PORTAL"
  selectedDate: new Date().toISOString().split('T')[0],
  selectedScheduleDay: getTodayDayOfWeek(),
  selectedGradeFilter: "Tümü",
  searchQuery: "",
  theme: localStorage.getItem("mekteb_theme") || "light",
  
  // Hoca ve Talebe Oturumları
  userSession: {
    isLoggedIn: true,
    username: "admin",
    displayName: "Yönetici Hoca",
    role: "Yönetici (Admin)"
  },
  studentSession: {
    isLoggedIn: false,
    student: null
  },

  // Modal Durumları
  activeModal: null, // "NEW_LESSON", "NEW_STUDENT", "ASSIGN_MEMORIZATION", "EVALUATE_MEMORIZATION", "DUTY_NOTE", "EXPORT_IMPORT", "STUDENT_SELECT"
  modalData: null
};

function getTodayDayOfWeek() {
  const days = ["PAZAR", "PAZARTESI", "SALI", "CARSAMBA", "PERSEMBE", "CUMA", "CUMARTESI"];
  return days[new Date().getDay()];
}

// Uygulama Başlatma
document.addEventListener("DOMContentLoaded", () => {
  StorageManager.init();

  // Yönetici Oturumu Kontrolü
  const savedTeacherSession = StorageManager.getTeacherSession();
  if (savedTeacherSession && savedTeacherSession.isLoggedIn) {
    AppState.userSession = savedTeacherSession;
  } else {
    AppState.userSession = {
      isLoggedIn: false,
      username: "",
      displayName: "",
      role: ""
    };
    if (AppState.currentScreen !== "STUDENT_PORTAL") {
      AppState.currentScreen = "LOGIN";
    }
  }

  // Talebe Oturumu Kontrolü
  const savedStudentSession = StorageManager.getStudentSession();
  if (savedStudentSession && savedStudentSession.student) {
    AppState.studentSession = savedStudentSession;
  }

  applyTheme(AppState.theme);
  setupGlobalListeners();
  navigateTo(AppState.currentScreen);
});

// Tema Uygula
function applyTheme(theme) {
  AppState.theme = theme;
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("mekteb_theme", theme);
  const themeToggleIcon = document.getElementById("theme-toggle-icon");
  if (themeToggleIcon) {
    themeToggleIcon.className = theme === "dark" ? "fas fa-sun text-amber-400" : "fas fa-moon text-indigo-200";
  }
}

function toggleTheme() {
  applyTheme(AppState.theme === "dark" ? "light" : "dark");
}

// Navigasyon Yöneticisi
function navigateTo(screen, data = null) {
  AppState.currentScreen = screen;
  AppState.searchQuery = "";
  
  // Sayfa başlığı ve görünümü güncelle
  window.scrollTo({ top: 0, behavior: 'smooth' });
  
  // Ekran Render Fonksiyonlarını Çağır
  renderApp();
  updateNavHighlights();
}

// Bildirim (Toast) Gösterici
function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  const bgClass = type === "success" ? "bg-emerald-800 text-white" : type === "error" ? "bg-rose-800 text-white" : "bg-slate-800 text-white";
  const icon = type === "success" ? "fa-circle-check text-emerald-400" : type === "error" ? "fa-triangle-exclamation text-rose-400" : "fa-circle-info text-amber-400";
  
  toast.className = `flex items-center gap-3 px-4 py-3 rounded-xl shadow-2xl border border-amber-500/30 ${bgClass} transform transition-all duration-300 translate-y-4 opacity-0 text-sm font-semibold z-50`;
  toast.innerHTML = `<i class="fas ${icon} text-lg"></i> <span>${message}</span>`;
  
  container.appendChild(toast);
  
  setTimeout(() => {
    toast.classList.remove("translate-y-4", "opacity-0");
  }, 10);

  setTimeout(() => {
    toast.classList.add("opacity-0", "translate-y-2");
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}

// Kutlama Konfetisi
function triggerConfetti() {
  if (typeof confetti === "function") {
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 },
      colors: ['#D4AF37', '#10B981', '#8B5CF6', '#F59E0B']
    });
  }
}

// Ana Render Fonksiyonu
function renderApp() {
  const contentArea = document.getElementById("app-content");
  if (!contentArea) return;

  // Eğer yönetici giriş yapmamışsa ve talep edilen ekran talebe portalı değilse, Yönetici Giriş Ekranını göster
  if ((!AppState.userSession || !AppState.userSession.isLoggedIn) && AppState.currentScreen !== "STUDENT_PORTAL") {
    contentArea.innerHTML = renderTeacherLoginScreen();
    updateNavHighlights();
    attachDynamicEventListeners();
    return;
  }

  const students = StorageManager.getStudents();
  const schedule = StorageManager.getSchedule();
  const memorization = StorageManager.getMemorization();
  const attendance = StorageManager.getAttendance();
  const duties = StorageManager.getDuties();

  switch (AppState.currentScreen) {
    case "MAIN_MENU":
      contentArea.innerHTML = renderTeacherDashboard(students, memorization, attendance, duties);
      break;
    case "SCHEDULE":
      contentArea.innerHTML = renderScheduleScreen(schedule, students, attendance);
      break;
    case "DUTIES":
      contentArea.innerHTML = renderDutiesScreen(students, duties);
      break;
    case "MEMORIZATION":
      contentArea.innerHTML = renderMemorizationScreen(students, memorization);
      break;
    case "STUDENTS":
      contentArea.innerHTML = renderStudentsScreen(students, memorization, attendance, duties);
      break;
    case "REPORTS":
      contentArea.innerHTML = renderReportsScreen(students, memorization, attendance, duties);
      break;
    case "SETTINGS":
      contentArea.innerHTML = renderSettingsScreen();
      break;
    case "STUDENT_PORTAL":
      contentArea.innerHTML = renderStudentPortalScreen(students, memorization, duties);
      break;
    case "LOGIN":
      contentArea.innerHTML = renderTeacherLoginScreen();
      break;
    default:
      contentArea.innerHTML = renderTeacherDashboard(students, memorization, attendance, duties);
  }

  // Event bağlamaları
  attachDynamicEventListeners();
}

// Alt ve Üst Menü Aktiflik Güncelleme
function updateNavHighlights() {
  document.querySelectorAll("[data-nav-target]").forEach(el => {
    const target = el.getAttribute("data-nav-target");
    if (target === AppState.currentScreen) {
      el.classList.add("active");
    } else {
      el.classList.remove("active");
    }
  });

  const isTeacherLoggedIn = AppState.userSession && AppState.userSession.isLoggedIn;
  const isStudentPortal = AppState.currentScreen === "STUDENT_PORTAL";
  const isLoginScreen = AppState.currentScreen === "LOGIN" || !isTeacherLoggedIn;

  // Masaüstü gezinme menüsü (Header Nav)
  const headerNav = document.querySelector("header nav");
  if (headerNav) {
    headerNav.style.display = isTeacherLoggedIn && !isStudentPortal ? "flex" : "none";
  }

  // Ayarlar butonu
  const settingsBtn = document.getElementById("header-settings-btn");
  if (settingsBtn) {
    settingsBtn.style.display = isTeacherLoggedIn && !isStudentPortal ? "flex" : "none";
  }

  // Mobil alt menü
  const bottomNav = document.getElementById("mobile-bottom-nav");
  if (bottomNav) {
    bottomNav.style.display = isTeacherLoggedIn && !isStudentPortal && !isLoginScreen ? "flex" : "none";
  }

  // Header oturum rozeti / çıkış butonu
  const sessionBadge = document.getElementById("teacher-session-badge");
  if (sessionBadge) {
    if (isTeacherLoggedIn) {
      sessionBadge.innerHTML = `
        <div class="flex items-center gap-2">
          <span class="hidden lg:inline text-xs font-semibold text-slate-300">
            <span class="text-amber-400 font-bold">${AppState.userSession.displayName}</span>
          </span>
          <button onclick="logoutTeacher()" class="w-9 h-9 rounded-xl bg-rose-600/20 hover:bg-rose-600/40 text-rose-300 border border-rose-500/30 flex items-center justify-center transition" title="Yönetici Oturumunu Kapat">
            <i class="fas fa-right-from-bracket text-xs"></i>
          </button>
        </div>
      `;
    } else {
      sessionBadge.innerHTML = `
        <button onclick="navigateTo('LOGIN')" class="px-3 py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 text-xs font-bold flex items-center gap-1.5 transition">
          <i class="fas fa-lock text-[10px]"></i>
          <span class="hidden sm:inline">Hoca Girişi</span>
        </button>
      `;
    }
  }
}

// =========================================================================
// 0. YÖNETİCİ & EĞİTMEN GİRİŞ EKRANI (TEACHER LOGIN SCREEN)
// =========================================================================
function renderTeacherLoginScreen() {
  const currentSchoolCode = StorageManager.getSchoolCode();

  return `
    <div class="max-w-md mx-auto my-6 sm:my-10 space-y-6 pb-16">
      <!-- 1. Üst Tezhip Başlık Kartı -->
      <div class="tezhip-card p-6 sm:p-8 bg-gradient-to-b from-[#0D1B2A] to-[#1E293B] text-white text-center relative overflow-hidden shadow-2xl border-2 border-amber-500/40">
        <div class="ornament-corner-tr"></div>
        <div class="ornament-corner-bl"></div>

        <div class="seljuk-star gold w-16 h-16 text-3xl mx-auto mb-3 shadow-xl">
          <i class="fas fa-shield-halved text-[#0D1B2A]"></i>
        </div>
        <span class="text-xs font-black tracking-widest text-amber-400 uppercase">MEKTEB-İ İRFAN</span>
        <h2 class="text-2xl font-black text-white mt-1">Yönetici & Hoca Girişi</h2>
        <p class="text-xs text-slate-300 mt-1">Ezber, Yoklama ve Talebe Yönetim Paneli</p>
      </div>

      <!-- 2. Giriş Formu Kartı -->
      <div class="tezhip-card p-6 space-y-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl">
        <!-- Hata Bildirimi -->
        <div id="teacher-login-error" class="hidden p-3 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-400/40 text-rose-700 dark:text-rose-300 text-xs font-bold flex items-center gap-2">
          <i class="fas fa-triangle-exclamation text-sm flex-shrink-0"></i>
          <span id="teacher-login-error-text">Hatalı bilgi</span>
        </div>

        <form onsubmit="event.preventDefault(); handleTeacherLoginSubmit();" class="space-y-4">
          <!-- 1. Kullanıcı Adı veya E-posta -->
          <div>
            <label class="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">
              Yönetici Kullanıcı Adı veya E-posta
            </label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <i class="fas fa-user-shield text-xs"></i>
              </span>
              <input id="teacher-login-username" type="text" placeholder="Örn: admin veya hoca" required autocomplete="username"
                class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-sm font-semibold focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
            </div>
          </div>

          <!-- 2. Şifre -->
          <div>
            <label class="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">
              Yönetici Şifresi
            </label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <i class="fas fa-lock text-xs"></i>
              </span>
              <input id="teacher-login-password" type="password" placeholder="Şifrenizi giriniz (Varsayılan: 1234)" required autocomplete="current-password"
                class="w-full pl-9 pr-10 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-sm font-semibold focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
              <button type="button" onclick="togglePasswordVisibility('teacher-login-password', 'teacher-login-eye')" class="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                <i id="teacher-login-eye" class="fas fa-eye text-xs"></i>
              </button>
            </div>
          </div>

          <!-- 3. Kurum Kodu (İsteğe Bağlı) -->
          <div>
            <label class="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center justify-between mb-1">
              <span>Kurum / Medrese Kodu</span>
              <span class="text-[10px] text-slate-400 font-normal">Varsayılan: ${currentSchoolCode}</span>
            </label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-amber-600">
                <i class="fas fa-landmark text-xs"></i>
              </span>
              <input id="teacher-login-school-code" type="text" value="${currentSchoolCode}" placeholder="Kurum kodu (Örn: irfan_2026)"
                class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-sm font-mono font-bold uppercase focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
            </div>
          </div>

          <!-- Giriş Yap Butonu -->
          <button type="submit" class="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#0D1B2A] via-[#1E293B] to-[#0D1B2A] hover:bg-slate-800 text-amber-300 font-black text-sm shadow-xl border border-amber-500/40 flex items-center justify-center gap-2 transition transform active:scale-98">
            <i class="fas fa-arrow-right-to-bracket"></i>
            <span>Yönetici Paneline Giriş Yap</span>
          </button>
        </form>

        <!-- Hızlı Demo Rol Seçicileri -->
        <div class="pt-4 border-t border-slate-200 dark:border-slate-800">
          <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-2 text-center">
            Hızlı Rol Seçimi (Tek Tıkla Doldur):
          </span>
          <div class="grid grid-cols-3 gap-2">
            <button type="button" onclick="fillTeacherLoginForm('admin', '1234', '${currentSchoolCode}')"
              class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/80 hover:bg-amber-50 dark:hover:bg-amber-950/40 border border-slate-200 dark:border-slate-700 text-center transition group">
              <i class="fas fa-user-gear text-amber-600 mb-1 block"></i>
              <div class="text-xs font-bold text-slate-800 dark:text-slate-200 group-hover:text-amber-600">Admin</div>
              <div class="text-[10px] text-slate-400 font-mono">1234</div>
            </button>

            <button type="button" onclick="fillTeacherLoginForm('hoca', '1234', '${currentSchoolCode}')"
              class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/80 hover:bg-amber-50 dark:hover:bg-amber-950/40 border border-slate-200 dark:border-slate-700 text-center transition group">
              <i class="fas fa-chalkboard-user text-emerald-600 mb-1 block"></i>
              <div class="text-xs font-bold text-slate-800 dark:text-slate-200 group-hover:text-emerald-600">Hoca</div>
              <div class="text-[10px] text-slate-400 font-mono">1234</div>
            </button>

            <button type="button" onclick="fillTeacherLoginForm('hafiz', '1234', '${currentSchoolCode}')"
              class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/80 hover:bg-amber-50 dark:hover:bg-amber-950/40 border border-slate-200 dark:border-slate-700 text-center transition group">
              <i class="fas fa-book-open-reader text-purple-600 mb-1 block"></i>
              <div class="text-xs font-bold text-slate-800 dark:text-slate-200 group-hover:text-purple-600">Hafız</div>
              <div class="text-[10px] text-slate-400 font-mono">1234</div>
            </button>
          </div>
        </div>

        <!-- Talebe Portalına Geçiş Butonu -->
        <div class="text-center pt-2">
          <button type="button" onclick="navigateTo('STUDENT_PORTAL')" class="text-xs font-bold text-emerald-600 hover:text-emerald-500 transition flex items-center justify-center gap-1.5 mx-auto">
            <i class="fas fa-graduation-cap"></i>
            <span>Talebe Portalı Girişine Geç ➔</span>
          </button>
        </div>
      </div>
    </div>
  `;
}

// Yönetici Giriş Formu Gönderme (Teacher Login Submit)
function handleTeacherLoginSubmit() {
  const usernameInput = document.getElementById("teacher-login-username")?.value;
  const passwordInput = document.getElementById("teacher-login-password")?.value;
  const schoolCodeInput = document.getElementById("teacher-login-school-code")?.value;
  const errorBox = document.getElementById("teacher-login-error");
  const errorText = document.getElementById("teacher-login-error-text");

  if (errorBox) errorBox.classList.add("hidden");

  const validation = StorageManager.validateTeacherLogin(usernameInput, passwordInput, schoolCodeInput);

  if (!validation.success) {
    if (errorBox && errorText) {
      errorText.textContent = validation.message;
      errorBox.classList.remove("hidden");
    } else {
      showToast(validation.message, "error");
    }
    return;
  }

  // Başarılı yönetici girişi
  AppState.userSession = validation.user;
  StorageManager.saveTeacherSession(validation.user);

  showToast(`Hoş geldiniz, ${validation.user.displayName}! Yönetim paneli açıldı. 👑`, "success");
  triggerConfetti();
  navigateTo("MAIN_MENU");
}

function fillTeacherLoginForm(username, password, schoolCode) {
  const uInput = document.getElementById("teacher-login-username");
  const pInput = document.getElementById("teacher-login-password");
  const cInput = document.getElementById("teacher-login-school-code");
  const errorBox = document.getElementById("teacher-login-error");

  if (uInput) uInput.value = username;
  if (pInput) pInput.value = password;
  if (cInput) cInput.value = schoolCode;
  if (errorBox) errorBox.classList.add("hidden");

  showToast(`${username} hesabı seçildi. 'Giriş Yap' butonuna basabilirsiniz.`);
}

function logoutTeacher() {
  AppState.userSession = {
    isLoggedIn: false,
    username: "",
    displayName: "",
    role: ""
  };
  StorageManager.clearTeacherSession();
  showToast("Yönetici oturumu güvenli şekilde kapatıldı.");
  navigateTo("LOGIN");
}

// =========================================================================
// 1. EĞİTMEN ANA SAYFA (DASHBOARD)
// =========================================================================
function renderTeacherDashboard(students, memorization, attendance, duties) {
  const today = AppState.selectedDate;
  const totalStudents = students.length;
  const todayAtt = attendance.filter(a => a.date === today);
  const presentCount = todayAtt.filter(a => a.status === "GELDI").length;
  const attPercent = totalStudents > 0 ? Math.round((presentCount / totalStudents) * 100) : 100;

  const todayDuties = duties.filter(d => d.date === today);
  const fullPrayers = todayDuties.filter(d => d.fajr && d.dhuhr && d.asr && d.maghrib && d.isha).length;
  const dutyPercent = totalStudents > 0 ? Math.round((fullPrayers / totalStudents) * 100) : 0;

  const inProgressMem = memorization.filter(m => m.status === "DEVAM_EDIYOR").length;
  const completedMem = memorization.filter(m => m.status === "TAMAMLANDI").length;
  const totalMem = memorization.length;
  const memPercent = totalMem > 0 ? Math.round((completedMem / totalMem) * 100) : 0;

  return `
    <div class="space-y-6 pb-20 max-w-6xl mx-auto">
      <!-- 1. Selçuklu Varak Başlık Bannerı -->
      <div class="tezhip-card p-6 relative overflow-hidden bg-gradient-to-r from-[#0D1B2A] via-[#1E293B] to-[#0D1B2A] text-white">
        <div class="ornament-corner-tr"></div>
        <div class="ornament-corner-bl"></div>
        
        <div class="flex flex-col md:flex-row items-center justify-between gap-4 relative z-10">
          <div class="flex items-center gap-4">
            <div class="seljuk-star gold w-14 h-14 text-2xl flex-shrink-0">
              <i class="fas fa-mosque"></i>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span class="text-xs font-bold text-amber-400 tracking-widest uppercase">Mekteb-i İrfan</span>
                <span class="px-2 py-0.5 rounded-full text-[10px] bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/40">Firestore Bulut Aktif</span>
              </div>
              <h1 class="text-2xl md:text-3xl font-black tracking-tight text-white">Ezber & Talebe Yönetim Paneli</h1>
              <p class="text-xs text-slate-300 mt-1">Eğitmen: <span class="font-bold text-amber-300">${AppState.userSession.displayName}</span> • Kurum Kodu: <span class="font-mono text-amber-200">irfan_2026</span></p>
            </div>
          </div>

          <!-- Talebe Portalına Geçiş Butonu -->
          <button onclick="navigateTo('STUDENT_PORTAL')" class="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-900/30 transition transform hover:-translate-y-0.5">
            <i class="fas fa-graduation-cap text-amber-300"></i>
            <span>Talebe Portalına Geç</span>
            <i class="fas fa-arrow-right text-xs ml-1"></i>
          </button>
        </div>
      </div>

      <!-- 2. GENİŞ KART: DERS PROGRAMI & CANLI YOKLAMA -->
      <div onclick="navigateTo('SCHEDULE')" class="tezhip-card p-5 tezhip-card-interactive hover:border-amber-400 cursor-pointer group">
        <div class="flex items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="seljuk-star w-12 h-12 text-xl bg-[#0D1B2A] text-amber-400">
              <i class="fas fa-calendar-check"></i>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-lg font-black text-slate-900 dark:text-white group-hover:text-amber-600 transition">Ders Programı & Canlı Yoklama</h2>
                <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">Bugün ${AppState.selectedScheduleDay}</span>
              </div>
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Haftalık ders saatleri, sınıflar & tek tıkla canlı yoklama alma</p>
            </div>
          </div>
          
          <div class="flex items-center gap-3">
            <div class="text-right hidden sm:block">
              <div class="text-sm font-black text-emerald-600 dark:text-emerald-400">%${attPercent} Katılım</div>
              <div class="text-[11px] text-slate-400">${presentCount} / ${totalStudents} Derste</div>
            </div>
            <div class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-600 dark:text-slate-300 group-hover:bg-amber-500 group-hover:text-white transition">
              <i class="fas fa-chevron-right text-xs"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. DÖRT ANA MODÜL ÇİFT KARTLARI -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Kart 1: Günlük Görevler -->
        <div onclick="navigateTo('DUTIES')" class="tezhip-card p-5 tezhip-card-interactive group">
          <div class="flex items-center justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center text-lg">
              <i class="fas fa-clipboard-check"></i>
            </div>
            <span class="text-xs font-bold text-emerald-600 dark:text-emerald-400">%${dutyPercent} Tam</span>
          </div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Günlük Görev</h3>
          <div class="text-xl font-black text-emerald-600 dark:text-emerald-400 mt-1">${fullPrayers} <span class="text-xs font-semibold text-slate-400">/ ${totalStudents} 5 Vakit</span></div>
          <p class="text-[11px] text-slate-400 mt-1">Namaz, Kur'an & Risale virdleri</p>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-3 overflow-hidden">
            <div class="bg-emerald-500 h-full rounded-full transition-all duration-500" style="width: ${dutyPercent}%"></div>
          </div>
        </div>

        <!-- Kart 2: Ezber Çetelesi -->
        <div onclick="navigateTo('MEMORIZATION')" class="tezhip-card p-5 tezhip-card-interactive group">
          <div class="flex items-center justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-purple-50 dark:bg-purple-950/40 text-purple-600 dark:text-purple-400 flex items-center justify-center text-lg">
              <i class="fas fa-book-quran"></i>
            </div>
            <span class="text-xs font-bold text-purple-600 dark:text-purple-400">%${memPercent}</span>
          </div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Ezber Çetelesi</h3>
          <div class="text-xl font-black text-purple-600 dark:text-purple-400 mt-1">${completedMem} <span class="text-xs font-semibold text-slate-400">Teslim • ${inProgressMem} Aktif</span></div>
          <p class="text-[11px] text-slate-400 mt-1">Sure, cüz & aşır puanlama</p>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-3 overflow-hidden">
            <div class="bg-purple-500 h-full rounded-full transition-all duration-500" style="width: ${memPercent}%"></div>
          </div>
        </div>

        <!-- Kart 3: Talebeler -->
        <div onclick="navigateTo('STUDENTS')" class="tezhip-card p-5 tezhip-card-interactive group">
          <div class="flex items-center justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center text-lg">
              <i class="fas fa-users"></i>
            </div>
            <span class="text-xs font-bold text-blue-600 dark:text-blue-400">${totalStudents} Kayıt</span>
          </div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Talebeler</h3>
          <div class="text-xl font-black text-blue-600 dark:text-blue-400 mt-1">${totalStudents} <span class="text-xs font-semibold text-slate-400">Aktif Talebe</span></div>
          <p class="text-[11px] text-slate-400 mt-1">Talebe listesi & profiller</p>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-3 overflow-hidden">
            <div class="bg-blue-500 h-full rounded-full" style="width: 100%"></div>
          </div>
        </div>

        <!-- Kart 4: Karneler & Raporlar -->
        <div onclick="navigateTo('REPORTS')" class="tezhip-card p-5 tezhip-card-interactive group">
          <div class="flex items-center justify-between mb-3">
            <div class="w-10 h-10 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 flex items-center justify-center text-lg">
              <i class="fas fa-file-invoice"></i>
            </div>
            <span class="text-xs font-bold text-amber-600 dark:text-amber-400">PDF / Yazdır</span>
          </div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Karneler & Rapor</h3>
          <div class="text-xl font-black text-amber-600 dark:text-amber-400 mt-1">Gelişim <span class="text-xs font-semibold text-slate-400">Analizi</span></div>
          <p class="text-[11px] text-slate-400 mt-1">Resmi karne ve haftalık çizelge</p>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-3 overflow-hidden">
            <div class="bg-amber-500 h-full rounded-full" style="width: 85%"></div>
          </div>
        </div>
      </div>

      <!-- 4. HIZLI TALEBE LİSTESİ ÖZETİ -->
      <div class="tezhip-card p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="seljuk-star w-8 h-8 text-sm bg-slate-800 text-amber-300"><i class="fas fa-user-graduate"></i></div>
            <h3 class="text-base font-black text-slate-900 dark:text-white">Talebe İlerleme Özeti</h3>
          </div>
          <button onclick="navigateTo('STUDENTS')" class="text-xs font-bold text-amber-600 hover:text-amber-500">Tümünü Gör ➔</button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          ${students.slice(0, 4).map(st => {
            const stMem = memorization.filter(m => m.studentId === st.id);
            const stCompleted = stMem.filter(m => m.status === "TAMAMLANDI").length;
            const stDuty = StorageManager.getDutyForStudentAndDate(st.id, today);
            const prayersDone = [stDuty.fajr, stDuty.dhuhr, stDuty.asr, stDuty.maghrib, stDuty.isha].filter(Boolean).length;
            return `
              <div class="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full flex items-center justify-center font-black text-sm text-white bg-gradient-to-tr from-[#0D1B2A] to-[#1E293B] border border-amber-500/40">
                    ${st.fullName.split(' ').map(n => n[0]).join('')}
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-slate-900 dark:text-white">${st.fullName}</h4>
                    <p class="text-xs text-slate-500 dark:text-slate-400">${st.grade} • ${stCompleted} Ezber Tamamlandı</p>
                  </div>
                </div>
                <div class="text-right">
                  <span class="inline-block px-2 py-0.5 rounded text-[11px] font-bold ${prayersDone === 5 ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300' : 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'}">
                    ${prayersDone}/5 Namaz
                  </span>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// 2. DERS PROGRAMI & CANLI YOKLAMA EKRANI
// =========================================================================
function renderScheduleScreen(schedule, students, attendance) {
  const selectedDay = AppState.selectedScheduleDay;
  const selectedDate = AppState.selectedDate;
  const activeStudents = students.filter(s => s.status === "Aktif");
  
  // Günlük dersler
  const dayLessons = schedule.filter(l => l.day === selectedDay).sort((a, b) => a.startTime.localeCompare(b.startTime));
  
  // Günlük yoklama istatistikleri
  const dayAtt = attendance.filter(a => a.date === selectedDate);
  const presentCount = dayAtt.filter(a => a.status === "GELDI").length;
  const absentCount = dayAtt.filter(a => a.status === "GELMEDI").length;
  const excusedCount = dayAtt.filter(a => a.status === "IZINLI").length;
  const lateCount = dayAtt.filter(a => a.status === "GEC").length;
  const attPercent = activeStudents.length > 0 ? Math.round((presentCount / activeStudents.length) * 100) : 0;

  const days = [
    { key: "PAZARTESI", short: "Pt", name: "Pazartesi" },
    { key: "SALI", short: "Sa", name: "Salı" },
    { key: "CARSAMBA", short: "Ça", name: "Çarşamba" },
    { key: "PERSEMBE", short: "Pe", name: "Perşembe" },
    { key: "CUMA", short: "Cu", name: "Cuma" },
    { key: "CUMARTESI", short: "Ct", name: "Cumartesi" },
    { key: "PAZAR", short: "Pz", name: "Pazar" }
  ];

  return `
    <div class="space-y-6 pb-20 max-w-6xl mx-auto">
      <!-- Üst Başlık & Tarih Seçici -->
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 tezhip-card p-4">
        <div class="flex items-center gap-3">
          <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
            <i class="fas fa-arrow-left text-sm"></i>
          </button>
          <div>
            <h1 class="text-xl font-black text-slate-900 dark:text-white">Ders Programı & Canlı Yoklama</h1>
            <p class="text-xs text-slate-500">${dayLessons.length} Ders Planlandı • %${attPercent} Günlük Katılım</p>
          </div>
        </div>

        <!-- Tarih & Yeni Ders Ekle Butonları -->
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <input type="date" value="${selectedDate}" onchange="changeSelectedDate(this.value)" class="px-3 py-2 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200">
          <button onclick="openNewLessonModal()" class="px-3.5 py-2 text-xs font-bold rounded-xl bg-indigo-900 text-amber-300 hover:bg-indigo-800 transition flex items-center gap-1.5">
            <i class="fas fa-plus"></i>
            <span>Ders Ekle</span>
          </button>
        </div>
      </div>

      <!-- Gün Seçici Sekme Çubuğu -->
      <div class="grid grid-cols-7 gap-2 bg-slate-100 dark:bg-slate-900 p-1.5 rounded-2xl border border-slate-200 dark:border-slate-800">
        ${days.map(d => {
          const isSelected = d.key === selectedDay;
          const count = schedule.filter(l => l.day === d.key).length;
          return `
            <button onclick="selectScheduleDay('${d.key}')" class="py-2.5 rounded-xl text-center transition flex flex-col items-center justify-center ${isSelected ? 'bg-[#0D1B2A] text-amber-400 shadow-md font-black' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-800'}">
              <span class="text-xs font-bold">${d.short}</span>
              <span class="text-[10px] opacity-80">${count} Ders</span>
            </button>
          `;
        }).join('')}
      </div>

      <!-- Günlük Özet & Hızlı Toplu Yoklama Kartı -->
      <div class="tezhip-card p-5">
        <div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-base font-black text-slate-900 dark:text-white">${days.find(d => d.key === selectedDay)?.name} Yoklama Özeti</h2>
              <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">Tarih: ${selectedDate}</span>
            </div>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">${presentCount} Geldi • ${absentCount} Gelmedi • ${excusedCount} İzinli • ${lateCount} Geç</p>
          </div>

          <!-- Hızlı Toplu İşlem Butonları -->
          <div class="flex items-center gap-2 flex-wrap">
            <button onclick="markAllClassAttendance('GELDI')" class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
              <i class="fas fa-check-double"></i>
              <span>Tüm Sınıf Geldi</span>
            </button>
            <button onclick="markAllClassAttendance('IZINLI')" class="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
              <i class="fas fa-clock"></i>
              <span>İzinli Yaz</span>
            </button>
            <button onclick="markAllClassAttendance('GELMEDI')" class="px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
              <i class="fas fa-xmark"></i>
              <span>Gelmedi</span>
            </button>
          </div>
        </div>

        <!-- Katılım İlerleme Çubuğu -->
        <div class="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full mt-4 overflow-hidden">
          <div class="bg-gradient-to-r from-emerald-500 to-amber-400 h-full rounded-full transition-all duration-500" style="width: ${attPercent}%"></div>
        </div>
      </div>

      <!-- Ders Listesi & İnteraktif Yoklama Kartları -->
      <div class="space-y-4">
        ${dayLessons.length === 0 ? `
          <div class="tezhip-card p-12 text-center">
            <div class="seljuk-star w-16 h-16 text-3xl mx-auto mb-3 bg-slate-100 dark:bg-slate-800 text-slate-400"><i class="fas fa-calendar-xmark"></i></div>
            <h3 class="text-base font-bold text-slate-800 dark:text-slate-200">Bu gün için planlanmış ders bulunamadı.</h3>
            <p class="text-xs text-slate-400 mt-1">Yeni bir ders ekleyebilir veya varsayılan programı yükleyebilirsiniz.</p>
            <button onclick="openNewLessonModal()" class="mt-4 px-4 py-2 bg-indigo-900 text-amber-300 text-xs font-bold rounded-xl hover:bg-indigo-800">Ders Ekle</button>
          </div>
        ` : dayLessons.map(lesson => {
          const targetStudents = lesson.targetGrade === "Tüm Sınıflar" 
            ? activeStudents 
            : activeStudents.filter(s => s.grade.includes(lesson.targetGrade) || lesson.targetGrade.includes(s.grade));

          return `
            <div class="tezhip-card p-5">
              <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
                <div class="flex items-center gap-3">
                  <div class="px-3 py-1.5 rounded-xl bg-[#0D1B2A] text-amber-300 font-mono text-xs font-black flex items-center gap-1.5">
                    <i class="fas fa-clock text-[10px]"></i>
                    <span>${lesson.startTime} - ${lesson.endTime}</span>
                  </div>
                  <div>
                    <h3 class="text-base font-black text-slate-900 dark:text-white">${lesson.title}</h3>
                    <p class="text-xs text-slate-500 dark:text-slate-400">${lesson.classroom} • Eğitmen: ${lesson.teacherName} • Hedef: <span class="font-bold text-indigo-600 dark:text-indigo-400">${lesson.targetGrade}</span></p>
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  <button onclick="markLessonAttendanceAll('${lesson.id}', 'GELDI')" class="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300 hover:bg-emerald-200">
                    <i class="fas fa-check-double mr-1"></i> Dersi Geldi Yap
                  </button>
                  <button onclick="deleteLesson('${lesson.id}')" class="p-1.5 text-slate-400 hover:text-rose-500"><i class="fas fa-trash-alt text-xs"></i></button>
                </div>
              </div>

              <!-- Talebe Yoklama Butonları Listesi -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-2.5 mt-4">
                ${targetStudents.map(st => {
                  const record = attendance.find(a => a.studentId === st.id && a.date === selectedDate);
                  const currentStatus = record ? record.status : "YOK";

                  return `
                    <div class="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 flex items-center justify-between gap-2">
                      <div class="flex items-center gap-2.5 min-w-0">
                        <div class="w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs text-white bg-slate-800 flex-shrink-0">
                          ${st.fullName.split(' ').map(n => n[0]).join('')}
                        </div>
                        <span class="text-xs font-bold text-slate-900 dark:text-white truncate">${st.fullName}</span>
                      </div>

                      <div class="flex items-center gap-1 flex-shrink-0">
                        <button onclick="setStudentAttendance(${st.id}, 'GELDI')" class="px-2 py-1 rounded text-[11px] font-bold transition ${currentStatus === 'GELDI' ? 'bg-emerald-600 text-white shadow' : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-emerald-100'}">Geldi</button>
                        <button onclick="setStudentAttendance(${st.id}, 'IZINLI')" class="px-2 py-1 rounded text-[11px] font-bold transition ${currentStatus === 'IZINLI' ? 'bg-amber-600 text-white shadow' : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-amber-100'}">İzinli</button>
                        <button onclick="setStudentAttendance(${st.id}, 'GEC')" class="px-2 py-1 rounded text-[11px] font-bold transition ${currentStatus === 'GEC' ? 'bg-cyan-600 text-white shadow' : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-cyan-100'}">Geç</button>
                        <button onclick="setStudentAttendance(${st.id}, 'GELMEDI')" class="px-2 py-1 rounded text-[11px] font-bold transition ${currentStatus === 'GELMEDI' ? 'bg-rose-600 text-white shadow' : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-rose-100'}">Gelmedi</button>
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

// =========================================================================
// 3. GÜNLÜK GÖREVLER (VAZİFE & VİRD ÇETELESİ) EKRANI
// =========================================================================
function renderDutiesScreen(students, duties) {
  const selectedDate = AppState.selectedDate;
  const activeStudents = students.filter(s => s.status === "Aktif");
  const todayDuties = duties.filter(d => d.date === selectedDate);

  const totalFullPrayers = todayDuties.filter(d => d.fajr && d.dhuhr && d.asr && d.maghrib && d.isha).length;
  const totalQuranPages = todayDuties.reduce((acc, d) => acc + (d.quranPages || 0), 0);
  const totalRisalePages = todayDuties.reduce((acc, d) => acc + (d.risalePages || 0), 0);
  const totalSalavat = todayDuties.reduce((acc, d) => acc + (d.salavatCount || 0), 0);

  return `
    <div class="space-y-6 pb-20 max-w-6xl mx-auto">
      <!-- Üst Başlık & Bilgilendirme Şeridi -->
      <div class="tezhip-card p-4">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
              <i class="fas fa-arrow-left text-sm"></i>
            </button>
            <div>
              <h1 class="text-xl font-black text-slate-900 dark:text-white">Günlük Görevler (Vird Çetelesi)</h1>
              <p class="text-xs text-slate-500">${activeStudents.length} Talebe • ${totalFullPrayers} Tam 5 Vakit • ${totalQuranPages} Syf Kur'an • ${totalRisalePages} Syf Risale</p>
            </div>
          </div>

          <div class="flex items-center gap-2 w-full sm:w-auto">
            <button onclick="navigateTo('SCHEDULE')" class="px-3.5 py-2 text-xs font-bold rounded-xl bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
              <i class="fas fa-calendar-check"></i>
              <span>Yoklama Ekranına Geç ➔</span>
            </button>
            <input type="date" value="${selectedDate}" onchange="changeSelectedDate(this.value)" class="px-3 py-2 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200">
          </div>
        </div>
      </div>

      <!-- Sınıf Genel Vird Bento Paneli -->
      <div class="tezhip-card p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <div class="seljuk-star w-7 h-7 text-xs bg-emerald-600 text-white"><i class="fas fa-hands-praying"></i></div>
            <h3 class="text-sm font-bold text-slate-900 dark:text-white">Sınıf Günlük Vird Göstergeleri</h3>
          </div>
          <span class="text-xs font-bold text-emerald-600">${totalFullPrayers}/${activeStudents.length} Tam Vakit</span>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div class="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-500/20 text-center">
            <div class="text-[11px] text-slate-500 dark:text-slate-400">5 Vakit Namaz</div>
            <div class="text-lg font-black text-emerald-600 dark:text-emerald-400">${totalFullPrayers} Talebe</div>
          </div>
          <div class="p-3 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-500/20 text-center">
            <div class="text-[11px] text-slate-500 dark:text-slate-400">Kur'an-ı Kerim</div>
            <div class="text-lg font-black text-blue-600 dark:text-blue-400">${totalQuranPages} Sayfa</div>
          </div>
          <div class="p-3 rounded-xl bg-purple-50 dark:bg-purple-950/40 border border-purple-500/20 text-center">
            <div class="text-[11px] text-slate-500 dark:text-slate-400">Risale-i Nur</div>
            <div class="text-lg font-black text-purple-600 dark:text-purple-400">${totalRisalePages} Sayfa</div>
          </div>
          <div class="p-3 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-500/20 text-center">
            <div class="text-[11px] text-slate-500 dark:text-slate-400">Salavat-ı Şerife</div>
            <div class="text-lg font-black text-amber-600 dark:text-amber-400">${totalSalavat} Adet</div>
          </div>
        </div>

        <!-- Hızlı Toplu Vird Butonları -->
        <div class="flex items-center gap-2 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex-wrap">
          <button onclick="markAllPrayersToday(true)" class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
            <i class="fas fa-check-double"></i> Tüm Sınıf 5 Vakit
          </button>
          <button onclick="adjustAllQuranPages(1)" class="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
            +1 Syf Kur'an
          </button>
          <button onclick="adjustAllRisalePages(5)" class="px-3 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold flex items-center gap-1.5 shadow">
            +5 Syf Risale
          </button>
        </div>
      </div>

      <!-- Talebe Başına Görev ve Vird Kartları -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        ${activeStudents.map(st => {
          const duty = StorageManager.getDutyForStudentAndDate(st.id, selectedDate);
          
          let score = 0;
          if (duty.fajr) score += 15;
          if (duty.dhuhr) score += 15;
          if (duty.asr) score += 15;
          if (duty.maghrib) score += 15;
          if (duty.isha) score += 15;
          if (duty.tesbihatDone) score += 10;
          if (duty.quranPages > 0) score += 10;
          if (duty.risalePages > 0) score += 5;
          score = Math.min(score, 100);

          return `
            <div class="tezhip-card p-4 space-y-3">
              <!-- Başlık & Puan -->
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs text-white bg-slate-800">
                    ${st.fullName.split(' ').map(n => n[0]).join('')}
                  </div>
                  <div>
                    <h3 class="text-sm font-bold text-slate-900 dark:text-white">${st.fullName}</h3>
                    <p class="text-xs text-slate-500 dark:text-slate-400">${st.grade}</p>
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 rounded text-xs font-bold ${score >= 80 ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}">%${score} Vird</span>
                  <button onclick="openDutyNoteModal(${st.id})" class="text-slate-400 hover:text-amber-500 p-1"><i class="fas fa-edit text-xs"></i></button>
                </div>
              </div>

              <!-- 5 Vakit Namaz Butonları -->
              <div>
                <div class="text-[11px] font-semibold text-slate-400 mb-1">5 Vakit Namaz Takibi</div>
                <div class="grid grid-cols-5 gap-1.5">
                  <button onclick="toggleStudentPrayer(${st.id}, 'fajr')" class="prayer-pill ${duty.fajr ? 'checked' : ''}">Sabah</button>
                  <button onclick="toggleStudentPrayer(${st.id}, 'dhuhr')" class="prayer-pill ${duty.dhuhr ? 'checked' : ''}">Öğle</button>
                  <button onclick="toggleStudentPrayer(${st.id}, 'asr')" class="prayer-pill ${duty.asr ? 'checked' : ''}">İkindi</button>
                  <button onclick="toggleStudentPrayer(${st.id}, 'maghrib')" class="prayer-pill ${duty.maghrib ? 'checked' : ''}">Akşam</button>
                  <button onclick="toggleStudentPrayer(${st.id}, 'isha')" class="prayer-pill ${duty.isha ? 'checked' : ''}">Yatsı</button>
                </div>
              </div>

              <!-- Tesbihat & Cevşen & Sayaçlar -->
              <div class="grid grid-cols-2 gap-2 pt-1">
                <button onclick="toggleStudentTesbihat(${st.id})" class="px-2.5 py-1.5 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 ${duty.tesbihatDone ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-500 text-emerald-700 dark:text-emerald-300' : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'}">
                  <i class="fas ${duty.tesbihatDone ? 'fa-check-circle text-emerald-500' : 'fa-circle-notch text-slate-400'}"></i>
                  <span>Tesbihat</span>
                </button>
                <button onclick="toggleStudentCevsen(${st.id})" class="px-2.5 py-1.5 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 ${duty.cevsenDone ? 'bg-amber-50 dark:bg-amber-950/40 border-amber-500 text-amber-700 dark:text-amber-300' : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'}">
                  <i class="fas ${duty.cevsenDone ? 'fa-sparkles text-amber-500' : 'fa-circle-notch text-slate-400'}"></i>
                  <span>Cevşen</span>
                </button>
              </div>

              <!-- Kur'an / Risale / Salavat Sayaçları -->
              <div class="grid grid-cols-3 gap-2 pt-1 border-t border-slate-100 dark:border-slate-800">
                <div class="flex items-center justify-between p-1.5 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800">
                  <button onclick="adjustStudentQuran(${st.id}, -1)" class="stepper-btn">-</button>
                  <div class="text-center"><span class="text-[10px] text-slate-400 block">Kur'an</span><span class="text-xs font-bold text-blue-600">${duty.quranPages} Syf</span></div>
                  <button onclick="adjustStudentQuran(${st.id}, 1)" class="stepper-btn">+</button>
                </div>

                <div class="flex items-center justify-between p-1.5 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800">
                  <button onclick="adjustStudentRisale(${st.id}, -5)" class="stepper-btn">-</button>
                  <div class="text-center"><span class="text-[10px] text-slate-400 block">Risale</span><span class="text-xs font-bold text-purple-600">${duty.risalePages} Syf</span></div>
                  <button onclick="adjustStudentRisale(${st.id}, 5)" class="stepper-btn">+</button>
                </div>

                <div class="flex items-center justify-between p-1.5 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800">
                  <button onclick="adjustStudentSalavat(${st.id}, -100)" class="stepper-btn">-</button>
                  <div class="text-center"><span class="text-[10px] text-slate-400 block">Salavat</span><span class="text-xs font-bold text-amber-600">${duty.salavatCount}</span></div>
                  <button onclick="adjustStudentSalavat(${st.id}, 100)" class="stepper-btn">+</button>
                </div>
              </div>

              ${duty.notes ? `
                <div class="text-xs bg-amber-50/60 dark:bg-amber-950/30 border border-amber-500/20 p-2 rounded-lg text-slate-700 dark:text-slate-300">
                  <i class="fas fa-comment-dots text-amber-500 mr-1"></i> ${duty.notes}
                </div>
              ` : ''}
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

// =========================================================================
// 4. EZBER & MÜFREDAT YÖNETİMİ EKRANI
// =========================================================================
function renderMemorizationScreen(students, memorization) {
  const completedList = memorization.filter(m => m.status === "TAMAMLANDI");
  const inProgressList = memorization.filter(m => m.status === "DEVAM_EDIYOR");

  return `
    <div class="space-y-6 pb-20 max-w-6xl mx-auto">
      <div class="tezhip-card p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
            <i class="fas fa-arrow-left text-sm"></i>
          </button>
          <div>
            <h1 class="text-xl font-black text-slate-900 dark:text-white">Ezber & Müfredat Takibi</h1>
            <p class="text-xs text-slate-500">${memorization.length} Toplam Kayıt • ${completedList.length} Tamamlanan • ${inProgressList.length} Devam Eden</p>
          </div>
        </div>

        <button onclick="openAssignMemorizationModal()" class="px-4 py-2 bg-gradient-to-r from-purple-800 to-indigo-900 text-amber-300 text-xs font-bold rounded-xl shadow hover:opacity-95 transition flex items-center gap-1.5">
          <i class="fas fa-plus"></i>
          <span>Yeni Ezber Tayin Et</span>
        </button>
      </div>

      <!-- Devam Eden Ezberler -->
      <div class="tezhip-card p-5">
        <div class="flex items-center gap-2 mb-4">
          <div class="seljuk-star w-7 h-7 text-xs bg-purple-700 text-white"><i class="fas fa-hourglass-half"></i></div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Devam Eden Ezberler (${inProgressList.length})</h3>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          ${inProgressList.map(rec => {
            const st = students.find(s => s.id === rec.studentId) || { fullName: "Talebe" };
            return `
              <div class="p-4 rounded-xl border border-purple-500/30 bg-purple-50/20 dark:bg-purple-950/20 space-y-2">
                <div class="flex items-center justify-between">
                  <div>
                    <span class="text-xs font-bold text-purple-600 dark:text-purple-400 uppercase tracking-wider">${rec.category}</span>
                    <h4 class="text-sm font-black text-slate-900 dark:text-white">${rec.title}</h4>
                    <p class="text-xs text-slate-500">Talebe: <span class="font-bold text-slate-800 dark:text-slate-200">${st.fullName}</span></p>
                  </div>
                  <button onclick="openEvaluateMemorizationModal(${rec.id})" class="px-3 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow">
                    Dinle & Puanla ⭐
                  </button>
                </div>
                ${rec.teacherNotes ? `<p class="text-xs text-slate-500 italic bg-white/60 dark:bg-slate-900/60 p-2 rounded">${rec.teacherNotes}</p>` : ''}
              </div>
            `;
          }).join('')}
        </div>
      </div>

      <!-- Tamamlanan Ezberler Arşivi -->
      <div class="tezhip-card p-5">
        <div class="flex items-center gap-2 mb-4">
          <div class="seljuk-star w-7 h-7 text-xs bg-emerald-600 text-white"><i class="fas fa-check"></i></div>
          <h3 class="text-base font-bold text-slate-900 dark:text-white">Tamamlanan Ezberler (${completedList.length})</h3>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          ${completedList.map(rec => {
            const st = students.find(s => s.id === rec.studentId) || { fullName: "Talebe" };
            return `
              <div class="p-3.5 rounded-xl border border-emerald-500/20 bg-emerald-50/10 dark:bg-emerald-950/10 flex items-center justify-between">
                <div>
                  <div class="flex items-center gap-2">
                    <h4 class="text-sm font-bold text-slate-900 dark:text-white">${rec.title}</h4>
                    <span class="text-amber-500 text-xs">★ ${rec.rating || 5}</span>
                  </div>
                  <p class="text-xs text-slate-500">Talebe: ${st.fullName} • ${rec.completedDate || 'Tamamlandı'}</p>
                </div>
                <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">Onaylandı ✓</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// 5. TALEBELER YÖNETİMİ EKRANI
// =========================================================================
function renderStudentsScreen(students, memorization, attendance, duties) {
  return `
    <div class="space-y-6 pb-20 max-w-6xl mx-auto">
      <div class="tezhip-card p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
            <i class="fas fa-arrow-left text-sm"></i>
          </button>
          <div>
            <h1 class="text-xl font-black text-slate-900 dark:text-white">Talebeler Yönetimi</h1>
            <p class="text-xs text-slate-500">${students.length} Kayıtlı Talebe</p>
          </div>
        </div>

        <button onclick="openNewStudentModal()" class="px-4 py-2 bg-blue-900 text-amber-300 text-xs font-bold rounded-xl shadow hover:bg-blue-800 transition flex items-center gap-1.5">
          <i class="fas fa-user-plus"></i>
          <span>Yeni Talebe Ekle</span>
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        ${students.map(st => {
          const stMem = memorization.filter(m => m.studentId === st.id);
          const completedCount = stMem.filter(m => m.status === "TAMAMLANDI").length;
          const activeCount = stMem.filter(m => m.status === "DEVAM_EDIYOR").length;

          return `
            <div class="tezhip-card p-5 space-y-4">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-12 h-12 rounded-full flex items-center justify-center font-black text-sm text-white bg-gradient-to-tr from-[#0D1B2A] to-[#1E293B] border border-amber-500/40">
                    ${st.fullName.split(' ').map(n => n[0]).join('')}
                  </div>
                  <div>
                    <h3 class="text-base font-black text-slate-900 dark:text-white">${st.fullName}</h3>
                    <p class="text-xs text-slate-500">${st.grade} • No: ${st.studentNumber || st.id}</p>
                  </div>
                </div>

                <div class="flex items-center gap-1">
                  <button onclick="openEditStudentModal(${st.id})" class="text-slate-400 hover:text-amber-500 p-1.5 transition" title="Talebe Bilgilerini & Giriş Kodunu Düzenle">
                    <i class="fas fa-user-pen text-sm"></i>
                  </button>
                  <button onclick="deleteStudent(${st.id})" class="text-slate-400 hover:text-rose-500 p-1.5 transition" title="Talebeliği Kaldır">
                    <i class="fas fa-trash-alt text-sm"></i>
                  </button>
                </div>
              </div>

              <!-- Yönetici Tarafından Verilen Giriş & Erişim Bilgileri -->
              <div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 space-y-2 text-xs">
                <div class="flex items-center justify-between">
                  <span class="text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                    <i class="fas fa-user text-[11px] text-slate-400"></i> Kullanıcı Adı:
                  </span>
                  <span class="font-bold text-slate-800 dark:text-slate-200 font-mono">${st.username || 'talebe' + st.id}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                    <i class="fas fa-lock text-[11px] text-slate-400"></i> Şifre:
                  </span>
                  <span class="font-bold text-slate-800 dark:text-slate-200 font-mono">${st.pin || '1234'}</span>
                </div>
                <div class="flex items-center justify-between pt-1.5 border-t border-slate-200 dark:border-slate-800">
                  <span class="font-bold text-amber-700 dark:text-amber-400 flex items-center gap-1.5">
                    <i class="fas fa-key text-[11px]"></i> Giriş Kodu:
                  </span>
                  <div class="flex items-center gap-1.5">
                    <span class="px-2 py-0.5 rounded font-mono font-black text-xs bg-amber-100 dark:bg-amber-950/70 text-amber-900 dark:text-amber-300 border border-amber-500/40">
                      ${st.accessCode || 'IRF-' + st.id}
                    </span>
                    <button onclick="copyToClipboard('${st.accessCode || 'IRF-' + st.id}', '${st.fullName} giriş kodu kopyalandı!')" class="p-1 text-slate-400 hover:text-amber-500 transition" title="Kodu Kopyala">
                      <i class="fas fa-copy text-xs"></i>
                    </button>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-2 text-center pt-1">
                <div class="p-2 bg-slate-50 dark:bg-slate-900 rounded-lg">
                  <div class="text-[10px] text-slate-400">Tamamlanan</div>
                  <div class="text-sm font-bold text-emerald-600">${completedCount} Ezber</div>
                </div>
                <div class="p-2 bg-slate-50 dark:bg-slate-900 rounded-lg">
                  <div class="text-[10px] text-slate-400">Aktif Ders</div>
                  <div class="text-sm font-bold text-purple-600">${activeCount} Ezber</div>
                </div>
              </div>

              <div class="flex items-center gap-2">
                <button onclick="openAssignMemorizationModal(${st.id})" class="flex-1 py-2 text-xs font-bold rounded-lg bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100">Ezber Ver</button>
                <button onclick="viewStudentReport(${st.id})" class="flex-1 py-2 text-xs font-bold rounded-lg bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 hover:bg-amber-100">Karne Gör</button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

// =========================================================================
// 6. RESMİ KARNELER & RAPORLAR EKRANI
// =========================================================================
function renderReportsScreen(students, memorization, attendance, duties) {
  const selectedStudentId = AppState.modalData?.reportStudentId || students[0]?.id;
  const student = students.find(s => s.id === selectedStudentId) || students[0];

  const stMem = memorization.filter(m => m.studentId === student.id);
  const completedMem = stMem.filter(m => m.status === "TAMAMLANDI");

  return `
    <div class="space-y-6 pb-20 max-w-5xl mx-auto">
      <div class="tezhip-card p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 no-print">
        <div class="flex items-center gap-3">
          <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
            <i class="fas fa-arrow-left text-sm"></i>
          </button>
          <div>
            <h1 class="text-xl font-black text-slate-900 dark:text-white">Talebe Gelişim Karnesi</h1>
            <p class="text-xs text-slate-500">Resmi karne ve gelişim çıktısı</p>
          </div>
        </div>

        <div class="flex items-center gap-2 w-full sm:w-auto">
          <select onchange="changeReportStudent(this.value)" class="px-3 py-2 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800">
            ${students.map(s => `<option value="${s.id}" ${s.id === student.id ? 'selected' : ''}>${s.fullName} (${s.grade})</option>`).join('')}
          </select>
          <button onclick="window.print()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow flex items-center gap-1.5">
            <i class="fas fa-print"></i>
            <span>Yazdır / PDF</span>
          </button>
        </div>
      </div>

      <!-- RESMİ YAZDIRILABİLİR KARNE ŞABLONU -->
      <div class="tezhip-card p-8 bg-white text-slate-900 border-2 border-amber-500/40 shadow-xl print-area">
        <!-- Başlık -->
        <div class="text-center pb-6 border-b-2 border-amber-500/30">
          <div class="seljuk-star gold w-12 h-12 text-xl mx-auto mb-2"><i class="fas fa-mosque"></i></div>
          <h2 class="text-xs font-bold text-amber-700 tracking-widest uppercase">MEKTEB-İ İRFAN MEDRESESİ</h2>
          <h1 class="text-2xl font-black text-slate-900">TALEBE EZBER VE VİRD TAKİP KARNESİ</h1>
          <p class="text-xs text-slate-500 mt-1">Dönem: 2025 - 2026 Eğitim Öğretim Yılı</p>
        </div>

        <!-- Talebe Bilgileri -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 py-6 border-b border-slate-200 text-sm">
          <div><span class="text-xs text-slate-400 block">Talebe Adı Soyadı:</span><strong class="text-base">${student.fullName}</strong></div>
          <div><span class="text-xs text-slate-400 block">Sınıf / Halka:</span><strong>${student.grade}</strong></div>
          <div><span class="text-xs text-slate-400 block">Öğrenci No:</span><strong>${student.studentNumber || student.id}</strong></div>
          <div><span class="text-xs text-slate-400 block">Kayıt Tarihi:</span><strong>${student.joinDate || '2025'}</strong></div>
        </div>

        <!-- Tamamlanan Ezberler Tablosu -->
        <div class="py-6 space-y-3">
          <h3 class="text-sm font-black text-slate-900 flex items-center gap-2">
            <i class="fas fa-quran text-amber-600"></i> Teslim Edilen ve Onaylanan Ezberler
          </h3>
          <table class="w-full text-left text-xs border border-slate-200">
            <thead class="bg-slate-100 border-b border-slate-200">
              <tr>
                <th class="p-2.5">Ezber Başlığı</th>
                <th class="p-2.5">Kategori</th>
                <th class="p-2.5">Puan</th>
                <th class="p-2.5">Tarih</th>
                <th class="p-2.5">Hoca Değerlendirmesi</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200">
              ${completedMem.length === 0 ? `
                <tr><td colspan="5" class="p-4 text-center text-slate-400">Henüz tamamlanmış ezber kaydı bulunmamaktadır.</td></tr>
              ` : completedMem.map(m => `
                <tr>
                  <td class="p-2.5 font-bold">${m.title}</td>
                  <td class="p-2.5">${m.category}</td>
                  <td class="p-2.5 font-bold text-amber-600">${'★'.repeat(m.rating || 5)}</td>
                  <td class="p-2.5">${m.completedDate || '-'}</td>
                  <td class="p-2.5 italic text-slate-600">${m.teacherNotes || 'Muvaffak oldu.'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <!-- İmza Alanı -->
        <div class="grid grid-cols-2 gap-8 pt-12 text-center text-xs">
          <div>
            <div class="font-bold">Ezber Hocası</div>
            <div class="mt-8 pt-2 border-t border-slate-400 w-36 mx-auto">İmza</div>
          </div>
          <div>
            <div class="font-bold">Kurum Müdürü / Hoca</div>
            <div class="mt-8 pt-2 border-t border-slate-400 w-36 mx-auto">Mühür / İmza</div>
          </div>
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// 7. AYARLAR VE VERİ YEDEKLEME
// =========================================================================
function renderSettingsScreen() {
  const currentSchoolCode = StorageManager.getSchoolCode();

  return `
    <div class="space-y-6 pb-20 max-w-4xl mx-auto">
      <div class="tezhip-card p-4 flex items-center gap-3">
        <button onclick="navigateTo('MAIN_MENU')" class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-white transition">
          <i class="fas fa-arrow-left text-sm"></i>
        </button>
        <div>
          <h1 class="text-xl font-black text-slate-900 dark:text-white">Ayarlar ve Veri Yönetimi</h1>
          <p class="text-xs text-slate-500">Kurum kodu, talebe erişim ayarları & veri yedekleme</p>
        </div>
      </div>

      <!-- Kurum ve Genel Giriş Kodu Yönetimi -->
      <div class="tezhip-card p-6 space-y-4">
        <div class="flex items-center gap-3">
          <div class="seljuk-star gold w-10 h-10 text-lg"><i class="fas fa-key"></i></div>
          <div>
            <h3 class="text-base font-bold text-slate-900 dark:text-white">Genel Kurum / Medrese Giriş Kodu</h3>
            <p class="text-xs text-slate-500">Talebelerin sisteme giriş yaparken kullanabileceği genel kurum kodunu belirleyin.</p>
          </div>
        </div>

        <div class="flex flex-col sm:flex-row items-start sm:items-center gap-3 pt-2">
          <div class="relative w-full sm:w-72">
            <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-amber-600">
              <i class="fas fa-shield-halved text-xs"></i>
            </span>
            <input id="settings-school-code" type="text" value="${currentSchoolCode}" class="w-full pl-9 pr-3 py-2 text-sm font-mono font-bold rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white uppercase">
          </div>
          <button onclick="updateSchoolCodeFromSettings()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl shadow transition flex items-center gap-2">
            <i class="fas fa-floppy-disk"></i>
            <span>Kodu Kaydet</span>
          </button>
          <button onclick="generateSchoolCodeInput()" class="px-3 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-amber-100 dark:hover:bg-amber-950/40 text-slate-700 dark:text-slate-200 text-xs font-bold rounded-xl border border-slate-300 dark:border-slate-700 transition flex items-center gap-1.5">
            <i class="fas fa-dice"></i>
            <span>Yeni Kod Üret</span>
          </button>
        </div>
        <p class="text-[11px] text-slate-400">
          Not: Talebeler hem kendilerine özel verilen giriş koduyla (Örn: <code>IRF-101</code>) hem de bu genel kurum koduyla giriş yapabilirler.
        </p>
      </div>

      <!-- Veri Yedekleme & Geri Yükleme -->
      <div class="tezhip-card p-6 space-y-5">
        <h3 class="text-base font-bold text-slate-900 dark:text-white">Veri Yedekleme & Geri Yükleme</h3>
        <p class="text-xs text-slate-500">Tüm talebe, ezber, ders programı ve yoklama verilerinizi JSON dosyası olarak bilgisayarınıza indirebilir veya yükleyebilirsiniz.</p>
        
        <div class="flex items-center gap-3 flex-wrap">
          <button onclick="exportDataJSON()" class="px-4 py-2.5 bg-[#0D1B2A] text-amber-300 font-bold text-xs rounded-xl shadow hover:bg-slate-800 flex items-center gap-2">
            <i class="fas fa-download"></i>
            <span>Verileri JSON Olarak İndir</span>
          </button>
          
          <label class="px-4 py-2.5 bg-emerald-700 text-white font-bold text-xs rounded-xl shadow hover:bg-emerald-600 cursor-pointer flex items-center gap-2">
            <i class="fas fa-upload"></i>
            <span>Yedeği Geri Yükle</span>
            <input type="file" accept=".json" onchange="importDataJSON(event)" class="hidden">
          </label>
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// 8. TALEBE PORTALI (ÖĞRENCİ KENDİ PANELİ VE GİRİŞ EKRANI)
// =========================================================================
function renderStudentPortalScreen(students, memorization, duties) {
  // Eğer talebe giriş yapmamışsa, Talebe Giriş Formunu göster
  if (!AppState.studentSession || !AppState.studentSession.isLoggedIn || !AppState.studentSession.student) {
    return renderStudentLoginView(students);
  }

  // Giriş yapmış talebenin bilgileri
  const currentStudent = AppState.studentSession.student;
  const stMem = memorization.filter(m => m.studentId === currentStudent.id);
  const activeMem = stMem.filter(m => m.status === "DEVAM_EDIYOR");
  const completedMem = stMem.filter(m => m.status === "TAMAMLANDI");
  const todayDuty = StorageManager.getDutyForStudentAndDate(currentStudent.id, AppState.selectedDate);

  return `
    <div class="space-y-6 pb-20 max-w-5xl mx-auto">
      <!-- Talebe Portalı Üst Banner -->
      <div class="tezhip-card p-6 bg-gradient-to-r from-emerald-900 via-[#0D1B2A] to-emerald-950 text-white relative">
        <div class="ornament-corner-tr"></div>
        <div class="ornament-corner-bl"></div>

        <div class="flex flex-col sm:flex-row items-center justify-between gap-4 relative z-10">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-full flex items-center justify-center font-black text-xl text-white bg-gradient-to-tr from-amber-400 to-amber-600 shadow-lg border-2 border-amber-300">
              ${currentStudent.fullName.split(' ').map(n => n[0]).join('')}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-amber-300 font-bold uppercase tracking-wider">Talebe Portalı</span>
                <span class="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-amber-400/20 text-amber-300 border border-amber-400/30">
                  <i class="fas fa-key mr-1"></i>Kod: ${currentStudent.accessCode || 'IRF-' + currentStudent.id}
                </span>
              </div>
              <h1 class="text-2xl font-black text-white">${currentStudent.fullName}</h1>
              <p class="text-xs text-emerald-200">${currentStudent.grade} • Kullanıcı: @${currentStudent.username || 'talebe' + currentStudent.id} • ${completedMem.length} Tamamlanan Ezber</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button onclick="logoutStudentPortal()" class="px-3.5 py-2 rounded-xl bg-rose-600/80 hover:bg-rose-600 text-white font-bold text-xs shadow transition flex items-center gap-1.5">
              <i class="fas fa-right-from-bracket"></i>
              <span>Oturumu Kapat</span>
            </button>
            <button onclick="navigateTo('MAIN_MENU')" class="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs border border-white/20 transition flex items-center gap-1.5">
              <i class="fas fa-chalkboard-user text-amber-300"></i>
              <span>Eğitmen Paneli</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Talebe Aktif Ezberleri -->
      <div class="tezhip-card p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <div class="seljuk-star purple w-7 h-7 text-xs text-white"><i class="fas fa-book-quran"></i></div>
            <h3 class="text-base font-black text-slate-900 dark:text-white">Devam Eden Ezberlerin (${activeMem.length})</h3>
          </div>
          <span class="text-xs font-bold text-purple-600 dark:text-purple-400">${completedMem.length} Ezber Teslim Edildi</span>
        </div>

        <div class="space-y-3">
          ${activeMem.length === 0 ? `
            <div class="p-6 text-center text-slate-400 bg-slate-50 dark:bg-slate-900/40 rounded-xl border border-dashed border-slate-200 dark:border-slate-800">
              <i class="fas fa-check-circle text-2xl text-emerald-500 mb-2"></i>
              <p class="text-xs">Şu an devam eden bir ezberiniz bulunmamaktadır. Hocanızdan yeni ezber talep edebilirsiniz.</p>
            </div>
          ` : activeMem.map(m => `
            <div class="p-4 rounded-xl border border-purple-500/30 bg-purple-50/20 dark:bg-purple-950/20 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <span class="text-[11px] font-bold text-purple-600 dark:text-purple-400 uppercase tracking-wider">${m.category}</span>
                <h4 class="text-base font-black text-slate-900 dark:text-white">${m.title}</h4>
                <p class="text-xs text-slate-500 mt-1">Hoca Değerlendirmesi: <span class="italic text-slate-700 dark:text-slate-300 font-semibold">${m.teacherNotes || 'Çalışmaya ve tekrara devam ediniz.'}</span></p>
              </div>
              <div class="flex items-center gap-2">
                <span class="px-3 py-1 rounded-full text-xs font-bold bg-purple-100 text-purple-800 dark:bg-purple-900/50 dark:text-purple-300">
                  <i class="fas fa-spinner fa-spin mr-1 text-[10px]"></i> Çalışılıyor
                </span>
              </div>
            </div>
          `).join('')}
        </div>
      </div>

      <!-- Tamamlanan Ezberler ve Alınan Yıldızlar -->
      <div class="tezhip-card p-5">
        <div class="flex items-center gap-2 mb-3">
          <div class="seljuk-star gold w-7 h-7 text-xs"><i class="fas fa-award"></i></div>
          <h3 class="text-base font-black text-slate-900 dark:text-white">Tamamlanan ve Onaylanan Ezberler (${completedMem.length})</h3>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          ${completedMem.length === 0 ? `
            <div class="col-span-2 p-4 text-center text-slate-400 text-xs">Henüz onaylanmış ezber bulunmuyor.</div>
          ` : completedMem.map(m => `
            <div class="p-3.5 rounded-xl border border-emerald-500/30 bg-emerald-50/10 dark:bg-emerald-950/20 flex items-center justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <h4 class="text-sm font-bold text-slate-900 dark:text-white">${m.title}</h4>
                  <span class="text-amber-500 text-xs font-bold">${'★'.repeat(m.rating || 5)}</span>
                </div>
                <p class="text-[11px] text-slate-500">${m.category} • ${m.completedDate || 'Onaylandı'}</p>
              </div>
              <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-300">
                Kabul Edildi ✓
              </span>
            </div>
          `).join('')}
        </div>
      </div>

      <!-- Talebe Günlük Vazifeleri -->
      <div class="tezhip-card p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <div class="seljuk-star emerald w-7 h-7 text-xs text-white"><i class="fas fa-hands-praying"></i></div>
            <h3 class="text-base font-black text-slate-900 dark:text-white">Bugünkü Vazifelerin (${AppState.selectedDate})</h3>
          </div>
          <span class="text-xs text-slate-400">Namaz & Vird Durumu</span>
        </div>

        <div class="grid grid-cols-5 gap-2 text-center">
          ${['Sabah', 'Öğle', 'İkindi', 'Akşam', 'Yatsı'].map((p, idx) => {
            const key = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'][idx];
            const isDone = todayDuty[key];
            return `
              <div class="p-3 rounded-xl border ${isDone ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-500 text-emerald-700 dark:text-emerald-300 font-bold' : 'border-slate-200 dark:border-slate-800 text-slate-400'}">
                <i class="fas ${isDone ? 'fa-check text-emerald-500' : 'fa-circle-xmark text-slate-300'} block mb-1"></i>
                <span class="text-xs">${p}</span>
              </div>
            `;
          }).join('')}
        </div>

        <div class="grid grid-cols-3 gap-3 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 text-center">
          <div class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900">
            <span class="text-[11px] text-slate-400 block">Kur'an Tilaveti</span>
            <span class="text-sm font-bold text-blue-600">${todayDuty.quranPages || 0} Sayfa</span>
          </div>
          <div class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900">
            <span class="text-[11px] text-slate-400 block">Risale Okuması</span>
            <span class="text-sm font-bold text-purple-600">${todayDuty.risalePages || 0} Sayfa</span>
          </div>
          <div class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900">
            <span class="text-[11px] text-slate-400 block">Salavat-ı Şerife</span>
            <span class="text-sm font-bold text-amber-600">${todayDuty.salavatCount || 0} Adet</span>
          </div>
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// TALEBE GİRİŞ EKRANI (STUDENT LOGIN VIEW)
// =========================================================================
function renderStudentLoginView(students) {
  const activeStudents = students.filter(s => s.status === "Aktif");

  return `
    <div class="max-w-md mx-auto my-6 sm:my-10 space-y-6 pb-16">
      <!-- 1. Üst Tezhip Başlık Kartı -->
      <div class="tezhip-card p-6 sm:p-8 bg-gradient-to-b from-[#0D1B2A] to-[#1E293B] text-white text-center relative overflow-hidden shadow-2xl border-2 border-amber-500/40">
        <div class="ornament-corner-tr"></div>
        <div class="ornament-corner-bl"></div>

        <div class="seljuk-star gold w-16 h-16 text-3xl mx-auto mb-3 shadow-xl">
          <i class="fas fa-graduation-cap text-[#0D1B2A]"></i>
        </div>
        <span class="text-xs font-black tracking-widest text-amber-400 uppercase">MEKTEB-İ İRFAN</span>
        <h2 class="text-2xl font-black text-white mt-1">Talebe Giriş Portalı</h2>
        <p class="text-xs text-slate-300 mt-1">Ezber ve Günlük Vazife Takip Sisteminize Erişin</p>
      </div>

      <!-- 2. Giriş Formu Kartı -->
      <div class="tezhip-card p-6 space-y-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl">
        <!-- Hata Bildirimi -->
        <div id="student-login-error" class="hidden p-3 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-400/40 text-rose-700 dark:text-rose-300 text-xs font-bold flex items-center gap-2">
          <i class="fas fa-triangle-exclamation text-sm flex-shrink-0"></i>
          <span id="student-login-error-text">Hatalı bilgi</span>
        </div>

        <form onsubmit="event.preventDefault(); handleStudentLoginSubmit();" class="space-y-4">
          <!-- 1. Kullanıcı Adı veya Öğrenci No -->
          <div>
            <label class="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">
              Kullanıcı Adı veya Talebe No
            </label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <i class="fas fa-user text-xs"></i>
              </span>
              <input id="student-login-username" type="text" placeholder="Örn: ahmet101 veya 101" required autocomplete="username"
                class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-sm font-semibold focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
            </div>
          </div>

          <!-- 2. Şifre -->
          <div>
            <label class="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">
              Şifre
            </label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <i class="fas fa-lock text-xs"></i>
              </span>
              <input id="student-login-password" type="password" placeholder="Şifrenizi giriniz (Varsayılan: 1234)" required autocomplete="current-password"
                class="w-full pl-9 pr-10 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-sm font-semibold focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
              <button type="button" onclick="togglePasswordVisibility('student-login-password', 'student-login-eye')" class="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                <i id="student-login-eye" class="fas fa-eye text-xs"></i>
              </button>
            </div>
          </div>

          <!-- 3. Yönetici / Hoca Tarafından Verilen Giriş Kodu -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <i class="fas fa-key text-amber-600 text-xs"></i>
                <span>Giriş Kodu (Erişim Kodu)</span>
              </label>
              <span class="text-[10px] text-amber-700 dark:text-amber-400 font-bold bg-amber-50 dark:bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-500/30">
                Yönetici Tarafından Verilen
              </span>
            </div>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-amber-600">
                <i class="fas fa-shield-halved text-xs"></i>
              </span>
              <input id="student-login-code" type="text" placeholder="Örn: IRF-101 veya irfan_2026" required
                class="w-full pl-9 pr-3 py-2.5 rounded-xl border border-amber-500/40 bg-amber-50/20 dark:bg-amber-950/20 text-slate-900 dark:text-white text-sm font-mono font-bold uppercase focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none transition">
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
              💡 Her öğrencinin sisteme girebilmesi için medrese yöneticisi / hocası tarafından verilen giriş kodu girilmelidir.
            </p>
          </div>

          <!-- Giriş Yap Butonu -->
          <button type="submit" class="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-emerald-600 via-emerald-500 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-black text-sm shadow-lg shadow-emerald-900/30 flex items-center justify-center gap-2 transition transform active:scale-98">
            <i class="fas fa-arrow-right-to-bracket"></i>
            <span>Sisteme Giriş Yap</span>
          </button>
        </form>

        <!-- Hızlı Demo Talebe Doldurucusu -->
        <div class="pt-4 border-t border-slate-200 dark:border-slate-800">
          <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-2 text-center">
            Demo / Test Talebeleri (Tek Tıkla Doldur):
          </span>
          <div class="grid grid-cols-2 gap-2">
            ${activeStudents.slice(0, 4).map(st => `
              <button type="button" onclick="fillStudentLoginForm('${st.username || 'talebe' + st.id}', '${st.pin || '1234'}', '${st.accessCode || 'IRF-' + st.id}')"
                class="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/80 hover:bg-amber-50 dark:hover:bg-amber-950/40 border border-slate-200 dark:border-slate-700 text-left transition group">
                <div class="text-xs font-bold text-slate-800 dark:text-slate-200 group-hover:text-amber-600 truncate">${st.fullName}</div>
                <div class="text-[10px] text-slate-500 font-mono mt-0.5">Kod: <span class="font-bold text-amber-600">${st.accessCode || 'IRF-' + st.id}</span></div>
              </button>
            `).join('')}
          </div>
        </div>

        <!-- Eğitmen Paneline Dön -->
        <div class="text-center pt-2">
          <button type="button" onclick="navigateTo('MAIN_MENU')" class="text-xs font-bold text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 transition flex items-center justify-center gap-1.5 mx-auto">
            <i class="fas fa-arrow-left text-[10px]"></i>
            <span>Eğitmen / Yönetici Paneline Dön</span>
          </button>
        </div>
      </div>
    </div>
  `;
}

// =========================================================================
// MODAL PENCERELERİ VE DİYALOGLAR
// =========================================================================
function openNewLessonModal() {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <h3 class="text-lg font-black text-slate-900 dark:text-white">Yeni Ders Programı Ekle</h3>
      
      <div class="space-y-3 text-sm">
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Ders Başlığı</label>
          <input id="modal-lesson-title" type="text" placeholder="Örn: Kur'an-ı Kerim Ezber Dersi" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Gün</label>
            <select id="modal-lesson-day" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
              <option value="PAZARTESI">Pazartesi</option>
              <option value="SALI">Salı</option>
              <option value="CARSAMBA">Çarşamba</option>
              <option value="PERSEMBE">Perşembe</option>
              <option value="CUMA">Cuma</option>
              <option value="CUMARTESI">Cumartesi</option>
              <option value="PAZAR">Pazar</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Hedef Sınıf</label>
            <select id="modal-lesson-grade" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
              <option value="Tüm Sınıflar">Tüm Sınıflar</option>
              <option value="5. Sınıf">5. Sınıf</option>
              <option value="6. Sınıf">6. Sınıf</option>
              <option value="7. Sınıf">7. Sınıf</option>
              <option value="8. Sınıf">8. Sınıf</option>
              <option value="Hafızlık Grubu">Hafızlık Grubu</option>
            </select>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Başlangıç Saati</label>
            <input id="modal-lesson-start" type="time" value="09:00" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Bitiş Saati</label>
            <input id="modal-lesson-end" type="time" value="10:30" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Eğitmen</label>
            <input id="modal-lesson-teacher" type="text" value="Ahmet Hoca" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Derslik</label>
            <input id="modal-lesson-room" type="text" value="Hafızlık Salonu 1" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
          </div>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveNewLessonFromModal()" class="px-4 py-2 bg-indigo-900 text-amber-300 text-xs font-bold rounded-xl">Kaydet</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function openNewStudentModal() {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  const students = StorageManager.getStudents();
  const nextNo = students.length > 0 ? Math.max(...students.map(s => parseInt(s.studentNumber || s.id) || 100)) + 1 : 101;
  const generatedCode = "IRF-" + nextNo;

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <div class="flex items-center gap-3">
        <div class="seljuk-star gold w-10 h-10 text-lg"><i class="fas fa-user-plus"></i></div>
        <div>
          <h3 class="text-lg font-black text-slate-900 dark:text-white">Yeni Talebe Kaydı</h3>
          <p class="text-xs text-slate-500">Talebe bilgileri, şifre ve yönetici giriş kodu tanımlayın</p>
        </div>
      </div>
      
      <div class="space-y-3 text-sm">
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Ad Soyad *</label>
          <input id="modal-student-name" type="text" placeholder="Örn: Bedirhan Demir" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white">
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Sınıf / Halka</label>
            <select id="modal-student-grade" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white">
              <option value="5. Sınıf">5. Sınıf</option>
              <option value="6. Sınıf">6. Sınıf</option>
              <option value="7. Sınıf">7. Sınıf</option>
              <option value="8. Sınıf">8. Sınıf</option>
              <option value="Hafızlık Grubu">Hafızlık Grubu</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Öğrenci No</label>
            <input id="modal-student-no" type="text" value="${nextNo}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white">
          </div>
        </div>

        <!-- Kullanıcı Adı ve Şifre -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Kullanıcı Adı</label>
            <input id="modal-student-username" type="text" value="talebe${nextNo}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-mono text-xs">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Şifre</label>
            <input id="modal-student-pin" type="text" value="1234" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-mono text-xs">
          </div>
        </div>

        <!-- Yönetici Tarafından Verilen Giriş Kodu -->
        <div class="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-950/20 border border-amber-500/30">
          <div class="flex items-center justify-between mb-1">
            <label class="text-xs font-bold text-amber-900 dark:text-amber-300 flex items-center gap-1.5">
              <i class="fas fa-key text-xs"></i> Yönetici Giriş Kodu (Erişim Kodu)
            </label>
            <button type="button" onclick="generateRandomAccessCode('modal-student-code')" class="text-[11px] font-bold text-amber-700 hover:text-amber-600 flex items-center gap-1">
              <i class="fas fa-dice"></i> Kod Üret
            </button>
          </div>
          <input id="modal-student-code" type="text" value="${generatedCode}" class="w-full p-2.5 rounded-xl border border-amber-500/40 bg-white dark:bg-slate-900 text-slate-900 dark:text-white font-mono font-bold uppercase text-sm">
          <p class="text-[10.5px] text-slate-500 dark:text-slate-400 mt-1">
            Talebe sisteme girerken kullanıcı adı ve şifresinin yanında bu kodu girmek zorundadır.
          </p>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveNewStudentFromModal()" class="px-4 py-2 bg-blue-900 text-amber-300 text-xs font-bold rounded-xl shadow hover:bg-blue-800 transition">Kaydet</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function openEditStudentModal(studentId) {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  const students = StorageManager.getStudents();
  const student = students.find(s => s.id === studentId);
  if (!student) return;

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <div class="flex items-center gap-3">
        <div class="seljuk-star gold w-10 h-10 text-lg"><i class="fas fa-user-pen"></i></div>
        <div>
          <h3 class="text-lg font-black text-slate-900 dark:text-white">Talebe Bilgilerini Düzenle</h3>
          <p class="text-xs text-slate-500">${student.fullName} • No: ${student.studentNumber || student.id}</p>
        </div>
      </div>
      
      <div class="space-y-3 text-sm">
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Ad Soyad</label>
          <input id="modal-edit-name" type="text" value="${student.fullName}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-semibold">
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Sınıf / Halka</label>
            <select id="modal-edit-grade" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white">
              <option value="5. Sınıf" ${student.grade === '5. Sınıf' ? 'selected' : ''}>5. Sınıf</option>
              <option value="6. Sınıf" ${student.grade === '6. Sınıf' ? 'selected' : ''}>6. Sınıf</option>
              <option value="7. Sınıf" ${student.grade === '7. Sınıf' ? 'selected' : ''}>7. Sınıf</option>
              <option value="8. Sınıf" ${student.grade === '8. Sınıf' ? 'selected' : ''}>8. Sınıf</option>
              <option value="Hafızlık Grubu" ${student.grade === 'Hafızlık Grubu' ? 'selected' : ''}>Hafızlık Grubu</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Öğrenci No</label>
            <input id="modal-edit-no" type="text" value="${student.studentNumber || student.id}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white">
          </div>
        </div>

        <!-- Kullanıcı Adı ve Şifre -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Kullanıcı Adı</label>
            <input id="modal-edit-username" type="text" value="${student.username || 'talebe' + student.id}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-mono text-xs">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 block mb-1">Şifre</label>
            <input id="modal-edit-pin" type="text" value="${student.pin || '1234'}" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-mono text-xs">
          </div>
        </div>

        <!-- Yönetici Tarafından Verilen Giriş Kodu -->
        <div class="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-950/20 border border-amber-500/30">
          <div class="flex items-center justify-between mb-1">
            <label class="text-xs font-bold text-amber-900 dark:text-amber-300 flex items-center gap-1.5">
              <i class="fas fa-key text-xs"></i> Yönetici Giriş Kodu (Erişim Kodu)
            </label>
            <button type="button" onclick="generateRandomAccessCode('modal-edit-code')" class="text-[11px] font-bold text-amber-700 hover:text-amber-600 flex items-center gap-1">
              <i class="fas fa-dice"></i> Yeni Kod Üret
            </button>
          </div>
          <input id="modal-edit-code" type="text" value="${student.accessCode || 'IRF-' + student.id}" class="w-full p-2.5 rounded-xl border border-amber-500/40 bg-white dark:bg-slate-900 text-slate-900 dark:text-white font-mono font-bold uppercase text-sm">
          <p class="text-[10.5px] text-slate-500 dark:text-slate-400 mt-1">
            Talebe giriş ekranında bu kodu kullanarak sisteme dahil olacaktır.
          </p>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Hoca Değerlendirme & Notu</label>
          <textarea id="modal-edit-notes" rows="2" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs">${student.notes || ''}</textarea>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveEditStudentFromModal(${student.id})" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow transition">Güncelle</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function openAssignMemorizationModal(preSelectedStudentId = null) {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  const students = StorageManager.getStudents();

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <h3 class="text-lg font-black text-slate-900 dark:text-white">Yeni Ezber Tayin Et</h3>
      
      <div class="space-y-3 text-sm">
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Talebe Seçiniz</label>
          <select id="modal-assign-student" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
            ${students.map(s => `<option value="${s.id}" ${s.id === preSelectedStudentId ? 'selected' : ''}>${s.fullName} (${s.grade})</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Müfredat Kategorisi</label>
          <select id="modal-assign-category" onchange="updateAssignItemOptions(this.value)" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
            <option value="Kur'an">Kur'an-ı Kerim Sûreleri & Aşırları</option>
            <option value="Tesbihat">Namaz Tesbihatı & Dualar</option>
            <option value="Risale">Risale-i Nur Ezberleri</option>
          </select>
        </div>
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Ezber Başlığı</label>
          <select id="modal-assign-title" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
            ${CURRICULUM_DATA.kuran.map(t => `<option value="${t}">${t}</option>`).join('')}
          </select>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveAssignMemorizationFromModal()" class="px-4 py-2 bg-purple-800 text-amber-300 text-xs font-bold rounded-xl">Ezber Tayin Et</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function updateAssignItemOptions(category) {
  const select = document.getElementById("modal-assign-title");
  if (!select) return;
  const items = category === "Tesbihat" ? CURRICULUM_DATA.tesbihat : category === "Risale" ? CURRICULUM_DATA.risale : CURRICULUM_DATA.kuran;
  select.innerHTML = items.map(t => `<option value="${t}">${t}</option>`).join('');
}

function openEvaluateMemorizationModal(recordId) {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  const records = StorageManager.getMemorization();
  const record = records.find(r => r.id === recordId);
  if (!record) return;

  const students = StorageManager.getStudents();
  const student = students.find(s => s.id === record.studentId) || { fullName: "Talebe" };

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <div class="flex items-center gap-3">
        <div class="seljuk-star gold w-10 h-10 text-lg"><i class="fas fa-star"></i></div>
        <div>
          <h3 class="text-lg font-black text-slate-900 dark:text-white">Ezber Dinleme & Puanlama</h3>
          <p class="text-xs text-slate-500">Talebe: <strong class="text-slate-800 dark:text-slate-200">${student.fullName}</strong> • Ezber: <strong>${record.title}</strong></p>
        </div>
      </div>
      
      <div class="space-y-3 text-sm">
        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Yıldız Puanı (1 - 5)</label>
          <select id="modal-eval-rating" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 font-bold text-amber-500">
            <option value="5">⭐⭐⭐⭐⭐ (5 Yıldız - Mükemmel)</option>
            <option value="4">⭐⭐⭐⭐ (4 Yıldız - Çok İyi)</option>
            <option value="3">⭐⭐⭐ (3 Yıldız - Orta / Tekrar Edilebilir)</option>
            <option value="2">⭐⭐ (2 Yıldız - Zayıf)</option>
            <option value="1">⭐ (1 Yıldız - Tekrar Edilecek)</option>
          </select>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Durum</label>
          <select id="modal-eval-status" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 font-bold text-emerald-600">
            <option value="TAMAMLANDI">Tamamlandı (Ezber Kabul Edildi ✓)</option>
            <option value="DEVAM_EDIYOR">Devam Ediyor (Tekrar Çalışılacak)</option>
          </select>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-500 block mb-1">Hoca Değerlendirme & Teşvik Notu</label>
          <textarea id="modal-eval-notes" rows="2" placeholder="Örn: Tecvidi ve mahreçleri kusursuz, tebrikler!" class="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs">${record.teacherNotes || ''}</textarea>
        </div>
      </div>

      <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-200 dark:border-slate-800">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveEvaluateMemorization(${record.id})" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow">Puanı Kaydet & Onayla</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function openDutyNoteModal(studentId) {
  const modal = document.getElementById("generic-modal");
  const modalContent = document.getElementById("generic-modal-body");
  if (!modal || !modalContent) return;

  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  const students = StorageManager.getStudents();
  const student = students.find(s => s.id === studentId);

  modalContent.innerHTML = `
    <div class="p-6 space-y-4">
      <h3 class="text-base font-bold text-slate-900 dark:text-white">${student.fullName} - Vazife Notu</h3>
      <textarea id="modal-duty-notes-input" rows="3" class="w-full p-3 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm" placeholder="Örn: Sabah namazı camide cemaatle kılındı.">${duty.notes || ''}</textarea>
      
      <div class="flex items-center justify-end gap-2">
        <button onclick="closeModal()" class="px-4 py-2 text-xs font-bold text-slate-500">İptal</button>
        <button onclick="saveDutyNoteFromModal(${studentId})" class="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-xl">Kaydet</button>
      </div>
    </div>
  `;
  modal.classList.remove("hidden");
}

function closeModal() {
  const modal = document.getElementById("generic-modal");
  if (modal) modal.classList.add("hidden");
}

// =========================================================================
// AKSİYONLAR & OLAY DİNLEYİCİLERİ
// =========================================================================
function changeSelectedDate(date) {
  AppState.selectedDate = date;
  renderApp();
}

function selectScheduleDay(day) {
  AppState.selectedScheduleDay = day;
  renderApp();
}

function setStudentAttendance(studentId, status) {
  StorageManager.setAttendance(studentId, AppState.selectedDate, status);
  showToast(`Yoklama güncellendi: ${status}`);
  renderApp();
}

function markAllClassAttendance(status) {
  const students = StorageManager.getStudents().filter(s => s.status === "Aktif");
  StorageManager.markAllAttendance(AppState.selectedDate, students, status);
  showToast(`Tüm aktif talebeler '${status}' olarak kaydedildi! 🌿`);
  renderApp();
}

function markLessonAttendanceAll(lessonId, status) {
  const schedule = StorageManager.getSchedule();
  const lesson = schedule.find(l => l.id === lessonId);
  if (!lesson) return;
  const students = StorageManager.getStudents().filter(s => s.status === "Aktif");
  const target = lesson.targetGrade === "Tüm Sınıflar" ? students : students.filter(s => s.grade.includes(lesson.targetGrade) || lesson.targetGrade.includes(s.grade));
  StorageManager.markAllAttendance(AppState.selectedDate, target, status);
  showToast(`${lesson.title} için ${target.length} talebe '${status}' yazıldı!`);
  renderApp();
}

function toggleStudentPrayer(studentId, prayerKey) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty[prayerKey] = !duty[prayerKey];
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function toggleStudentTesbihat(studentId) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.tesbihatDone = !duty.tesbihatDone;
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function toggleStudentCevsen(studentId) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.cevsenDone = !duty.cevsenDone;
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function adjustStudentQuran(studentId, delta) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.quranPages = Math.max(0, (duty.quranPages || 0) + delta);
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function adjustStudentRisale(studentId, delta) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.risalePages = Math.max(0, (duty.risalePages || 0) + delta);
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function adjustStudentSalavat(studentId, delta) {
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.salavatCount = Math.max(0, (duty.salavatCount || 0) + delta);
  StorageManager.saveDailyDuty(duty);
  renderApp();
}

function markAllPrayersToday(allDone) {
  const students = StorageManager.getStudents().filter(s => s.status === "Aktif");
  StorageManager.markAllPrayers(AppState.selectedDate, students, allDone);
  showToast("Tüm talebeler için 5 vakit namaz tamamlandı! 🌿");
  renderApp();
}

function adjustAllQuranPages(delta) {
  const students = StorageManager.getStudents().filter(s => s.status === "Aktif");
  students.forEach(st => {
    const d = StorageManager.getDutyForStudentAndDate(st.id, AppState.selectedDate);
    d.quranPages = Math.max(0, (d.quranPages || 0) + delta);
    StorageManager.saveDailyDuty(d);
  });
  showToast("Tüm talebelere Kur'an sayfası eklendi.");
  renderApp();
}

function adjustAllRisalePages(delta) {
  const students = StorageManager.getStudents().filter(s => s.status === "Aktif");
  students.forEach(st => {
    const d = StorageManager.getDutyForStudentAndDate(st.id, AppState.selectedDate);
    d.risalePages = Math.max(0, (d.risalePages || 0) + delta);
    StorageManager.saveDailyDuty(d);
  });
  showToast("Tüm talebelere Risale sayfası eklendi.");
  renderApp();
}

function saveDutyNoteFromModal(studentId) {
  const note = document.getElementById("modal-duty-notes-input")?.value || "";
  const duty = StorageManager.getDutyForStudentAndDate(studentId, AppState.selectedDate);
  duty.notes = note;
  StorageManager.saveDailyDuty(duty);
  closeModal();
  showToast("Vazife notu kaydedildi.");
  renderApp();
}

function saveNewLessonFromModal() {
  const title = document.getElementById("modal-lesson-title")?.value;
  const day = document.getElementById("modal-lesson-day")?.value;
  const grade = document.getElementById("modal-lesson-grade")?.value;
  const start = document.getElementById("modal-lesson-start")?.value;
  const end = document.getElementById("modal-lesson-end")?.value;
  const teacher = document.getElementById("modal-lesson-teacher")?.value;
  const room = document.getElementById("modal-lesson-room")?.value;

  if (!title) {
    alert("Lütfen ders başlığı giriniz.");
    return;
  }

  StorageManager.saveLesson({
    title,
    day,
    targetGrade: grade,
    startTime: start,
    endTime: end,
    teacherName: teacher,
    classroom: room,
    category: "KURAN"
  });

  closeModal();
  showToast("Yeni ders programı kaydedildi.");
  renderApp();
}

function deleteLesson(id) {
  if (confirm("Bu dersi programdan kaldırmak istediğinize emin misiniz?")) {
    StorageManager.deleteLesson(id);
    showToast("Ders kaldırıldı.");
    renderApp();
  }
}

function saveNewStudentFromModal() {
  const name = document.getElementById("modal-student-name")?.value;
  const grade = document.getElementById("modal-student-grade")?.value;
  const no = document.getElementById("modal-student-no")?.value;
  const username = document.getElementById("modal-student-username")?.value;
  const pin = document.getElementById("modal-student-pin")?.value;
  const accessCode = document.getElementById("modal-student-code")?.value;

  if (!name || !name.trim()) {
    alert("Lütfen talebe adını giriniz.");
    return;
  }

  StorageManager.addStudent({
    fullName: name.trim(),
    grade: grade || "5. Sınıf",
    studentNumber: no || "100",
    username: (username && username.trim()) || ("talebe" + (no || Date.now())),
    pin: (pin && pin.trim()) || "1234",
    accessCode: (accessCode && accessCode.trim().toUpperCase()) || ("IRF-" + (no || Date.now())),
    status: "Aktif",
    joinDate: new Date().toISOString().split('T')[0]
  });

  closeModal();
  showToast("Yeni talebe, şifre ve giriş kodu başarıyla kaydedildi! 🌿");
  renderApp();
}

function saveEditStudentFromModal(studentId) {
  const name = document.getElementById("modal-edit-name")?.value;
  const grade = document.getElementById("modal-edit-grade")?.value;
  const no = document.getElementById("modal-edit-no")?.value;
  const username = document.getElementById("modal-edit-username")?.value;
  const pin = document.getElementById("modal-edit-pin")?.value;
  const accessCode = document.getElementById("modal-edit-code")?.value;
  const notes = document.getElementById("modal-edit-notes")?.value;

  if (!name || !name.trim()) {
    alert("Lütfen talebe adını giriniz.");
    return;
  }

  const students = StorageManager.getStudents();
  const student = students.find(s => s.id === studentId);
  if (!student) return;

  const updatedStudent = {
    ...student,
    fullName: name.trim(),
    grade: grade || student.grade,
    studentNumber: no || student.studentNumber,
    username: (username && username.trim()) || student.username,
    pin: (pin && pin.trim()) || student.pin || "1234",
    accessCode: (accessCode && accessCode.trim().toUpperCase()) || student.accessCode || ("IRF-" + student.id),
    notes: notes || student.notes
  };

  StorageManager.updateStudent(updatedStudent);

  // Eğer şu an giriş yapan talebe bu öğrenciyse oturumu da güncelle
  if (AppState.studentSession?.student?.id === studentId) {
    AppState.studentSession.student = updatedStudent;
    StorageManager.saveStudentSession(updatedStudent);
  }

  closeModal();
  showToast("Talebe ve giriş bilgileri güncellendi! ✅");
  renderApp();
}

// Talebe Giriş Formu Gönderme (Login Submit Handler)
function handleStudentLoginSubmit() {
  const usernameInput = document.getElementById("student-login-username")?.value;
  const passwordInput = document.getElementById("student-login-password")?.value;
  const codeInput = document.getElementById("student-login-code")?.value;
  const errorBox = document.getElementById("student-login-error");
  const errorText = document.getElementById("student-login-error-text");

  if (errorBox) errorBox.classList.add("hidden");

  const validation = StorageManager.validateStudentLogin(usernameInput, passwordInput, codeInput);

  if (!validation.success) {
    if (errorBox && errorText) {
      errorText.textContent = validation.message;
      errorBox.classList.remove("hidden");
    } else {
      showToast(validation.message, "error");
    }
    return;
  }

  // Başarılı giriş
  const student = validation.student;
  AppState.studentSession = {
    isLoggedIn: true,
    student: student
  };
  StorageManager.saveStudentSession(student);

  showToast(`Hoş geldin, ${student.fullName}! Sisteme başarıyla giriş yapıldı. 🌟`, "success");
  triggerConfetti();
  renderApp();
}

// Hızlı Demo Talebe Doldurucu
function fillStudentLoginForm(username, pin, accessCode) {
  const uInput = document.getElementById("student-login-username");
  const pInput = document.getElementById("student-login-password");
  const cInput = document.getElementById("student-login-code");
  const errorBox = document.getElementById("student-login-error");

  if (uInput) uInput.value = username;
  if (pInput) pInput.value = pin;
  if (cInput) cInput.value = accessCode;
  if (errorBox) errorBox.classList.add("hidden");

  showToast(`${username} bilgileri ve kodu dolduruldu. 'Giriş Yap' butonuna basabilirsiniz.`);
}

// Şifre Göster/Gizle Butonu
function togglePasswordVisibility(inputId, eyeIconId) {
  const input = document.getElementById(inputId);
  const icon = document.getElementById(eyeIconId);
  if (!input) return;

  if (input.type === "password") {
    input.type = "text";
    if (icon) icon.className = "fas fa-eye-slash text-xs text-amber-500";
  } else {
    input.type = "password";
    if (icon) icon.className = "fas fa-eye text-xs text-slate-400";
  }
}

// Talebe Portalı Oturum Kapatma
function logoutStudentPortal() {
  AppState.studentSession = {
    isLoggedIn: false,
    student: null
  };
  StorageManager.clearStudentSession();
  showToast("Talebe oturumu güvenli şekilde kapatıldı.");
  renderApp();
}

// Rastgele Giriş Kodu Üretici
function generateRandomAccessCode(targetInputId) {
  const randNum = Math.floor(100 + Math.random() * 900);
  const code = "IRF-" + randNum;
  const input = document.getElementById(targetInputId);
  if (input) {
    input.value = code;
    showToast(`Yeni kod üretildi: ${code}`);
  }
  return code;
}

// Metin Kopyalama Yardımcısı
function copyToClipboard(text, message = "Kopyalandı!") {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => showToast(message)).catch(() => fallbackCopy(text, message));
  } else {
    fallbackCopy(text, message);
  }
}

function fallbackCopy(text, message) {
  const textArea = document.createElement("textarea");
  textArea.value = text;
  document.body.appendChild(textArea);
  textArea.select();
  document.execCommand("copy");
  document.body.removeChild(textArea);
  showToast(message);
}

// Ayarlardan Kurum Kodunu Güncelleme
function updateSchoolCodeFromSettings() {
  const input = document.getElementById("settings-school-code");
  const code = input ? input.value : "";
  if (!code.trim()) {
    alert("Lütfen geçerli bir kurum kodu giriniz.");
    return;
  }
  StorageManager.saveSchoolCode(code);
  showToast(`Genel medrese/kurum giriş kodu güncellendi: ${code.trim().toUpperCase()} ✅`);
  renderApp();
}

function generateSchoolCodeInput() {
  const input = document.getElementById("settings-school-code");
  if (input) {
    const newCode = "IRFAN_" + (Math.floor(1000 + Math.random() * 9000));
    input.value = newCode;
    showToast(`Yeni kurum kodu önerildi: ${newCode}`);
  }
}

function deleteStudent(id) {
  if (confirm("Bu talebeyi ve tüm kayıtlarını silmek istediğinize emin misiniz?")) {
    StorageManager.deleteStudent(id);
    showToast("Talebe silindi.");
    renderApp();
  }
}

function saveAssignMemorizationFromModal() {
  const studentId = parseInt(document.getElementById("modal-assign-student")?.value);
  const category = document.getElementById("modal-assign-category")?.value;
  const title = document.getElementById("modal-assign-title")?.value;

  StorageManager.saveMemorizationRecord({
    studentId,
    category,
    title,
    status: "DEVAM_EDIYOR",
    assignedDate: new Date().toISOString().split('T')[0],
    rating: 0
  });

  closeModal();
  showToast("Ezber başarıyla tayin edildi! 📖");
  renderApp();
}

function saveEvaluateMemorization(recordId) {
  const rating = parseInt(document.getElementById("modal-eval-rating")?.value || "5");
  const status = document.getElementById("modal-eval-status")?.value || "TAMAMLANDI";
  const notes = document.getElementById("modal-eval-notes")?.value || "";

  const records = StorageManager.getMemorization();
  const record = records.find(r => r.id === recordId);
  if (record) {
    record.rating = rating;
    record.status = status;
    record.teacherNotes = notes;
    if (status === "TAMAMLANDI") {
      record.completedDate = new Date().toISOString().split('T')[0];
      triggerConfetti();
    }
    StorageManager.saveMemorization(records);
  }

  closeModal();
  showToast(status === "TAMAMLANDI" ? "Ezber tamamlandı ve onaylandı! 🎉" : "Ezber puanı kaydedildi.");
  renderApp();
}

function viewStudentReport(studentId) {
  AppState.modalData = { reportStudentId: studentId };
  navigateTo("REPORTS");
}

function changeReportStudent(studentId) {
  AppState.modalData = { reportStudentId: parseInt(studentId) };
  renderApp();
}

function exportDataJSON() {
  const json = StorageManager.exportAllDataJSON();
  const blob = new Blob([json], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `mekteb_irfan_yedek_${new Date().toISOString().split('T')[0]}.json`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("Yedek dosyası indirildi.");
}

function importDataJSON(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const res = StorageManager.importAllDataJSON(e.target.result);
    if (res.success) {
      showToast("Yedek başarıyla geri yüklendi! ✅");
      renderApp();
    } else {
      alert("Yedek yüklenirken hata oluştu: " + res.error);
    }
  };
  reader.readAsText(file);
}

function setupGlobalListeners() {
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeModal();
  });
}

function attachDynamicEventListeners() {
  // Dinamik etkileşimler
}
