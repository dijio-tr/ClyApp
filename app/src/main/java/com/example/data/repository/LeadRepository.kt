package com.example.data.repository

import com.example.data.dao.LeadDao
import com.example.data.dao.AutomationRuleDao
import com.example.data.dao.ActivityLogDao
import com.example.data.model.Lead
import com.example.data.model.AutomationRule
import com.example.data.model.ActivityLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeadRepository(
    private val leadDao: LeadDao,
    private val automationRuleDao: AutomationRuleDao,
    private val activityLogDao: ActivityLogDao
) {
    val allLeads: Flow<List<Lead>> = leadDao.getAllLeads()
    val allRules: Flow<List<AutomationRule>> = automationRuleDao.getRules()
    val allLogs: Flow<List<ActivityLog>> = activityLogDao.getLogs()

    suspend fun insertLead(lead: Lead): Long {
        return leadDao.insertLead(lead)
    }

    suspend fun updateLead(lead: Lead) {
        leadDao.updateLead(lead)
    }

    suspend fun deleteLead(lead: Lead) {
        leadDao.deleteLead(lead)
    }

    suspend fun clearAllLeads() {
        leadDao.deleteAllLeads()
    }

    suspend fun insertRule(rule: AutomationRule) {
        automationRuleDao.insertRule(rule)
    }

    suspend fun deleteRuleById(id: Int) {
        automationRuleDao.deleteRuleById(id)
    }

    suspend fun insertLog(operator: String, action: String, tag: String = "INFO") {
        activityLogDao.insertLog(ActivityLog(
            operatorName = operator,
            actionDetails = action,
            timestamp = System.currentTimeMillis(),
            tag = tag
        ))
    }

    suspend fun clearLogs() {
        activityLogDao.clearLogs()
    }

    suspend fun seedInitialDataIfNecessary() {
        // Seed leads
        val existingLeads = leadDao.getAllLeads().firstOrNull()
        if (existingLeads.isNullOrEmpty()) {
            leadDao.insertLead(Lead(
                name = "Avrasya Hotel",
                websiteUrl = "https://www.avrasyahotel.com",
                hasWebsite = true,
                hasAds = false,
                leadScorePercent = 85,
                contactEmail = "mustafaeenesozkaya@gmail.com", // Pre-defined to user's email for outreach
                analysisNotes = "Avrasya Hotel maintains a stable website footprint but misses critical marketing triggers: Meta Pixels and Google Ads scripts are missing. Ads Library exhibits no recent active campaigns. Well-developed researcher verdict: 85% growth potential. Recommendation: Pitch automated Booking Campaign & Dynamic Google Retargeting Strategy.",
                searchCategory = "Hotels",
                outreachStatus = "NEW",
                createdBy = "Mustafa",
                latitude = 41.0027,
                longitude = 39.7168,
                city = "Trabzon"
            ))
 
            leadDao.insertLead(Lead(
                name = "Trabzon Grand Suites",
                websiteUrl = "",
                hasWebsite = false,
                hasAds = false,
                leadScorePercent = 95,
                contactEmail = "suites@trabzongrand.com",
                analysisNotes = "No website found on Google and no digital ads. Potential score 95%. Excellent prospect to pitch a full-funnel custom Landing Page development bundle paired with Google Search campaigns.",
                searchCategory = "Hotels",
                outreachStatus = "NEW",
                createdBy = "System",
                latitude = 41.0120,
                longitude = 39.7280,
                city = "Trabzon"
            ))
 
            leadDao.insertLead(Lead(
                name = "BlackSea Resort & Spa",
                websiteUrl = "https://www.blacksearesort.tr",
                hasWebsite = true,
                hasAds = true,
                leadScorePercent = 30,
                contactEmail = "marketing@blacksearesort.tr",
                analysisNotes = "Active advertisements detected on Google. Elegant web design with solid SEO optimization. Potential score 30%. Low-priority pitch opportunity.",
                searchCategory = "Resorts",
                outreachStatus = "NEW",
                createdBy = "Partner",
                latitude = 41.0201,
                longitude = 40.5234,
                city = "Rize"
            ))

            insertLog("System", "Seeded premium demo database with Avrasya Hotel", "SCAN")
        }

        // Seed rules
        val existingRules = automationRuleDao.getRules().firstOrNull()
        if (existingRules.isNullOrEmpty()) {
            automationRuleDao.insertRule(AutomationRule(
                ruleName = "Avrasya Marketing Brochure Auto-delivery",
                triggerOn = "ON_REPLY",
                actionType = "SEND_PDF",
                definedSubject = "RE: Exclusive Marketing Assets for Avrasya Hotel",
                definedBody = "Hi there,\n\nWe received your reply and we are thrilled to connect! As promised, attached is our comprehensive PDF guide on how we can double booking conversions for Avrasya Hotel.\n\nBest regards,\nB2B Lead Scout Automation Hub",
                pdfAttachmentName = "Avrasya_Hotel_Marketing_Proposal.pdf",
                destinationEmail = "mustafaeenesozkaya@gmail.com"
            ))

            automationRuleDao.insertRule(AutomationRule(
                ruleName = "B2B Subscription System Enroller",
                triggerOn = "ON_REPLY",
                actionType = "SUBSCRIBE_SYSTEM",
                definedSubject = "Welcome to Our Premium Marketing Subscription",
                definedBody = "Success! You have been subscribed directly to our Lead Nurturing sequences.",
                pdfAttachmentName = "",
                destinationEmail = "mustafaeenesozkaya@gmail.com"
            ))
            insertLog("System", "Initialized default outreach automation rules", "RULE")
        }
    }
}
