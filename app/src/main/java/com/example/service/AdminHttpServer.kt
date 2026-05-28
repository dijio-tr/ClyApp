package com.example.service

import android.util.Log
import com.example.data.model.Lead
import com.example.data.model.AutomationRule
import com.example.data.model.ActivityLog
import com.example.data.repository.LeadRepository
import com.example.ui.viewmodel.LeadViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object AdminHttpServer {
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start(viewModel: LeadViewModel, repository: LeadRepository) {
        if (isRunning) return
        isRunning = true
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(9090)
                Log.d("AdminHttpServer", "Clyve App local admin hub server listening on port 9090...")
                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        try {
                            handleClient(clientSocket, viewModel, repository)
                        } catch (e: Exception) {
                            Log.e("AdminHttpServer", "Client handler error: ${e.message}")
                        } finally {
                            try { clientSocket.close() } catch (ex: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminHttpServer", "Server socket exception: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        serverJob = null
    }

    private fun handleClient(socket: Socket, viewModel: LeadViewModel, repository: LeadRepository) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = socket.getOutputStream()
        
        val firstLine = reader.readLine() ?: return
        Log.d("AdminHttpServer", "Request: $firstLine")
        
        val parts = firstLine.split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        val fullPath = parts[1]
        
        val urlParts = fullPath.split("?")
        val path = urlParts[0]
        val queryString = if (urlParts.size > 1) urlParts[1] else ""
        val params = parseQueryString(queryString)

        when {
            path == "/" || path == "/index.html" || path == "/dashboard" -> {
                serveDashboard(output, viewModel, repository)
            }
            path == "/api/leads" -> {
                serveLeadsJson(output, viewModel)
            }
            path == "/api/logs" -> {
                serveLogsJson(output, viewModel)
            }
            path == "/api/remote-scan" -> {
                val q = params["query"] ?: ""
                if (q.isNotEmpty()) {
                    viewModel.viewModelScope.launch {
                        viewModel.scanLeads(q)
                        repository.insertLog("Admin (Web PC)", "PC tarayıcısından uzaktan B2B araması başlatıldı: '$q'", "WEB")
                    }
                }
                serveRedirect(output, "/dashboard?msg=ScanStarted")
            }
            path == "/api/delete" -> {
                val idStr = params["id"] ?: ""
                val id = idStr.toIntOrNull()
                if (id != null) {
                    viewModel.viewModelScope.launch {
                        val list = viewModel.leads.value
                        val matched = list.find { it.id == id }
                        if (matched != null) {
                            viewModel.deleteLead(matched)
                            repository.insertLog("Admin (Web PC)", "Firma kartı PC arayüzünden silindi: '${matched.name}'", "DELETE")
                        }
                    }
                }
                serveRedirect(output, "/dashboard?msg=LeadDeleted")
            }
            path == "/api/add-rule" -> {
                val trigger = params["trigger"] ?: ""
                val act = params["action"] ?: "MOCK_ALERT"
                if (trigger.isNotEmpty()) {
                    viewModel.viewModelScope.launch {
                        repository.insertRule(AutomationRule(ruleName = trigger, actionType = act))
                        repository.insertLog("Admin (Web PC)", "PC arayüzünden yeni otomasyon kuralı eklendi: '$trigger'", "RULE")
                    }
                }
                serveRedirect(output, "/dashboard?msg=RuleAdded")
            }
            path == "/api/delete-rule" -> {
                val idStr = params["id"] ?: ""
                val id = idStr.toIntOrNull()
                if (id != null) {
                    viewModel.viewModelScope.launch {
                        repository.deleteRuleById(id)
                        repository.insertLog("Admin (Web PC)", "PC arayüzünden otomasyon kuralı kaldırıldı ID: $id", "RULE")
                    }
                }
                serveRedirect(output, "/dashboard?msg=RuleDeleted")
            }
            path == "/api/clear-logs" -> {
                viewModel.viewModelScope.launch {
                    repository.clearLogs()
                    repository.insertLog("Admin (Web PC)", "Tüm sistem, arama ve güvenlik logları temizlendi.", "SECURITY")
                }
                serveRedirect(output, "/dashboard?msg=LogsCleared")
            }
            path == "/api/reset-database" -> {
                viewModel.viewModelScope.launch {
                    repository.clearAllLeads()
                    repository.seedInitialDataIfNecessary()
                    repository.insertLog("Admin (Web PC)", "Veritabanı sıfırlandı ve örnek B2B verileriyle dolduruldu.", "RESET")
                }
                serveRedirect(output, "/dashboard?msg=DatabaseReset")
            }
            else -> {
                serve404(output)
            }
        }
    }

    private fun parseQueryString(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isEmpty()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun serveRedirect(output: OutputStream, locate: String) {
        val pw = PrintWriter(output)
        pw.println("HTTP/1.1 303 See Other")
        pw.println("Location: $locate")
        pw.println("Connection: close")
        pw.println()
        pw.flush()
    }

    private fun serve404(output: OutputStream) {
        val pw = PrintWriter(output)
        pw.println("HTTP/1.1 404 Not Found")
        pw.println("Content-Type: text/plain; charset=utf-8")
        pw.println("Connection: close")
        pw.println()
        pw.println("404 - Clyve Console Page Not Found")
        pw.flush()
    }

    private fun serveLeadsJson(output: OutputStream, viewModel: LeadViewModel) {
        val pw = PrintWriter(output)
        pw.println("HTTP/1.1 200 OK")
        pw.println("Content-Type: application/json; charset=utf-8")
        pw.println("Connection: close")
        pw.println()
        
        val leads = viewModel.leads.value
        val sb = StringBuilder()
        sb.append("[")
        leads.forEachIndexed { idx, lead ->
            sb.append("{")
            sb.append("\"id\":${lead.id},")
            sb.append("\"name\":\"${escapeJson(lead.name)}\",")
            sb.append("\"website\":\"${escapeJson(lead.websiteUrl)}\",")
            sb.append("\"email\":\"${escapeJson(lead.contactEmail)}\",")
            sb.append("\"leadScorePercent\":${lead.leadScorePercent},")
            sb.append("\"outreachStatus\":\"${escapeJson(lead.outreachStatus)}\"")
            sb.append("}")
            if (idx < leads.size - 1) sb.append(",")
        }
        sb.append("]")
        
        pw.println(sb.toString())
        pw.flush()
    }

    private fun serveLogsJson(output: OutputStream, viewModel: LeadViewModel) {
        val pw = PrintWriter(output)
        pw.println("HTTP/1.1 200 OK")
        pw.println("Content-Type: application/json; charset=utf-8")
        pw.println("Connection: close")
        pw.println()
        
        val logs = viewModel.logs.value
        val sb = StringBuilder()
        sb.append("[")
        logs.forEachIndexed { idx, log ->
            sb.append("{")
            sb.append("\"id\":${log.id},")
            sb.append("\"operator\":\"${escapeJson(log.operatorName)}\",")
            sb.append("\"action\":\"${escapeJson(log.actionDetails)}\",")
            sb.append("\"timestamp\":${log.timestamp},")
            sb.append("\"tag\":\"${escapeJson(log.tag)}\"")
            sb.append("}")
            if (idx < logs.size - 1) sb.append(",")
        }
        sb.append("]")
        
        pw.println(sb.toString())
        pw.flush()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
    }

    private fun serveDashboard(output: OutputStream, viewModel: LeadViewModel, repository: LeadRepository) {
        val pw = PrintWriter(output)
        pw.println("HTTP/1.1 200 OK")
        pw.println("Content-Type: text/html; charset=utf-8")
        pw.println("Connection: close")
        pw.println()

        val leads = viewModel.leads.value
        val rules = viewModel.rules.value
        val logs = viewModel.logs.value
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        val leadsContent = if (leads.isEmpty()) {
            "<tr><td colspan='7' class='text-center text-muted'>Veritabanında taranmış herhangi bir B2B firma bulunamadı.</td></tr>"
        } else {
            leads.joinToString("") { lead ->
                val webBadge = if (lead.hasWebsite) {
                    "<span class='badge bg-success'>Web Var</span>"
                } else {
                    "<span class='badge bg-danger'>Web Yok</span>"
                }
                val adsBadge = if (lead.hasAds) {
                    "<span class='badge bg-info'>Reklam Veriyor</span>"
                } else {
                    "<span class='badge bg-secondary'>Sessiz</span>"
                }
                
                val scoreColorClass = when {
                    lead.leadScorePercent >= 80 -> "text-success fw-bold"
                    lead.leadScorePercent >= 50 -> "text-warning fw-bold"
                    else -> "text-danger fw-bold"
                }

                """
                <tr>
                    <td><strong>#${lead.id}</strong></td>
                    <td>
                        <div class='fw-bold text-light'>${lead.name}</div>
                        <small class='text-muted'>${lead.searchCategory}</small>
                    </td>
                    <td>
                        <a href='${lead.websiteUrl}' target='_blank' class='text-info text-decoration-none'>${lead.websiteUrl}</a>
                    </td>
                    <td>
                        <small class='text-light'>${lead.contactEmail}</small>
                    </td>
                    <td>
                         <div class='outreach-status-bubble'>${lead.outreachStatus}</div>
                    </td>
                    <td>
                        <span class='$scoreColorClass'>${lead.leadScorePercent}%</span>
                        <div class='mt-1'>$webBadge $adsBadge</div>
                    </td>
                    <td>
                        <div class='btn-group btn-group-sm'>
                            <button type='button' class='btn btn-outline-info' onclick='showVerdict("${escapeJs(lead.name)}", "${escapeJs(lead.analysisNotes)}")'>Analiz Detayı</button>
                            <a href='/api/delete?id=${lead.id}' class='btn btn-outline-danger' onclick='return confirm("${lead.name} silinsin mi?")'>Sil</a>
                        </div>
                    </td>
                </tr>
                """.trimIndent()
            }
        }

        val rulesContent = if (rules.isEmpty()) {
            "<li class='list-group-item bg-dark text-mutedSmall text-center'>Tanımlanmış bir otomasyon tetikleyici kuralı bulunmamaktadır.</li>"
        } else {
            rules.joinToString("") { rule ->
                """
                <li class='list-group-item bg-dark text-light d-flex justify-content-between align-items-center border-secondary'>
                    <div>
                        <span class='badge bg-primary me-2'>Kural</span>
                        <strong>"${rule.ruleName}"</strong> geçerse
                        <span class='badge bg-secondary ms-2'>${rule.actionType}</span> eylemini tetikle
                    </div>
                    <a href='/api/delete-rule?id=${rule.id}' class='btn btn-sm btn-outline-danger py-0 px-2'>&times;</a>
                </li>
                """.trimIndent()
            }
        }

        val logsContent = if (logs.isEmpty()) {
            "<tr><td colspan='4' class='text-center text-muted'>Çalışma kaydı bulunamadı.</td></tr>"
        } else {
            logs.reversed().take(25).joinToString("") { log ->
                val trColor = when(log.tag) {
                    "SECURITY" -> "table-danger"
                    "RULE" -> "table-warning"
                    "DELETE" -> "table-secondary"
                    "RESET" -> "table-dark"
                    else -> "table-active"
                }
                val badgeColor = when(log.tag) {
                    "SECURITY" -> "bg-danger"
                    "RULE" -> "bg-warning text-dark"
                    "DELETE" -> "bg-secondary"
                    "RESET" -> "bg-light text-dark"
                    "WEB" -> "bg-info text-dark"
                    else -> "bg-primary"
                }
                """
                <tr class='$trColor'>
                    <td><small class='text-dark'>${sdf.format(Date(log.timestamp))}</small></td>
                    <td><span class='badge $badgeColor'>${log.tag}</span></td>
                    <td><strong class='text-dark'>${log.operatorName}</strong></td>
                    <td class='text-dark'>${log.actionDetails}</td>
                </tr>
                """.trimIndent()
            }
        }

        val html = """
        <!DOCTYPE html>
        <html lang="tr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Clyve B2B - Arka Plan Web Yönetim Paneli</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            <style>
                body {
                    background-color: #0d1117;
                    color: #c9d1d9;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                }
                .navbar-custom {
                    background-color: #161b22;
                    border-bottom: 1.5px solid #21262d;
                }
                .card-custom {
                    background-color: #161b22;
                    border: 1px solid #30363d;
                    border-radius: 12px;
                    margin-bottom: 20px;
                }
                .card-header-custom {
                    background-color: #21262d;
                    border-bottom: 1px solid #30363d;
                    color: #58a6ff;
                    font-weight: bold;
                    border-top-left-radius: 12px !important;
                    border-top-right-radius: 12px !important;
                }
                .table-custom {
                    color: #c9d1d9;
                }
                .table-custom th {
                    background-color: #21262d;
                    color: #8b949e;
                    border-bottom: 2px solid #30363d;
                }
                .table-custom td {
                    border-bottom: 1px solid #21262d;
                    vertical-align: middle;
                }
                .text-mutedSmall {
                    font-size: 0.85rem;
                }
                .outreach-status-bubble {
                    display: inline-block;
                    padding: 4px 8px;
                    font-size: 0.75rem;
                    font-weight: bold;
                    color: #ffffff;
                    background: linear-gradient(135deg, #1d4ed8, #2563eb);
                    border-radius: 6px;
                }
                /* Custom scrollbar */
                ::-webkit-scrollbar {
                    width: 8px;
                    height: 8px;
                }
                ::-webkit-scrollbar-track {
                    background: #0d1117;
                }
                ::-webkit-scrollbar-thumb {
                    background: #30363d;
                    border-radius: 4px;
                }
                ::-webkit-scrollbar-thumb:hover {
                    background: #8b949e;
                }
            </style>
        </head>
        <body>
            <nav class="navbar navbar-dark navbar-custom py-3">
                <div class="container-fluid px-4">
                    <span class="navbar-brand mb-0 h1 d-flex align-items-center fw-bold">
                        <span class="fs-4 me-2">📡</span> CLYVE B2B AI CRAWLER & RADAR PORTAL
                    </span>
                    <div class="d-flex align-items-center">
                        <span class="badge bg-success me-3 py-2 px-3">Sunucu Durumu: AKTİF (Port 9090)</span>
                        <a href="/dashboard" class="btn btn-outline-light btn-sm">Paneli Yenile</a>
                    </div>
                </div>
            </nav>

            <div class="container-fluid p-4">
                <div class="row">
                    <!-- Stat Widgets -->
                    <div class="col-md-3">
                        <div class="card card-custom p-3 text-center">
                            <h6 class="text-muted mb-1">Taranan B2B Portföyü</h6>
                            <h2 class="text-primary fw-bold mb-0">${leads.size}</h2>
                            <small class="text-success mt-1">✓ Yerel Veritabanına Kayıtlı</small>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-custom p-3 text-center">
                            <h6 class="text-muted mb-1">Yönetici Hesabı</h6>
                            <h2 class="text-light fw-bold mb-0" style="font-size: 1.4rem; padding: 6px 0;">mustafaeenesozkaya</h2>
                            <small class="text-muted">Kurucu / Super Admin</small>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-custom p-3 text-center">
                            <h6 class="text-muted mb-1">Kural Bazlı Otomasyon</h6>
                            <h2 class="text-warning fw-bold mb-0">${rules.size}</h2>
                            <small class="text-muted">Aktif Tarama Tetikleyicisi</small>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card card-custom p-3 text-center">
                            <h6 class="text-muted mb-1">Operasyonel Log Sayısı</h6>
                            <h2 class="text-info fw-bold mb-0">${logs.size}</h2>
                            <small class="text-muted">Sistem ve Güvenlik İzleri</small>
                        </div>
                    </div>
                </div>

                <div class="row">
                    <!-- Left: Remote Actions / Scanning & Automation Rules -->
                    <div class="col-lg-4 col-md-5">
                        
                        <!-- Remote Trigger Console -->
                        <div class="card card-custom">
                            <div class="card-header card-header-custom d-flex justify-content-between align-items-center">
                                <span>🚀 Bilgisayardan Uzaktan B2B Tarama Başlat</span>
                            </div>
                            <div class="card-body">
                                <form action="/api/remote-scan" method="get">
                                    <div class="mb-3">
                                        <label for="query" class="form-label text-mutedSmall">Bölge, Sektör veya İşletme Adı Girin</label>
                                        <input type="text" class="form-control bg-dark text-light border-secondary" id="query" name="query" placeholder="Örn: Avrasya Hotel, Trabzon Kafe, İstanbul Yazılım" required>
                                    </div>
                                    <button type="submit" class="btn btn-primary w-100 fw-bold">Bulut AI Sondaj Radarı Tetikle</button>
                                </form>
                                <hr class="border-secondary my-3"/>
                                <div class="text-mutedSmall">
                                    <strong>Nasıl Çalışır?</strong> Web konsolundan girdiğiniz hedef arama sinyali anında mobil uygulamanın yapay zeka tarayıcısına aktarılır ve otomatik olarak yeni firmalar taranır.
                                </div>
                            </div>
                        </div>

                        <!-- Automation Trigger Rules Card -->
                        <div class="card card-custom">
                            <div class="card-header card-header-custom">
                                <span>⚙️ Akıllı Otomasyon ve E-Posta Tetikleyicileri</span>
                            </div>
                            <div class="card-body">
                                <form action="/api/add-rule" method="get" class="mb-3">
                                    <div class="row g-2">
                                        <div class="col-8">
                                            <input type="text" class="form-control form-control-sm bg-dark text-light border-secondary" name="trigger" placeholder="Tetikleyici Kelime (Örn: otel)" required>
                                        </div>
                                        <div class="col-4">
                                            <select name="action" class="form-select form-select-sm bg-dark text-light border-secondary">
                                                <option value="MOCK_ALERT">Uyar</option>
                                                <option value="PITCH_PROPOSAL">Teklif Yaz</option>
                                                <option value="PRIORITY_BOOST">Skor Yükselt</option>
                                            </select>
                                        </div>
                                    </div>
                                    <button type="submit" class="btn btn-warning btn-sm w-100 mt-2 fw-bold text-dark">Kural Ekle</button>
                                </form>

                                <ul class="list-group list-group-flush rounded border border-secondary" style="max-height: 250px; overflow-y: auto;">
                                    $rulesContent
                                </ul>
                            </div>
                        </div>

                        <!-- System Utilities Console -->
                        <div class="card card-custom">
                            <div class="card-header card-header-custom">
                                <span>🛠️ Veritabanı Servisleri ve Araçlar</span>
                            </div>
                            <div class="card-body text-center">
                                <div class="d-grid gap-2">
                                    <a href="/api/reset-database" class="btn btn-outline-warning btn-sm" onclick="return confirm('Tüm veriler silinip başlangıç verileri sıfırdan yüklenecektir. Emin misiniz?')">Örnek Verileri ve Simülasyonu Sıfırla</a>
                                    <a href="/api/clear-logs" class="btn btn-outline-secondary btn-sm" onclick="return confirm('Tüm çalışma ve IP log veri kayıtları silinecektir. Emin misiniz?')">Güvenlik Loglarını Temizle</a>
                                    <a href="/api/leads" target="_blank" class="btn btn-outline-info btn-sm">Müşteri Verilerini JSON Olarak Çek</a>
                                    <a href="/api/logs" target="_blank" class="btn btn-outline-info btn-sm">Sistem Loglarını JSON Olarak Çek</a>
                                </div>
                            </div>
                        </div>

                    </div>

                    <!-- Right: Large Leads database & activity logs table -->
                    <div class="col-lg-8 col-md-7">
                        
                        <!-- Leads Database Card -->
                        <div class="card card-custom">
                            <div class="card-header card-header-custom d-flex justify-content-between align-items-center">
                                <span>🏢 Taranan Firmanın Dosyaları ve Detaylı Analiz Raporları</span>
                                <span class="badge bg-primary">${leads.size} Kayıt</span>
                            </div>
                            <div class="table-responsive" style="max-height: 500px; overflow-y: auto;">
                                <table class="table table-custom mb-0 text-mutedSmall">
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Firma / Sektör</th>
                                            <th>İletişim & Web Sitesi</th>
                                            <th>E-Posta Adresi</th>
                                            <th>Teklif Durumu</th>
                                            <th>Yapay Zeka Skorlama</th>
                                            <th>İşlemler</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        $leadsContent
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <!-- Real-time Activity Logs Console -->
                        <div class="card card-custom">
                            <div class="card-header card-header-custom d-flex justify-content-between align-items-center">
                                <span>📋 Arka Plan Operasyonel ve IP Güvenlik Audit Log İzleyici</span>
                                <span class="badge bg-secondary">Son 25 Kayıt</span>
                            </div>
                            <div class="table-responsive" style="max-height: 350px;">
                                <table class="table table-sm table-custom mb-0 text-mutedSmall" style="color: #111;">
                                    <thead>
                                        <tr class="table-dark">
                                            <th>Zaman Damgası</th>
                                            <th>Kategori</th>
                                            <th>Operatör</th>
                                            <th>Mekanizma & Yapılan İşlem Detayı</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        $logsContent
                                    </tbody>
                                </table>
                            </div>
                        </div>

                    </div>
                </div>
            </div>

            <!-- Verdict Analysis Modal -->
            <div class="modal fade" id="verdictModal" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-lg modal-dialog-centered">
                    <div class="modal-content bg-dark text-light border-secondary">
                        <div class="modal-header border-secondary">
                            <h5 class="modal-title" id="verdictModalTitle">B2B Firma Analiz Detay Raporu</h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <p id="verdictModalBody" style="white-space: pre-line; line-height: 1.6; font-size: 0.95rem;"></p>
                        </div>
                        <div class="modal-footer border-secondary">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Kapat</button>
                        </div>
                    </div>
                </div>
            </div>

            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
            <script>
                const modal = new bootstrap.Modal(document.getElementById('verdictModal'));
                function showVerdict(compName, text) {
                    document.getElementById('verdictModalTitle').innerText = compName + " - B2B Yapay Zeka Karar Raporu";
                    document.getElementById('verdictModalBody').innerText = text ? text : "Bu firma hakkında detaylı bir yapay zeka analiz raporu bulunmuyor.";
                    modal.show();
                }
            </script>
        </body>
        </html>
        """.trimIndent()

        pw.println(html)
        pw.flush()
    }

    private fun escapeJs(str: String): String {
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
    }
}
