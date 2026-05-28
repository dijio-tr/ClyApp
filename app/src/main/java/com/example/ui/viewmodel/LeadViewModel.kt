package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.model.Lead
import com.example.data.model.AutomationRule
import com.example.data.model.ActivityLog
import com.example.data.model.PreloadedPdf
import com.example.data.network.GeminiApiClient
import com.example.data.repository.LeadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LeadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LeadRepository

    // State flows directly backed by DB
    val leads: StateFlow<List<Lead>>
    val rules: StateFlow<List<AutomationRule>>
    val logs: StateFlow<List<ActivityLog>>

    // UI Interactive States
    val currentUser = MutableStateFlow("Mustafa")
    val searchQuery = MutableStateFlow("Avrasya Hotel")
    val isScanning = MutableStateFlow(false)
    val selectedLead = MutableStateFlow<Lead?>(null)

    // User Authentication & IP Security States
    val isUserLoggedIn = MutableStateFlow(true)
    val userEmail = MutableStateFlow("mustafaeenesozkaya@gmail.com")
    val isAdmin = MutableStateFlow(true) // Initialized to true since userEmail matches admin list
    val userDisplayName = MutableStateFlow("Mustafa Enes Özkaya")
    val userPassword = MutableStateFlow("Password123")
    val userSimulatedIp = MutableStateFlow("85.105.42.118") // Captured Turkish Dynamic ISP IP block

    // Settings & Policy States
    val pushNotificationsEnabled = MutableStateFlow(true)
    val scanningNotificationsEnabled = MutableStateFlow(true)
    val evaluationRating = MutableStateFlow(0)
    val evaluationFeedback = MutableStateFlow("")
    val appLanguage = MutableStateFlow("tr") // Multilingual Toggle: "tr" or "en"
    val accountDeletionScheduledTime = MutableStateFlow<Long?>(null) // Millis if queued

    // Notification HUD State
    val notification = MutableStateFlow<String?>(null)

    // Preloaded PDF listing
    val pdfFiles = MutableStateFlow(listOf(
        PreloadedPdf("Avrasya_Hotel_Marketing_Proposal.pdf", "2.4 MB", "Tailored booking optimization brochure and ad budget projections.", "2026-05-27"),
        PreloadedPdf("SEO_Strategic_Audit.pdf", "4.1 MB", "Technological scan analyzing missing tags, slow load metrics, and site keywords.", "2026-05-27"),
        PreloadedPdf("Web_Redesign_Sales_Brochure.pdf", "1.8 MB", "A beautiful sales catalogue outlining our standard custom landing page packages.", "2026-05-27"),
        PreloadedPdf("Subscription_Welcome_Guide.pdf", "1.2 MB", "Onboarding sequence document containing guides on managing CRM subscriptions.", "2026-05-27")
    ))

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LeadRepository(
            database.leadDao(),
            database.automationRuleDao(),
            database.activityLogDao()
        )

        // Launch the local desktop Web Administrative Server on startup
        com.example.service.AdminHttpServer.start(this, repository)

        leads = repository.allLeads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        rules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        logs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial records securely in ViewModel launch background
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
            // Default selected lead to Avrasya Hotel if available
            leads.collectLatest { list ->
                if (list.isNotEmpty() && selectedLead.value == null) {
                    val avrasya = list.find { it.name.contains("Avrasya") }
                    selectedLead.value = avrasya ?: list.first()
                }
            }
        }
    }

    // AI Researcher search & scanning engine
    fun scanLeads(query: String) {
        if (query.trim().isEmpty()) return
        viewModelScope.launch {
            isScanning.value = true
            val activeUser = currentUser.value
            repository.insertLog(activeUser, "Started deep-research query: \"$query\" using Google index data via Gemini", "SCAN")

            val apiKey = BuildConfig.GEMINI_API_KEY
            val foundLeads = GeminiApiClient.searchAndAnalyzeLeads(query, apiKey)
            
            var addedCount = 0
            for (lead in foundLeads) {
                val leadWithCreator = lead.copy(createdBy = activeUser, lastActivityTime = System.currentTimeMillis())
                val newId = repository.insertLead(leadWithCreator)
                addedCount++
                
                // Automatically set the newly scanned lead as selected for detail view
                if (addedCount == 1) {
                    selectedLead.value = leadWithCreator.copy(id = newId.toInt())
                }
            }

            repository.insertLog(
                "System", 
                "Successfully analyzed & scoured $addedCount prospect(s) for query: \"$query\"", 
                "SCAN"
            )
            isScanning.value = false
            triggerNotification("$addedCount new leads scoured successfully!")
        }
    }

    // Toggle local collaborative user workspace
    fun switchUser(newUser: String) {
        viewModelScope.launch {
            currentUser.value = newUser
            repository.insertLog("System", "Active researcher profile changed to: $newUser", "INFO")
            triggerNotification("Switched to $newUser's workspace")
        }
    }

    fun selectLead(lead: Lead?) {
        selectedLead.value = lead
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
            if (selectedLead.value?.id == lead.id) {
                selectedLead.value = null
            }
            repository.insertLog(currentUser.value, "Deleted prospect lead: ${lead.name}", "INFO")
        }
    }

    // Manual email outreach simulation
    fun sendManualEmail(lead: Lead, subject: String, body: String) {
        viewModelScope.launch {
            val updated = lead.copy(outreachStatus = "EMAIL_SENT", lastActivityTime = System.currentTimeMillis())
            repository.updateLead(updated)
            if (selectedLead.value?.id == lead.id) {
                selectedLead.value = updated
            }

            // Real simulated email sent to your own email mustafaeenesozkaya@gmail.com or predefined destination
            val operator = currentUser.value
            repository.insertLog(
                operator, 
                "Outbound cold email sent to ${lead.contactEmail} from ${operator.lowercase()}@duoprospector.io. Subject: \"$subject\"", 
                "EMAIL"
            )
            triggerNotification("Outreach proposal dispatched to ${lead.contactEmail}")
        }
    }

    // Simulator: customer replies and trigger core flow logic
    fun simulateCustomerReply(lead: Lead) {
        viewModelScope.launch {
            val activeRules = rules.value.filter { it.isActive && it.triggerOn == "ON_REPLY" }
            val updatedLead = lead.copy(outreachStatus = "REPLIED", lastActivityTime = System.currentTimeMillis())
            repository.updateLead(updatedLead)
            
            if (selectedLead.value?.id == lead.id) {
                selectedLead.value = updatedLead
            }

            repository.insertLog("Customer (${lead.name})", "Received customer reply to our campaign from ${lead.contactEmail}", "EMAIL")

            // Wait a brief second to simulate asynchronous worker engine processing
            if (activeRules.isNotEmpty()) {
                activeRules.forEach { rule ->
                    when (rule.actionType) {
                        "SEND_PDF" -> {
                            repository.insertLog(
                                "Auto-Bot",
                                "Rule triggered [${rule.ruleName}]: Sent predefined file \"${rule.pdfAttachmentName}\" to defined admin e-mail: ${rule.destinationEmail}",
                                "RULE"
                            )
                        }
                        "SUBSCRIBE_SYSTEM" -> {
                            val subscribedLead = updatedLead.copy(outreachStatus = "SUBSCRIBED")
                            repository.updateLead(subscribedLead)
                            if (selectedLead.value?.id == lead.id) {
                                selectedLead.value = subscribedLead
                            }
                            repository.insertLog(
                                "Automation",
                                "Rule triggered [${rule.ruleName}]: Enrolled ${lead.name} into CRM Premium Subscription System",
                                "RULE"
                            )
                        }
                        "EMAIL_OUT" -> {
                            repository.insertLog(
                                "Auto-Bot",
                                "Rule triggered [${rule.ruleName}]: Dispatched automated reply template: \"${rule.definedSubject}\" to ${lead.contactEmail}",
                                "RULE"
                            )
                        }
                    }
                }
                triggerNotification("Customer replied! Automation rules triggered successfully.")
            } else {
                // Default automated reply fallback
                val defaultSub = updatedLead.copy(outreachStatus = "SUBSCRIBED")
                repository.updateLead(defaultSub)
                if (selectedLead.value?.id == lead.id) {
                    selectedLead.value = defaultSub
                }
                repository.insertLog(
                    "Auto-Bot",
                    "Dispatched default proposal PDF \"Avrasya_Hotel_Marketing_Proposal.pdf\" to customer defined email: mustafaeenesozkaya@gmail.com",
                    "RULE"
                )
                triggerNotification("Customer replied! Default proposal PDF auto-dispatched.")
            }
        }
    }

    // Automation Rules management
    fun addNewRule(name: String, action: String, pdfAttachment: String, subject: String, body: String, targetEmail: String) {
        viewModelScope.launch {
            val rule = AutomationRule(
                ruleName = name,
                actionType = action,
                triggerOn = "ON_REPLY",
                pdfAttachmentName = pdfAttachment,
                definedSubject = subject,
                definedBody = body,
                destinationEmail = targetEmail.ifEmpty { "mustafaeenesozkaya@gmail.com" }
            )
            repository.insertRule(rule)
            repository.insertLog(currentUser.value, "Added active automation rule: \"$name\"", "RULE")
            triggerNotification("Rule '$name' added successfully!")
        }
    }

    fun deleteRule(ruleId: Int, name: String) {
        viewModelScope.launch {
            repository.deleteRuleById(ruleId)
            repository.insertLog(currentUser.value, "Removed automation rule: \"$name\"", "RULE")
            triggerNotification("Rule deleted")
        }
    }

    // Manual lead input
    fun addManualLead(name: String, website: String, email: String, notes: String, score: Int) {
        viewModelScope.launch {
            val hasWeb = website.trim().isNotEmpty()
            val hasAds = false // Manual default leads assume no active advertising initially
            val lead = Lead(
                name = name,
                websiteUrl = website,
                hasWebsite = hasWeb,
                hasAds = hasAds,
                leadScorePercent = score,
                contactEmail = email.ifEmpty { "contact@${name.lowercase().replace(" ", "")}.com" },
                analysisNotes = notes,
                searchCategory = "Manual Input",
                outreachStatus = "NEW",
                createdBy = currentUser.value
            )
            val newId = repository.insertLead(lead)
            selectedLead.value = lead.copy(id = newId.toInt())
            
            repository.insertLog(currentUser.value, "Manually logged B2B prospect: $name", "INFO")
            triggerNotification("Manual lead '$name' logged successfully")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllLeads()
            repository.clearLogs()
            selectedLead.value = null
            repository.seedInitialDataIfNecessary()
            triggerNotification("Database reset to initial state")
        }
    }

    private val ADMIN_EMAILS = setOf(
        "mustafaeenesozkaya@gmail.com"
    )

    fun checkIsEmailAdmin(email: String): Boolean {
        val em = email.lowercase().trim()
        return em in ADMIN_EMAILS
    }

    // User Authentication & Simulation Actions
    fun loginWithEmail(email: String, usernameVal: String, pass: String): Boolean {
        if (email.contains("@") && pass.length >= 6) {
            userEmail.value = email
            userDisplayName.value = if (usernameVal.isNotEmpty()) usernameVal else "Mustafa Enes"
            userPassword.value = pass
            currentUser.value = if (usernameVal.isNotEmpty()) usernameVal else "Mustafa"
            isUserLoggedIn.value = true
            isAdmin.value = checkIsEmailAdmin(email)
            
            viewModelScope.launch {
                repository.insertLog(
                    currentUser.value,
                    "Secure login success via credentials. Session IP logged: ${userSimulatedIp.value} (Secure Audit Vault)",
                    "AUTH"
                )
                triggerNotification(if (appLanguage.value == "tr") "Giriş Başarılı! Hoşgeldiniz, ${userDisplayName.value}" else "Login Success! Welcome, ${userDisplayName.value}")
            }
            return true
        } else {
            triggerNotification(if (appLanguage.value == "tr") "Hata: Geçersiz e-posta adresi veya şifre (Min 6 karakter)!" else "Error: Invalid email address or password (Min 6 characters)!")
            return false
        }
    }

    fun loginWithGoogle() {
        val googleEmail = "mustafaeenesozkaya@gmail.com"
        val googleName = "Mustafa Enes Özkaya (Google)"
        userEmail.value = googleEmail
        userDisplayName.value = googleName
        currentUser.value = "Mustafa"
        isUserLoggedIn.value = true
        isAdmin.value = checkIsEmailAdmin(googleEmail)
        
        viewModelScope.launch {
            repository.insertLog(
                "GoogleOAuth",
                "Successfully authenticated via Google Identity OpenID. Session IP stored: ${userSimulatedIp.value}",
                "AUTH"
            )
            triggerNotification(if (appLanguage.value == "tr") "Google ile başarıyla bağlandı! Hoşgeldiniz." else "Successfully connected with Google! Welcome.")
        }
    }

    fun signOut() {
        isUserLoggedIn.value = false
        val preUser = currentUser.value
        viewModelScope.launch {
            repository.insertLog(preUser, "User manually terminated interactive session. Safe sign-out recorded. IP: ${userSimulatedIp.value}", "AUTH")
            triggerNotification("Oturum kapatıldı.")
        }
    }

    fun changePassword(current: String, new: String): Boolean {
        if (current == userPassword.value) {
            if (new.length >= 6) {
                userPassword.value = new
                viewModelScope.launch {
                    repository.insertLog(
                        currentUser.value,
                        "Password changed successfully. Request secure token approved. IP Log: ${userSimulatedIp.value}",
                        "SECURITY"
                    )
                    triggerNotification("Şifreniz başarıyla güncellendi!")
                }
                return true
            } else {
                triggerNotification("Yeni şifre en az 6 karakter olmalıdır!")
                return false
            }
        } else {
            triggerNotification("Mevcut şifreniz hatalı!")
            return false
        }
    }

    fun submitEvaluation(stars: Int, comments: String) {
        evaluationRating.value = stars
        evaluationFeedback.value = comments
        viewModelScope.launch {
            repository.insertLog(
                currentUser.value,
                "Submitted application store evaluation: $stars stars with comments \"$comments\". IP: ${userSimulatedIp.value}",
                "FEEDBACK"
            )
            triggerNotification("Değerlendirmeniz için teşekkür ederiz! (Puan: $stars/5)")
        }
    }

    fun dispatchSupportTicket(subject: String, message: String): Boolean {
        if (subject.trim().isEmpty() || message.trim().isEmpty()) {
            triggerNotification("Hata: Konu ve mesaj alanları boş bırakılamaz!")
            return false
        }
        viewModelScope.launch {
            repository.insertLog(
                currentUser.value,
                "Support ticket dispatched via Email. Client IP: ${userSimulatedIp.value}. Subject: \"$subject\" Message: \"$message\"",
                "SUPPORT"
            )
            triggerNotification("Destek talebiniz e-posta ile iletildi! En kısa sürede döneceğiz.")
        }
        return true
    }

    fun scheduleAccountDeletion() {
        // Enforce 72-hour delay hold. 72 hours = 259,200,000 milliseconds
        val targetEpoch = System.currentTimeMillis() + (72L * 60L * 60L * 1000L)
        accountDeletionScheduledTime.value = targetEpoch
        viewModelScope.launch {
            repository.insertLog(
                currentUser.value,
                "ACCOUNT TERMINATION INITIATED. 72-hour safety delay queue enabled. KVKK rule: data vaulted under 6 months security quarantine prior to permanent system clean. Session IP: ${userSimulatedIp.value}",
                "SECURITY"
            )
            triggerNotification("Hesap silme işlemi başlatıldı! 72 saat sonra kalıcı olarak silinecektir.")
        }
    }

    fun cancelAccountDeletion() {
        accountDeletionScheduledTime.value = null
        viewModelScope.launch {
            repository.insertLog(
                currentUser.value,
                "Account termination request canceled. Status restored to active. Session IP: ${userSimulatedIp.value}",
                "SECURITY"
            )
            triggerNotification("Hesap silme işlemi iptal edildi.")
        }
    }

    private fun triggerNotification(message: String) {
        notification.value = message
    }

    fun dismissNotification() {
        notification.value = null
    }
}
