package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "leads")
@JsonClass(generateAdapter = true)
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val websiteUrl: String,
    val hasWebsite: Boolean,
    val hasAds: Boolean,
    val leadScorePercent: Int,
    val contactEmail: String,
    val analysisNotes: String,
    val searchCategory: String = "All",
    val outreachStatus: String = "NEW", // NEW, EMAIL_SENT, REPLIED, SUBSCRIBED
    val lastActivityTime: Long = System.currentTimeMillis(),
    val createdBy: String = "Mustafa",
    val latitude: Double = 39.9334,
    val longitude: Double = 32.8597,
    val city: String = "Ankara"
)

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleName: String,
    val triggerOn: String = "ON_REPLY", // ON_REPLY, ON_OUTREACH
    val actionType: String = "SEND_PDF", // SEND_PDF, SUBSCRIBE_SYSTEM, EMAIL_OUT
    val definedSubject: String = "RE: Your Digital Growth Strategy Proposal",
    val definedBody: String = "Thank you for getting back to us! Here is the detailed proposal we prepared for you.",
    val pdfAttachmentName: String = "Avrasya_Hotel_Marketing_Proposal.pdf",
    val destinationEmail: String = "mustafaeenesozkaya@gmail.com",
    val isActive: Boolean = true
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operatorName: String, // "Mustafa" or "Partner"
    val actionDetails: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "INFO" // INFO, SCAN, EMAIL, RULE
)

data class PreloadedPdf(
    val fileName: String,
    val sizeLabel: String,
    val description: String,
    val uploadTime: String
)
