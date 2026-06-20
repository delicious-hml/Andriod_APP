package com.example.dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_templates")
data class PlanTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val slotsJson: String  // JSON: [{"slotIndex":0,"content":"...","category":"..."}, ...]
)
