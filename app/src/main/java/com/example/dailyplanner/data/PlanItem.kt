package com.example.dailyplanner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_items",
    indices = [Index(value = ["date", "slotIndex"], unique = true)]
)
data class PlanItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val slotIndex: Int,
    val content: String = "",
    val category: String = "",
    val title: String = ""
)
