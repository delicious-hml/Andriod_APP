package com.example.dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyplanner.data.AppDatabase
import com.example.dailyplanner.data.PlanItem
import com.example.dailyplanner.data.PlanTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class PlanCategory(val key: String, val label: String, val color: Long) {
    NONE("", "无分类", 0xFF9E9E9E),
    WORK("work", "工作", 0xFF2196F3),
    STUDY("study", "学习", 0xFF9C27B0),
    EXERCISE("exercise", "运动", 0xFF4CAF50),
    REST("rest", "休息", 0xFFFF9800),
    SOCIAL("social", "社交", 0xFFE91E63),
    MEAL("meal", "餐饮", 0xFFFF5722),
    OTHER("other", "其他", 0xFF607D8B);

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: NONE
    }
}

data class TimeSlot(
    val index: Int,
    val timeLabel: String,
    val content: String,
    val category: PlanCategory,
    val isEditable: Boolean,
    val isPast: Boolean,
    val isCurrent: Boolean
)

data class CalendarDay(
    val date: LocalDate,
    val title: String,
    val hasContent: Boolean,
    val isToday: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).planDao()
    private val today get() = LocalDate.now()

    // === 核心状态 ===
    private val _viewDate = MutableStateFlow(today)
    val viewDate: StateFlow<LocalDate> = _viewDate.asStateFlow()

    private val _viewDateStr = MutableStateFlow("")
    val viewDateStr: StateFlow<String> = _viewDateStr.asStateFlow()

    private val _isToday = MutableStateFlow(true)
    val isToday: StateFlow<Boolean> = _isToday.asStateFlow()

    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    // 今日时间流逝进度 0f~1f
    private val _dayProgress = MutableStateFlow(0f)
    val dayProgress: StateFlow<Float> = _dayProgress.asStateFlow()

    private val _filledCount = MutableStateFlow(0)
    val filledCount: StateFlow<Int> = _filledCount.asStateFlow()

    // 已过时间段数量
    private val _pastSlotCount = MutableStateFlow(0)
    val pastSlotCount: StateFlow<Int> = _pastSlotCount.asStateFlow()

    // 缓存当前日期原始数据
    private var cachedItems: List<PlanItem> = emptyList()

    // 当前正在编辑的 slot，避免 Room Flow 覆盖用户输入
    private val _editingSlotIndex = MutableStateFlow<Int?>(null)

    // === 搜索 ===
    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<PlanItem>>(emptyList())
    val searchResults: StateFlow<List<PlanItem>> = _searchResults.asStateFlow()

    // === 输入防抖 ===
    private val _contentBuffer = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 64)

    // === 模板 ===
    val templates: StateFlow<List<PlanTemplate>> = dao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === 日历 ===
    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _calendarDays = MutableStateFlow<List<CalendarDay>>(emptyList())
    val calendarDays: StateFlow<List<CalendarDay>> = _calendarDays.asStateFlow()

    private val _monthTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val monthTitles: StateFlow<Map<String, String>> = _monthTitles.asStateFlow()

    // === 弹窗 ===
    private val _showCopyDialog = MutableStateFlow(false)
    val showCopyDialog: StateFlow<Boolean> = _showCopyDialog.asStateFlow()

    private val _showTemplateDialog = MutableStateFlow(false)
    val showTemplateDialog: StateFlow<Boolean> = _showTemplateDialog.asStateFlow()

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    private val _showStatsDialog = MutableStateFlow(false)
    val showStatsDialog: StateFlow<Boolean> = _showStatsDialog.asStateFlow()

    private val _showCategoryPicker = MutableStateFlow(-1)
    val showCategoryPicker: StateFlow<Int> = _showCategoryPicker.asStateFlow()

    private val _showCalendarDialog = MutableStateFlow(false)
    val showCalendarDialog: StateFlow<Boolean> = _showCalendarDialog.asStateFlow()

    // === 消息 ===
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // 日期变化 → 原子更新
        viewModelScope.launch {
            _viewDate.collectLatest { date ->
                _viewDateStr.value = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                _isToday.value = date == today
                _editingSlotIndex.value = null
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

                launch {
                    dao.getPlanByDate(dateStr)
                        .distinctUntilChanged()
                        .collect { items ->
                            cachedItems = items
                            rebuildTimeSlots(date, items)
                        }
                }
                launch {
                    dao.getFilledCount(dateStr).collect { _filledCount.value = it }
                }
            }
        }

        // 搜索防抖
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .flatMapLatest { dao.search(it) }
                .collect { _searchResults.value = it }
        }

        // 输入防抖：300ms 批量写入
        viewModelScope.launch {
            _contentBuffer
                .debounce(300)
                .collect { (slotIndex, content) ->
                    saveSlotToDb(slotIndex, content)
                }
        }

        // 每秒刷新时间进度（平滑增长）
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000)
                updateDayProgress()
            }
        }

        // 每分钟刷新可编辑状态（半小时切换时才需要）
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                rebuildTimeSlots(_viewDate.value, cachedItems)
            }
        }

        // 初始加载日历
        loadCalendarData(YearMonth.now())
        updateDayProgress()
    }

    // === 核心方法 ===

    private fun getCurrentSlotIndex(): Int {
        val now = LocalTime.now()
        return now.hour * 2 + if (now.minute >= 30) 1 else 0
    }

    private fun updateDayProgress() {
        if (_viewDate.value == today) {
            val now = LocalTime.now()
            val totalMinutes = now.hour * 60 + now.minute
            _dayProgress.value = (totalMinutes.toFloat() / (24f * 60f)).coerceIn(0f, 1f)
        } else if (_viewDate.value.isBefore(today)) {
            _dayProgress.value = 1f
        } else {
            _dayProgress.value = 0f
        }
    }

    private fun rebuildTimeSlots(date: LocalDate, items: List<PlanItem>) {
        val contentMap = items.associateBy { it.slotIndex }
        val currentSlot = if (date == today) getCurrentSlotIndex() else -1
        val isPast = date.isBefore(today)

        _timeSlots.value = (0..47).map { index ->
            val item = contentMap[index]
            TimeSlot(
                index = index,
                timeLabel = formatTimeLabel(index),
                content = item?.content ?: "",
                category = PlanCategory.fromKey(item?.category ?: ""),
                isEditable = !isPast && (date != today || index >= currentSlot),
                isPast = isPast || (date == today && index < currentSlot),
                isCurrent = date == today && index == currentSlot
            )
        }

        // 计算已过时间段数量
        _pastSlotCount.value = when {
            isPast -> 48
            date == today -> currentSlot.coerceIn(0, 48)
            else -> 0
        }
    }

    private fun formatTimeLabel(index: Int): String {
        val hour = index / 2
        val minute = if (index % 2 == 0) "00" else "30"
        return String.format("%02d:%s", hour, minute)
    }

    // === 文本输入（只写缓冲区，不修改 _timeSlots，避免 LazyColumn 跳动） ===

    fun updateContent(slotIndex: Int, content: String) {
        _editingSlotIndex.value = slotIndex
        // 只发送到防抖缓冲区，300ms 后写 Room，Room Flow 回写 _timeSlots
        _contentBuffer.tryEmit(slotIndex to content)
    }

    fun onSlotFocused(slotIndex: Int) {
        _editingSlotIndex.value = slotIndex
    }

    fun onSlotBlurred() {
        _editingSlotIndex.value = null
    }

    private suspend fun saveSlotToDb(slotIndex: Int, content: String) {
        val dateStr = _viewDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = dao.getItem(dateStr, slotIndex)
        dao.insertOrUpdate(
            PlanItem(
                id = existing?.id ?: 0,
                date = dateStr,
                slotIndex = slotIndex,
                content = content,
                category = existing?.category ?: "",
                title = existing?.title ?: ""
            )
        )
    }

    // === 日期标题 ===
    fun updateTitle(title: String) {
        val dateStr = _viewDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            // 找到该日期任意一个 slot 来存储标题（用 slot 0）
            val existing = dao.getItem(dateStr, 0)
            dao.insertOrUpdate(
                PlanItem(
                    id = existing?.id ?: 0,
                    date = dateStr,
                    slotIndex = 0,
                    content = existing?.content ?: "",
                    category = existing?.category ?: "",
                    title = title
                )
            )
        }
    }

    // === 日期导航 ===
    fun goToToday() { _viewDate.value = today }
    fun goToDate(date: LocalDate) { _viewDate.value = date }
    fun previousDay() { _viewDate.value = _viewDate.value.minusDays(1) }
    fun nextDay() { _viewDate.value = _viewDate.value.plusDays(1) }

    // === 设置分类 ===
    fun setCategory(slotIndex: Int, category: PlanCategory) {
        val dateStr = _viewDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _showCategoryPicker.value = -1
        viewModelScope.launch {
            val existing = dao.getItem(dateStr, slotIndex)
            dao.insertOrUpdate(
                PlanItem(
                    id = existing?.id ?: 0,
                    date = dateStr,
                    slotIndex = slotIndex,
                    content = existing?.content ?: "",
                    category = category.key,
                    title = existing?.title ?: ""
                )
            )
        }
    }

    fun showCategoryPicker(slotIndex: Int) { _showCategoryPicker.value = slotIndex }
    fun hideCategoryPicker() { _showCategoryPicker.value = -1 }

    // === 复制计划 ===
    fun showCopyDialog() { _showCopyDialog.value = true }
    fun hideCopyDialog() { _showCopyDialog.value = false }

    fun copyPlanToFutureDays(days: Int) {
        if (days <= 0) {
            viewModelScope.launch { _toastMessage.emit("天数必须大于 0") }
            _showCopyDialog.value = false
            return
        }
        val sourceDate = _viewDate.value
        val sourceDateStr = sourceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            val sourceItems = dao.getPlanByDate(sourceDateStr).first()
            if (sourceItems.isEmpty()) {
                _toastMessage.emit("当前日期没有计划内容")
                return@launch
            }
            val targetDates = (1..days).map { sourceDate.plusDays(it.toLong()) }
            val targetDateStrs = targetDates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
            val existingDates = dao.getDatesWithData(targetDateStrs).toSet()

            val newItems = mutableListOf<PlanItem>()
            var skipped = 0
            targetDateStrs.forEach { targetDateStr ->
                if (targetDateStr in existingDates) {
                    skipped++
                } else {
                    sourceItems.forEach { item ->
                        newItems.add(PlanItem(date = targetDateStr, slotIndex = item.slotIndex, content = item.content, category = item.category))
                    }
                }
            }
            if (newItems.isNotEmpty()) dao.insertAll(newItems)
            val msg = if (skipped > 0) "已复制到 ${days - skipped} 天，跳过 $skipped 天" else "已复制到未来 $days 天"
            _toastMessage.emit(msg)
        }
        _showCopyDialog.value = false
    }

    // === 模板 ===
    fun showTemplateDialog() { _showTemplateDialog.value = true }
    fun hideTemplateDialog() { _showTemplateDialog.value = false }

    fun saveAsTemplate(name: String) {
        val dateStr = _viewDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            val items = dao.getPlanByDate(dateStr).first()
            if (items.isEmpty()) {
                _toastMessage.emit("当前日期没有计划内容")
                return@launch
            }
            val jsonArray = JSONArray()
            items.forEach { item ->
                jsonArray.put(JSONObject().apply {
                    put("slotIndex", item.slotIndex)
                    put("content", item.content)
                    put("category", item.category)
                })
            }
            dao.insertTemplate(PlanTemplate(name = name, slotsJson = jsonArray.toString()))
            _toastMessage.emit("模板「$name」已保存")
        }
    }

    fun loadTemplate(template: PlanTemplate) {
        val dateStr = _viewDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(template.slotsJson)
                val items = mutableListOf<PlanItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val existing = dao.getItem(dateStr, obj.getInt("slotIndex"))
                    items.add(
                        PlanItem(
                            id = existing?.id ?: 0,
                            date = dateStr,
                            slotIndex = obj.getInt("slotIndex"),
                            content = obj.getString("content"),
                            category = obj.optString("category", ""),
                            title = existing?.title ?: ""
                        )
                    )
                }
                dao.insertAll(items)
                _toastMessage.emit("模板「${template.name}」已加载")
            } catch (e: Exception) {
                _toastMessage.emit("模板数据损坏")
            }
        }
        _showTemplateDialog.value = false
    }

    fun deleteTemplate(template: PlanTemplate) {
        viewModelScope.launch {
            dao.deleteTemplate(template.id)
            _toastMessage.emit("模板「${template.name}」已删除")
        }
    }

    // === 搜索 ===
    fun showSearchDialog() { _showSearchDialog.value = true }
    fun hideSearchDialog() {
        _showSearchDialog.value = false
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
    fun search(keyword: String) { _searchQuery.value = keyword }

    // === 统计 ===
    fun showStatsDialog() { _showStatsDialog.value = true }
    fun hideStatsDialog() { _showStatsDialog.value = false }

    // === 日历 ===
    fun showCalendarDialog() {
        _calendarMonth.value = YearMonth.from(_viewDate.value)
        loadCalendarData(_calendarMonth.value)
        _showCalendarDialog.value = true
    }
    fun hideCalendarDialog() { _showCalendarDialog.value = false }

    fun previousMonth() {
        _calendarMonth.value = _calendarMonth.value.minusMonths(1)
        loadCalendarData(_calendarMonth.value)
    }

    fun nextMonth() {
        _calendarMonth.value = _calendarMonth.value.plusMonths(1)
        loadCalendarData(_calendarMonth.value)
    }

    private fun loadCalendarData(month: YearMonth) {
        viewModelScope.launch {
            val yearMonthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val titles = dao.getMonthTitles(yearMonthStr).associate { it.date to it.title }
            val datesWithData = dao.getMonthDatesWithData(yearMonthStr).toSet()
            _monthTitles.value = titles

            val daysInMonth = month.lengthOfMonth()
            val days = (1..daysInMonth).map { day ->
                val date = month.atDay(day)
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                CalendarDay(
                    date = date,
                    title = titles[dateStr] ?: "",
                    hasContent = dateStr in datesWithData,
                    isToday = date == today
                )
            }
            _calendarDays.value = days
        }
    }
}
