package com.example.data.dao

import androidx.room.*
import com.example.data.model.Lead
import com.example.data.model.AutomationRule
import com.example.data.model.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY leadScorePercent DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: Int): Lead?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("DELETE FROM leads")
    suspend fun deleteAllLeads()
}

@Dao
interface AutomationRuleDao {
    @Query("SELECT * FROM automation_rules ORDER BY id DESC")
    fun getRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Int)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
