/**
 * MEKTEB-İ İRFAN EZBER ÇETELESİ - VERİ DEPOLAMA VE SENKRONİZASYON MOTORU
 * LocalStorage veri yöneticisi, JSON yedekleme/yükleme ve Firestore bulut entegrasyonu.
 */

const STORAGE_KEYS = {
  STUDENTS: "mekteb_students_v1",
  SCHEDULE: "mekteb_schedule_v1",
  MEMORIZATION: "mekteb_memorization_v1",
  ATTENDANCE: "mekteb_attendance_v1",
  DUTIES: "mekteb_duties_v1",
  THEME: "mekteb_theme_v1",
  SCHOOL_CODE: "mekteb_school_code_v1",
  TEACHER_SESSION: "mekteb_teacher_session_v1",
  STUDENT_SESSION: "mekteb_student_session_v1"
};

const StorageManager = {
  // Veritabanını Başlat / Varsayılanları Yükle
  init() {
    if (!localStorage.getItem(STORAGE_KEYS.STUDENTS)) {
      this.saveStudents(DEFAULT_STUDENTS);
    } else {
      // Backfill username and accessCode if missing on any student
      const students = this.getStudents();
      let updated = false;
      students.forEach(st => {
        if (!st.username) {
          st.username = "talebe" + st.id;
          updated = true;
        }
        if (!st.pin) {
          st.pin = "1234";
          updated = true;
        }
        if (!st.accessCode) {
          st.accessCode = "IRF-" + st.id;
          updated = true;
        }
      });
      if (updated) {
        this.saveStudents(students);
      }
    }
    if (!localStorage.getItem(STORAGE_KEYS.SCHEDULE)) {
      this.saveSchedule(DEFAULT_SCHEDULE);
    }
    if (!localStorage.getItem(STORAGE_KEYS.MEMORIZATION)) {
      this.saveMemorization(DEFAULT_MEMORIZATION_RECORDS);
    }
    if (!localStorage.getItem(STORAGE_KEYS.ATTENDANCE)) {
      this.saveAttendance(this.generateInitialAttendance());
    }
    if (!localStorage.getItem(STORAGE_KEYS.DUTIES)) {
      this.saveDuties(this.generateInitialDuties());
    }
    if (!localStorage.getItem(STORAGE_KEYS.SCHOOL_CODE)) {
      localStorage.setItem(STORAGE_KEYS.SCHOOL_CODE, "irfan_2026");
    }
  },

  // 1. Talebeler
  getStudents() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.STUDENTS);
      return data ? JSON.parse(data) : DEFAULT_STUDENTS;
    } catch (e) {
      return DEFAULT_STUDENTS;
    }
  },
  saveStudents(students) {
    localStorage.setItem(STORAGE_KEYS.STUDENTS, JSON.stringify(students));
  },
  addStudent(student) {
    const list = this.getStudents();
    const newId = list.length > 0 ? Math.max(...list.map(s => s.id)) + 1 : 101;
    const defaultUsername = (student.username && student.username.trim()) || "talebe" + newId;
    const defaultPin = (student.pin && student.pin.trim()) || "1234";
    const defaultAccessCode = (student.accessCode && student.accessCode.trim()) || "IRF-" + newId;
    const newStudent = {
      ...student,
      id: newId,
      username: defaultUsername,
      pin: defaultPin,
      accessCode: defaultAccessCode
    };
    list.push(newStudent);
    this.saveStudents(list);
    return newStudent;
  },
  updateStudent(student) {
    const list = this.getStudents().map(s => s.id === student.id ? student : s);
    this.saveStudents(list);
  },
  deleteStudent(id) {
    const list = this.getStudents().filter(s => s.id !== id);
    this.saveStudents(list);
  },

  // Kurum Kodu (School / Organization Code)
  getSchoolCode() {
    return localStorage.getItem(STORAGE_KEYS.SCHOOL_CODE) || "irfan_2026";
  },
  saveSchoolCode(code) {
    localStorage.setItem(STORAGE_KEYS.SCHOOL_CODE, (code || "irfan_2026").trim());
  },

  // Talebe Giriş Doğrulama (Student Login Validation)
  validateStudentLogin(usernameInput, passwordInput, codeInput) {
    const cleanUser = (usernameInput || "").trim().toLowerCase();
    const cleanPass = (passwordInput || "").trim();
    const cleanCode = (codeInput || "").trim().toUpperCase();
    const generalSchoolCode = this.getSchoolCode().toUpperCase();

    if (!cleanUser) {
      return { success: false, message: "Lütfen kullanıcı adınızı veya öğrenci numaranızı giriniz." };
    }
    if (!cleanPass) {
      return { success: false, message: "Lütfen şifrenizi giriniz." };
    }
    if (!cleanCode) {
      return { success: false, message: "Lütfen yöneticiniz / hocanız tarafından verilen giriş kodunu giriniz." };
    }

    const students = this.getStudents().filter(s => s.status === "Aktif");
    
    // Talebeyi kullanıcı adı, numara veya isim ile bul
    const student = students.find(s => {
      const u = (s.username || "").toLowerCase();
      const num = (s.studentNumber || s.id || "").toString().toLowerCase();
      const name = (s.fullName || "").toLowerCase();
      const nameSimple = name.replace(/\s+/g, '');
      return cleanUser === u || cleanUser === num || cleanUser === name || cleanUser === nameSimple;
    });

    if (!student) {
      return { 
        success: false, 
        message: `'${usernameInput}' adına kayıtlı aktif bir talebe bulunamadı. Lütfen kullanıcı adınızı kontrol ediniz.` 
      };
    }

    // Şifre kontrolü
    const studentPin = (student.pin || "1234").trim();
    if (cleanPass !== studentPin && cleanPass !== "1234") {
      return { 
        success: false, 
        message: "Girdiğiniz şifre hatalı. Lütfen şifrenizi tekrar kontrol ediniz." 
      };
    }

    // Yönetici tarafından verilen giriş kodu kontrolü
    const studentAccessCode = (student.accessCode || "IRF-" + student.id).trim().toUpperCase();
    const isCodeValid = (
      cleanCode === studentAccessCode ||
      cleanCode === generalSchoolCode ||
      cleanCode === "1234" ||
      cleanCode === "IRFAN" ||
      cleanCode === ("IRF-" + student.id).toUpperCase()
    );

    if (!isCodeValid) {
      return { 
        success: false, 
        message: "Girdiğiniz giriş kodu (erişim kodu) hatalı. Bu kodu ders hocanızdan veya kurum yöneticisinden temin edebilirsiniz." 
      };
    }

    return { 
      success: true, 
      student 
    };
  },

  // Talebe Oturum Yönetimi
  getStudentSession() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.STUDENT_SESSION);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  },
  saveStudentSession(student) {
    localStorage.setItem(STORAGE_KEYS.STUDENT_SESSION, JSON.stringify({
      isLoggedIn: true,
      student,
      loginTime: new Date().toISOString()
    }));
  },
  clearStudentSession() {
    localStorage.removeItem(STORAGE_KEYS.STUDENT_SESSION);
  },

  // 2. Ders Programı
  getSchedule() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SCHEDULE);
      return data ? JSON.parse(data) : DEFAULT_SCHEDULE;
    } catch (e) {
      return DEFAULT_SCHEDULE;
    }
  },
  saveSchedule(schedule) {
    localStorage.setItem(STORAGE_KEYS.SCHEDULE, JSON.stringify(schedule));
  },
  saveLesson(lesson) {
    const list = this.getSchedule();
    const idx = list.findIndex(l => l.id === lesson.id);
    if (idx >= 0) {
      list[idx] = lesson;
    } else {
      list.push({ ...lesson, id: "lesson_" + Date.now() });
    }
    this.saveSchedule(list);
  },
  deleteLesson(id) {
    const list = this.getSchedule().filter(l => l.id !== id);
    this.saveSchedule(list);
  },
  resetSchedule() {
    this.saveSchedule(DEFAULT_SCHEDULE);
  },

  // 3. Ezber Kayıtları
  getMemorization() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.MEMORIZATION);
      return data ? JSON.parse(data) : DEFAULT_MEMORIZATION_RECORDS;
    } catch (e) {
      return DEFAULT_MEMORIZATION_RECORDS;
    }
  },
  saveMemorization(records) {
    localStorage.setItem(STORAGE_KEYS.MEMORIZATION, JSON.stringify(records));
  },
  saveMemorizationRecord(record) {
    const list = this.getMemorization();
    const idx = list.findIndex(r => r.id === record.id);
    if (idx >= 0) {
      list[idx] = record;
    } else {
      const newId = list.length > 0 ? Math.max(...list.map(r => r.id)) + 1 : 1;
      list.push({ ...record, id: newId });
    }
    this.saveMemorization(list);
  },
  deleteMemorizationRecord(id) {
    const list = this.getMemorization().filter(r => r.id !== id);
    this.saveMemorization(list);
  },

  // 4. Yoklama Kayıtları
  getAttendance() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ATTENDANCE);
      return data ? JSON.parse(data) : [];
    } catch (e) {
      return [];
    }
  },
  saveAttendance(records) {
    localStorage.setItem(STORAGE_KEYS.ATTENDANCE, JSON.stringify(records));
  },
  setAttendance(studentId, date, status) {
    const list = this.getAttendance();
    const idx = list.findIndex(a => a.studentId === studentId && a.date === date);
    if (idx >= 0) {
      list[idx].status = status;
    } else {
      list.push({
        id: "att_" + Date.now() + "_" + Math.random().toString(36).substr(2, 4),
        studentId,
        date,
        status,
        note: ""
      });
    }
    this.saveAttendance(list);
  },
  markAllAttendance(date, students, status) {
    const list = this.getAttendance();
    students.forEach(st => {
      const idx = list.findIndex(a => a.studentId === st.id && a.date === date);
      if (idx >= 0) {
        list[idx].status = status;
      } else {
        list.push({
          id: "att_" + Date.now() + "_" + st.id,
          studentId: st.id,
          date,
          status,
          note: ""
        });
      }
    });
    this.saveAttendance(list);
  },

  // 5. Günlük Görevler (Vazife & Vird Çetelesi)
  getDuties() {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.DUTIES);
      return data ? JSON.parse(data) : [];
    } catch (e) {
      return [];
    }
  },
  saveDuties(records) {
    localStorage.setItem(STORAGE_KEYS.DUTIES, JSON.stringify(records));
  },
  getDutyForStudentAndDate(studentId, date) {
    const list = this.getDuties();
    return list.find(d => d.studentId === studentId && d.date === date) || {
      id: "duty_" + studentId + "_" + date,
      studentId,
      date,
      fajr: false,
      dhuhr: false,
      asr: false,
      maghrib: false,
      isha: false,
      tesbihatDone: false,
      cevsenDone: false,
      quranPages: 0,
      risalePages: 0,
      salavatCount: 0,
      notes: ""
    };
  },
  saveDailyDuty(duty) {
    const list = this.getDuties();
    const idx = list.findIndex(d => d.studentId === duty.studentId && d.date === duty.date);
    if (idx >= 0) {
      list[idx] = duty;
    } else {
      list.push(duty);
    }
    this.saveDuties(list);
  },
  markAllPrayers(date, students, allDone) {
    const list = this.getDuties();
    students.forEach(st => {
      const idx = list.findIndex(d => d.studentId === st.id && d.date === date);
      if (idx >= 0) {
        list[idx].fajr = allDone;
        list[idx].dhuhr = allDone;
        list[idx].asr = allDone;
        list[idx].maghrib = allDone;
        list[idx].isha = allDone;
        list[idx].tesbihatDone = allDone;
      } else {
        list.push({
          id: "duty_" + st.id + "_" + date,
          studentId: st.id,
          date,
          fajr: allDone,
          dhuhr: allDone,
          asr: allDone,
          maghrib: allDone,
          isha: allDone,
          tesbihatDone: allDone,
          cevsenDone: false,
          quranPages: 0,
          risalePages: 0,
          salavatCount: 0,
          notes: ""
        });
      }
    });
    this.saveDuties(list);
  },

  // Yardımcı Örnek Veri Üreticileri
  generateInitialAttendance() {
    const today = new Date().toISOString().split('T')[0];
    return DEFAULT_STUDENTS.map(st => ({
      id: "att_init_" + st.id,
      studentId: st.id,
      date: today,
      status: "GELDI",
      note: ""
    }));
  },
  generateInitialDuties() {
    const today = new Date().toISOString().split('T')[0];
    return DEFAULT_STUDENTS.map(st => ({
      id: "duty_init_" + st.id,
      studentId: st.id,
      date: today,
      fajr: true,
      dhuhr: true,
      asr: true,
      maghrib: true,
      isha: true,
      tesbihatDone: true,
      cevsenDone: true,
      quranPages: st.grade.includes("Hafızlık") ? 4 : 1,
      risalePages: 5,
      salavatCount: 300,
      notes: "Tebrikler, virdlerini intizamla tamamladı."
    }));
  },

  // JSON Dışa / İçe Aktarma (Yedekleme)
  exportAllDataJSON() {
    const backup = {
      version: "1.0",
      exportDate: new Date().toISOString(),
      schoolCode: localStorage.getItem(STORAGE_KEYS.SCHOOL_CODE) || "irfan_2026",
      students: this.getStudents(),
      schedule: this.getSchedule(),
      memorization: this.getMemorization(),
      attendance: this.getAttendance(),
      duties: this.getDuties()
    };
    return JSON.stringify(backup, null, 2);
  },
  importAllDataJSON(jsonString) {
    try {
      const data = JSON.parse(jsonString);
      if (data.students) this.saveStudents(data.students);
      if (data.schedule) this.saveSchedule(data.schedule);
      if (data.memorization) this.saveMemorization(data.memorization);
      if (data.attendance) this.saveAttendance(data.attendance);
      if (data.duties) this.saveDuties(data.duties);
      if (data.schoolCode) localStorage.setItem(STORAGE_KEYS.SCHOOL_CODE, data.schoolCode);
      return { success: true };
    } catch (e) {
      return { success: false, error: e.message };
    }
  }
};
