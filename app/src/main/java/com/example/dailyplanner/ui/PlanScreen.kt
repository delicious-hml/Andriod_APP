package com.example.dailyplanner.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dailyplanner.ui.theme.*
import com.example.dailyplanner.viewmodel.CalendarDay
import com.example.dailyplanner.viewmodel.PlanCategory
import com.example.dailyplanner.viewmodel.PlanViewModel
import com.example.dailyplanner.viewmodel.TimeSlot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(viewModel: PlanViewModel) {
    val timeSlots by viewModel.timeSlots.collectAsState()
    val viewDateStr by viewModel.viewDateStr.collectAsState()
    val isToday by viewModel.isToday.collectAsState()
    val dayProgress by viewModel.dayProgress.collectAsState()
    val filledCount by viewModel.filledCount.collectAsState()
    val showCopyDialog by viewModel.showCopyDialog.collectAsState()
    val showTemplateDialog by viewModel.showTemplateDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()
    val showStatsDialog by viewModel.showStatsDialog.collectAsState()
    val showCategoryPicker by viewModel.showCategoryPicker.collectAsState()
    val showCalendarDialog by viewModel.showCalendarDialog.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // 自动滚动到当前时间段
    val currentSlotIndex = remember(timeSlots) { timeSlots.indexOfFirst { it.isCurrent } }
    LaunchedEffect(currentSlotIndex, isToday) {
        if (isToday && currentSlotIndex >= 0) {
            listState.animateScrollToItem(maxOf(0, currentSlotIndex - 2))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgGradientStart, BgGradientMid, BgGradientEnd)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    viewDateStr = viewDateStr,
                    isToday = isToday,
                    dayProgress = dayProgress,
                    filledCount = filledCount,
                    onPrevious = { viewModel.previousDay() },
                    onNext = { viewModel.nextDay() },
                    onToday = { viewModel.goToToday() },
                    onDateClick = { viewModel.showCalendarDialog() },
                    onCopy = { viewModel.showCopyDialog() },
                    onTemplate = { viewModel.showTemplateDialog() },
                    onSearch = { viewModel.showSearchDialog() },
                    onStats = { viewModel.showStatsDialog() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    count = timeSlots.size,
                    key = { timeSlots[it].index }
                ) { idx ->
                    val slot = timeSlots[idx]
                    GlassTimeSlotRow(
                        slot = slot,
                        dateKey = viewDateStr,
                        onContentChange = remember(slot.index) {
                            { text: String -> viewModel.updateContent(slot.index, text) }
                        },
                        onFocus = remember(slot.index) {
                            { viewModel.onSlotFocused(slot.index) }
                        },
                        onBlur = remember(slot.index) {
                            { viewModel.onSlotBlurred() }
                        },
                        onCategoryClick = remember(slot.index) {
                            { viewModel.showCategoryPicker(slot.index) }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // 当前时间浮动按钮
        if (isToday && currentSlotIndex >= 0) {
            var currentTime by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while (true) {
                    val now = java.time.LocalTime.now()
                    currentTime = String.format("%02d:%02d", now.hour, now.minute)
                    val secondsUntilNextMinute = 60 - now.second
                    kotlinx.coroutines.delay(secondsUntilNextMinute * 1000L)
                }
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(maxOf(0, currentSlotIndex - 2))
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = CurrentSlotBorder,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text(currentTime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 弹窗
    if (showCopyDialog) CopyPlanDialog(onConfirm = { viewModel.copyPlanToFutureDays(it) }, onDismiss = { viewModel.hideCopyDialog() })
    if (showTemplateDialog) TemplateDialog(viewModel = viewModel)
    if (showSearchDialog) SearchDialog(viewModel = viewModel)
    if (showStatsDialog) StatsDialog(viewModel = viewModel)
    if (showCategoryPicker >= 0) CategoryPickerDialog(onSelect = { viewModel.setCategory(showCategoryPicker, it) }, onDismiss = { viewModel.hideCategoryPicker() })
    if (showCalendarDialog) CalendarDialog(viewModel = viewModel)
}

// ==================== 顶部栏 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    viewDateStr: String,
    isToday: Boolean,
    dayProgress: Float,
    filledCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateClick: () -> Unit,
    onCopy: () -> Unit,
    onTemplate: () -> Unit,
    onSearch: () -> Unit,
    onStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xC0FFFFFF), Color(0x80FFFFFF))))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("日常计划", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, "搜索", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onTemplate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Bookmark, "模板", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, "复制", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onStats, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.BarChart, "统计", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日期导航 — 点击日期打开日历
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronLeft, "前一天", tint = TextPrimary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = viewDateStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.clickable { onDateClick() }
                )
                Icon(
                    Icons.Default.CalendarMonth, "日历",
                    tint = TimeLabelColor,
                    modifier = Modifier.size(20.dp).clickable { onDateClick() }
                )
                if (!isToday) {
                    TextButton(
                        onClick = onToday,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("今天", fontSize = 12.sp, color = ButtonPrimary) }
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, "后一天", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 时间流逝进度条
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("今日已过 ${String.format("%.2f", dayProgress * 100)}%", fontSize = 11.sp, color = TextSecondary)
                Text("已填写 $filledCount/48", fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = dayProgress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = TimeLabelColor,
                trackColor = Color(0xFFE8EAED)
            )
        }
    }
}

// ==================== 时间段行（自适应高度、无 checkbox） ====================

@Composable
fun GlassTimeSlotRow(
    slot: TimeSlot,
    dateKey: String,
    onContentChange: (String) -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    onCategoryClick: () -> Unit
) {
    // dateKey 作为 remember key，切换日期时自动重置文本状态
    var text by remember(slot.index, dateKey) { mutableStateOf(slot.content) }

    // 外部数据变化时同步（加载模板等）
    LaunchedEffect(slot.content, dateKey) {
        if (text != slot.content) {
            text = slot.content
        }
    }

    // 背景色基于本地 text（不依赖 slot.content，避免 Room Flow 触发列表重建）
    val bgColor = when {
        slot.isCurrent -> CurrentSlotBg
        slot.isPast -> ReadOnlyBg
        text.isNotEmpty() && slot.isEditable -> FutureSlotBg
        slot.isEditable -> EditableBg
        else -> ReadOnlyBg
    }
    val borderColor = when {
        slot.isCurrent -> CurrentSlotBorder
        slot.isPast -> Color.Transparent
        slot.isEditable -> FutureSlotBorder
        else -> Color.Transparent
    }
    val textColor = if (slot.isPast) ReadOnlyText else TextOnGlass

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 时间标签
        Column(
            modifier = Modifier.width(52.dp).padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slot.timeLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (slot.isCurrent) CompletedGreen else TimeLabelColor
            )
            if (slot.isCurrent) {
                Text("现在", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CompletedGreen)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 分类色条
        if (slot.category != PlanCategory.NONE) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(slot.category.color))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // 输入框 — 自适应高度，无 maxLines 限制
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                if (slot.isEditable) {
                    text = newValue
                    onContentChange(newValue)
                }
            },
            enabled = slot.isEditable,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 40.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocus() else if (!focusState.isFocused) onBlur()
                },
            placeholder = {
                Text(
                    text = if (slot.isEditable) "输入计划..." else if (slot.isPast) "已过时" else "未到时间",
                    color = TextTertiary,
                    fontSize = 13.sp
                )
            },
            textStyle = LocalTextStyle.current.copy(
                color = textColor,
                fontSize = 13.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                disabledTextColor = textColor,
                disabledPlaceholderColor = TextTertiary,
                cursorColor = ButtonPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            singleLine = false
        )

        // 分类按钮
        if (slot.isEditable) {
            IconButton(onClick = onCategoryClick, modifier = Modifier.size(28.dp).padding(top = 8.dp)) {
                Icon(
                    Icons.Default.Label, "分类",
                    tint = if (slot.category != PlanCategory.NONE) Color(slot.category.color) else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==================== 日历弹窗 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(viewModel: PlanViewModel) {
    val calendarDays by viewModel.calendarDays.collectAsState()
    val calendarMonth by viewModel.calendarMonth.collectAsState()

    Dialog(onDismissRequest = { viewModel.hideCalendarDialog() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 月份导航
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "上月", tint = TextPrimary)
                    }
                    Text(
                        calendarMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "下月", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 星期标题
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 日历网格
                val firstDayOfWeek = calendarMonth.atDay(1).dayOfWeek.value // 1=周一
                val emptyCells = firstDayOfWeek - 1

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.heightIn(max = 360.dp),
                    userScrollEnabled = false
                ) {
                    // 前面的空白格
                    items(emptyCells) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                    // 日期格子
                    items(calendarDays) { day ->
                        CalendarDayCell(
                            day = day,
                            onClick = {
                                viewModel.goToDate(day.date)
                                viewModel.hideCalendarDialog()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.hideCalendarDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("关闭") }
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: CalendarDay, onClick: () -> Unit) {
    val bgColor = when {
        day.isToday -> TimeLabelColor.copy(alpha = 0.15f)
        day.hasContent -> Color(0xFFF0F4FF)
        else -> Color.Transparent
    }
    val textColor = when {
        day.isToday -> TimeLabelColor
        day.hasContent -> TextPrimary
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${day.date.dayOfMonth}",
                fontSize = 13.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (day.title.isNotEmpty()) {
                Text(
                    text = day.title.take(4),
                    fontSize = 8.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (day.hasContent) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (day.isToday) TimeLabelColor else ButtonPrimary)
                )
            }
        }
    }
}

// ==================== 复制计划弹窗 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyPlanDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var days by remember { mutableStateOf("7") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ContentCopy, null, tint = ButtonPrimary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("复制计划到未来", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("将当前日期的计划复制到未来若干天\n（已有计划的日期不会覆盖）", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter { c -> c.isDigit() } },
                    label = { Text("天数") },
                    suffix = { Text("天") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("3", "7", "14", "30")) { d ->
                        FilterChip(
                            selected = days == d,
                            onClick = { days = d },
                            label = { Text("${d}天", fontSize = 12.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("取消") }
                    Button(
                        onClick = { onConfirm(days.toIntOrNull() ?: 7) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                    ) { Text("确认复制") }
                }
            }
        }
    }
}

// ==================== 分类选择弹窗 ====================

@Composable
fun CategoryPickerDialog(onSelect: (PlanCategory) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("选择分类", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                PlanCategory.entries.toList().chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onSelect(cat) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(cat.color).copy(alpha = 0.1f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(cat.color)))
                                    Text(cat.label, fontSize = 14.sp, color = TextPrimary)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ==================== 模板弹窗 ====================

@Composable
fun TemplateDialog(viewModel: PlanViewModel) {
    val templates by viewModel.templates.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<com.example.dailyplanner.data.PlanTemplate?>(null) }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除模板「${template.name}」吗？") },
            confirmButton = { TextButton(onClick = { viewModel.deleteTemplate(template); pendingDelete = null }) { Text("删除", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }

    Dialog(onDismissRequest = { viewModel.hideTemplateDialog() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("计划模板", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = { showSaveDialog = true }) { Icon(Icons.Default.Add, "保存为模板", tint = ButtonPrimary) }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (templates.isEmpty()) {
                    Text("暂无模板，点击右上角保存当前计划为模板", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    templates.forEach { template ->
                        key(template.id) {
                            var pendingLoad by remember { mutableStateOf(false) }
                            if (pendingLoad) {
                                AlertDialog(
                                    onDismissRequest = { pendingLoad = false },
                                    title = { Text("加载模板") },
                                    text = { Text("加载「${template.name}」会覆盖当前计划，确定？") },
                                    confirmButton = { TextButton(onClick = { viewModel.loadTemplate(template); pendingLoad = false }) { Text("确定") } },
                                    dismissButton = { TextButton(onClick = { pendingLoad = false }) { Text("取消") } }
                                )
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { pendingLoad = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Bookmark, null, tint = ButtonPrimary, modifier = Modifier.size(18.dp))
                                        Text(template.name, fontSize = 14.sp, color = TextPrimary)
                                    }
                                    IconButton(onClick = { pendingDelete = template }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, "删除", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { viewModel.hideTemplateDialog() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("关闭") }
            }
        }
    }

    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DialogBg)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("保存为模板", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("模板名称") },
                        placeholder = { Text("如：工作日模板") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showSaveDialog = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("取消") }
                        Button(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    viewModel.saveAsTemplate(templateName.trim())
                                    templateName = ""
                                    showSaveDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                        ) { Text("保存") }
                    }
                }
            }
        }
    }
}

// ==================== 搜索弹窗 ====================

@Composable
fun SearchDialog(viewModel: PlanViewModel) {
    val results by viewModel.searchResults.collectAsState()
    var keyword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { viewModel.hideSearchDialog() }) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("搜索计划", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it; viewModel.search(it) },
                    placeholder = { Text("输入关键词搜索...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (keyword.length >= 2 && results.isEmpty()) {
                    Text("正在搜索或未找到匹配内容", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 20.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(count = results.size, key = { "${results[it].date}_${results[it].slotIndex}" }) { idx ->
                        val item = results[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${item.date}  ${String.format("%02d:%02d", item.slotIndex / 2, if (item.slotIndex % 2 == 0) 0 else 30)}",
                                    fontSize = 11.sp, color = TimeLabelColor
                                )
                                Text(item.content, fontSize = 14.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { viewModel.hideSearchDialog() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("关闭") }
            }
        }
    }
}

// ==================== 统计弹窗 ====================

@Composable
fun StatsDialog(viewModel: PlanViewModel) {
    val dayProgress by viewModel.dayProgress.collectAsState()
    val filledCount by viewModel.filledCount.collectAsState()
    val pastSlotCount by viewModel.pastSlotCount.collectAsState()

    Dialog(onDismissRequest = { viewModel.hideStatsDialog() }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, null, tint = ButtonPrimary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("今日统计", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "已填写", "$filledCount", "48", TimeLabelColor)
                    StatCard(Modifier.weight(1f), "任务进度", "$pastSlotCount", "48", CategoryRest)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val taskProgress = pastSlotCount.toFloat() / 48
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("任务进度", fontSize = 12.sp, color = TextSecondary)
                        Text("$pastSlotCount/48", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = taskProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CategoryRest,
                        trackColor = Color(0xFFE8EAED)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val fillProgress = filledCount.toFloat() / 48
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("填写进度", fontSize = 12.sp, color = TextSecondary)
                        Text("$filledCount/48", fontSize = 12.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = fillProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CompletedGreen,
                        trackColor = Color(0xFFE8EAED)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(onClick = { viewModel.hideStatsDialog() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("关闭") }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, total: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            if (total.isNotEmpty()) Text("/ $total", fontSize = 11.sp, color = TextTertiary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
