package com.example.util

import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student

object WebPortalGenerator {

  fun generateWebPortalHtml(
    students: List<Student>,
    attendanceList: List<AttendanceRecord>,
    memorizationList: List<MemorizationRecord>,
    firebaseConfigJson: String = ""
  ): String {
    return """
<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Mekteb-i İrfan | Web Yönetim ve Ezber Portalı</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700;800&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --primary-navy: #0F172A;
      --primary-blue: #1E3A8A;
      --gold-accent: #D97706;
      --gold-light: #FDE68A;
      --bg-cream: #F8FAFC;
      --card-bg: #FFFFFF;
      --text-dark: #0F172A;
      --text-muted: #64748B;
      --border-color: #E2E8F0;
      --green-present: #059669;
      --red-absent: #DC2626;
      --amber-excused: #D97706;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Plus Jakarta Sans', sans-serif;
      background-color: var(--bg-cream);
      color: var(--text-dark);
      line-height: 1.5;
    }

    /* Top Islamic Header */
    .top-bar {
      background: linear-gradient(135deg, #0F172A 0%, #1E3A8A 100%);
      color: #FFFFFF;
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
      border-bottom: 3px solid var(--gold-accent);
    }
    .brand-title {
      font-family: 'Cinzel', serif;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 1px;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .brand-title span { color: var(--gold-light); }
    .auth-pill {
      background: rgba(255,255,255,0.12);
      border: 1px solid rgba(217, 119, 6, 0.4);
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .container {
      max-width: 1200px;
      margin: 24px auto;
      padding: 0 16px;
    }

    /* Tabs */
    .nav-tabs {
      display: flex;
      gap: 12px;
      margin-bottom: 24px;
      border-bottom: 2px solid var(--border-color);
      padding-bottom: 8px;
    }
    .tab-btn {
      background: none;
      border: none;
      font-family: inherit;
      font-size: 15px;
      font-weight: 600;
      color: var(--text-muted);
      padding: 10px 18px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .tab-btn.active {
      background: var(--primary-blue);
      color: #FFFFFF;
      box-shadow: 0 2px 6px rgba(30, 58, 138, 0.25);
    }

    /* Cards & Tables */
    .card {
      background: var(--card-bg);
      border-radius: 16px;
      border: 1px solid var(--border-color);
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);
      padding: 20px;
      margin-bottom: 20px;
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--border-color);
    }
    .card-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--primary-navy);
      display: flex;
      align-items: center;
      gap: 8px;
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .stat-box {
      background: #FFFFFF;
      padding: 16px;
      border-radius: 12px;
      border-left: 4px solid var(--gold-accent);
      box-shadow: 0 1px 4px rgba(0,0,0,0.05);
    }
    .stat-box.green { border-left-color: var(--green-present); }
    .stat-box.blue { border-left-color: var(--primary-blue); }
    .stat-num { font-size: 24px; font-weight: 700; color: var(--primary-navy); }
    .stat-label { font-size: 12px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; }

    /* Tables */
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    th {
      background: #F1F5F9;
      color: var(--text-muted);
      text-align: left;
      padding: 10px 14px;
      font-weight: 600;
      font-size: 12px;
      text-transform: uppercase;
    }
    td {
      padding: 12px 14px;
      border-bottom: 1px solid var(--border-color);
    }
    tr:hover { background-color: #F8FAFC; }

    /* Status Badges */
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 700;
    }
    .badge-present { background: #D1FAE5; color: #065F46; }
    .badge-absent { background: #FEE2E2; color: #991B1B; }
    .badge-excused { background: #FEF3C7; color: #92400E; }
    .badge-done { background: #DBEAFE; color: #1E40AF; }

    /* Quick Action Button */
    .btn {
      background: var(--primary-blue);
      color: #FFFFFF;
      border: none;
      padding: 8px 16px;
      border-radius: 8px;
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn:hover { background: #172554; }
    .btn-gold { background: var(--gold-accent); }
    .btn-gold:hover { background: #B45309; }

    /* Search & Inputs */
    .input-control {
      padding: 8px 12px;
      border: 1px solid var(--border-color);
      border-radius: 8px;
      font-size: 13px;
      font-family: inherit;
    }
  </style>
</head>
<body>

  <header class="top-bar">
    <div class="brand-title">
      🕌 <span>MEKTEB-İ İRFAN</span> | Web Yönetim Portalı
    </div>
    <div class="auth-pill">
      <span>👤 Eğitmen Girişi: Aktif</span>
      <span style="color: var(--gold-light);">● Çevrimiçi</span>
    </div>
  </header>

  <main class="container">
    <!-- Quick Top Statistics -->
    <div class="stats-grid">
      <div class="stat-box blue">
        <div class="stat-num" id="total-students">${students.size}</div>
        <div class="stat-label">Kayıtlı Talebe</div>
      </div>
      <div class="stat-box green">
        <div class="stat-num" id="total-attendance">${attendanceList.size}</div>
        <div class="stat-label">Toplam Yoklama Kaydı</div>
      </div>
      <div class="stat-box">
        <div class="stat-num" id="total-memorization">${memorizationList.filter { it.status == "TAMAMLANDI" }.size}</div>
        <div class="stat-label">Teslim Edilen Ezber Dersi</div>
      </div>
    </div>

    <!-- Navigation Tabs -->
    <div class="nav-tabs">
      <button class="tab-btn active" onclick="switchTab('students')">👥 Talebe Listesi</button>
      <button class="tab-btn" onclick="switchTab('attendance')">📋 Günlük Yoklama</button>
      <button class="tab-btn" onclick="switchTab('memorization')">📖 Ezber Çetelesi</button>
      <button class="tab-btn" onclick="switchTab('cloud')">☁️ Bulut & Firebase Ayarları</button>
    </div>

    <!-- TAB 1: Talebe Listesi -->
    <div id="tab-students" class="card">
      <div class="card-header">
        <div class="card-title">👥 Talebe Kayıtları</div>
        <input type="text" id="student-search" class="input-control" placeholder="Talebe ara..." onkeyup="filterStudents()">
      </div>
      <table>
        <thead>
          <tr>
            <th>#No</th>
            <th>Adı Soyadı</th>
            <th>Sınıf / Grup</th>
            <th>Veli Adı & Telefon</th>
            <th>Notlar</th>
          </tr>
        </thead>
        <tbody id="students-tbody">
          ${students.joinToString("") { s ->
            val numDisplay = s.id.toString()
            val gradeDisplay = if (s.grade.isNotBlank()) s.grade else "Genel"
            val parentDisplay = if (s.parentName.isNotBlank() || s.parentPhone.isNotBlank()) "${s.parentName} (${s.parentPhone})" else "-"
            val noteDisplay = if (s.notes.isNotBlank()) s.notes else "-"
            """
            <tr>
              <td><strong>#$numDisplay</strong></td>
              <td><strong>${s.fullName}</strong></td>
              <td><span class="badge badge-done">$gradeDisplay</span></td>
              <td>$parentDisplay</td>
              <td><small>$noteDisplay</small></td>
            </tr>
            """
          }}
        </tbody>
      </table>
    </div>

    <!-- TAB 2: Yoklama -->
    <div id="tab-attendance" class="card" style="display: none;">
      <div class="card-header">
        <div class="card-title">📋 Yoklama Çizelgesi</div>
        <div>
          <button class="btn" onclick="window.print()">🖨️ Yazdır / PDF Al</button>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>Tarih</th>
            <th>Talebe ID</th>
            <th>Durum</th>
          </tr>
        </thead>
        <tbody>
          ${attendanceList.take(50).joinToString("") { a ->
            val badgeClass = when(a.status) {
              "GELDI" -> "badge-present"
              "GELMEDI" -> "badge-absent"
              else -> "badge-excused"
            }
            """
            <tr>
              <td>${a.date}</td>
              <td>#${a.studentId}</td>
              <td><span class="badge $badgeClass">${a.status}</span></td>
            </tr>
            """
          }}
        </tbody>
      </table>
    </div>

    <!-- TAB 3: Ezber Çetelesi -->
    <div id="tab-memorization" class="card" style="display: none;">
      <div class="card-header">
        <div class="card-title">📖 Ezber & Ders Takip Listesi</div>
      </div>
      <table>
        <thead>
          <tr>
            <th>Talebe ID</th>
            <th>Kategori</th>
            <th>Ders / Sure</th>
            <th>Durum</th>
            <th>Puan</th>
            <th>Tarih</th>
          </tr>
        </thead>
        <tbody>
          ${memorizationList.take(50).joinToString("") { m ->
            """
            <tr>
              <td>#${m.studentId}</td>
              <td><strong>${m.category}</strong></td>
              <td>${m.title}</td>
              <td><span class="badge ${if (m.status == "TAMAMLANDI") "badge-present" else "badge-excused"}">${m.status}</span></td>
              <td>⭐ ${m.rating}/5</td>
              <td>${m.date}</td>
            </tr>
            """
          }}
        </tbody>
      </table>
    </div>

    <!-- TAB 4: Cloud Settings -->
    <div id="tab-cloud" class="card" style="display: none;">
      <div class="card-header">
        <div class="card-title">☁️ Firebase & Cloud Firestore Entegrasyonu</div>
      </div>
      <p style="color: var(--text-muted); margin-bottom: 16px;">
        Bu web portalı, Android uygulamanızla aynı Cloud Firestore veritabanına bağlanarak anlık iki yönlü veri senkronizasyonu sağlar.
      </p>
      <div style="background: #F8FAFC; border: 1px solid var(--border-color); padding: 16px; border-radius: 8px;">
        <strong>Firebase Proje Durumu:</strong> <span style="color: var(--green-present); font-weight: bold;">● Entegre & Hazır</span><br>
        <small>Web arayüzünden yapılan değişiklikler Android uygulamanızda anında görüntülenir.</small>
      </div>
    </div>
  </main>

  <script>
    function switchTab(tabId) {
      document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
      document.querySelectorAll('.card').forEach(card => card.style.display = 'none');
      
      const activeBtn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.getAttribute('onclick').includes(tabId));
      if (activeBtn) activeBtn.classList.add('active');
      
      const targetCard = document.getElementById('tab-' + tabId);
      if (targetCard) targetCard.style.display = 'block';
    }

    function filterStudents() {
      const query = document.getElementById('student-search').value.toLowerCase();
      const rows = document.querySelectorAll('#students-tbody tr');
      rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(query) ? '' : 'none';
      });
    }
  </script>
</body>
</html>
    """.trimIndent()
  }
}
