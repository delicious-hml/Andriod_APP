package com.example.dailyplanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_items WHERE date = :date ORDER BY slotIndex ASC")
    fun getPlanByDate(date: String): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE date = :date AND slotIndex = :slotIndex LIMIT 1")
    suspend fun getItem(date: String, slotIndex: Int): PlanItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: PlanItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlanItem>)

    @Query("SELECT DISTINCT date FROM plan_items ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Query("SELECT DISTINCT date FROM plan_items WHERE date IN (:dates)")
    suspend fun getDatesWithData(dates: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM plan_items WHERE date = :date AND content != ''")
    fun getFilledCount(date: String): Flow<Int>

    @Query("SELECT * FROM plan_items WHERE content LIKE '%' || :keyword || '%' ORDER BY date DESC, slotIndex ASC")
    fun search(keyword: String): Flow<List<PlanItem>>

    // 月历：获取某月每天的标题
    @Query("SELECT date, title FROM plan_items WHERE date LIKE :yearMonth || '%' AND title != '' GROUP BY date")
    suspend fun getMonthTitles(yearMonth: String): List<DateTitle>

    // 月历：获取某月有内容的日期
    @Query("SELECT DISTINCT date FROM plan_items WHERE date LIKE :yearMonth || '%' AND content != ''")
    suspend fun getMonthDatesWithData(yearMonth: String): List<String>

    // 模板
    @Query("SELECT * FROM plan_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<PlanTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PlanTemplate)

    @Query("DELETE FROM plan_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Int)
}

data class DateTitle(
    val date: String,
    val title: String
)
