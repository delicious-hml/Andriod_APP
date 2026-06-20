# 时笺恪日

一款 Android 日常计划管理应用，以半小时为单位将一天划分为 48 个时间段，帮助用户精细化管理每日时间。

采用 iOS 26 液态玻璃风格 UI，界面通透明亮。

---

## 功能特性

### 时间段管理
- 一天 48 个时间段（00:00 ~ 23:30），每半小时一格
- 每个时间段可自由输入任意计划内容
- 文本框自适应高度，写多少字显示多少字，不遮盖
- 支持 7 种分类标签：工作 / 学习 / 运动 / 休息 / 社交 / 餐饮 / 其他，不同分类不同颜色标识

### 时间锁机制
- 只能修改当前半小时及以后的时间段
- 过去的时间段自动锁定为只读，显示"已过时"
- 当前时间段高亮绿色边框，标记"现在"
- 每分钟自动刷新可编辑状态

### 日历视图
- 点击顶部日期打开月历弹窗
- 每个日期格子显示：日期数字 + 该日标题（前 4 字）+ 内容标记点
- 今天高亮蓝色，有内容的日期显示蓝色圆点
- 支持左右切换月份，点击某天直接跳转

### 日期导航
- 左右箭头逐天切换
- 点击"今天"快速回到当天
- 支持查看过去和未来的日期（过去只读，未来可编辑）

### 复制计划
- 一键将当前日期的计划复制到未来 N 天
- 支持快捷选择 3 / 7 / 14 / 30 天
- 已有计划的日期自动跳过，不会覆盖
- 复制完成后提示成功天数和跳过天数

### 计划模板
- 将当前计划保存为命名模板（如"工作日模板"、"周末模板"）
- 加载模板时弹出确认对话框，避免误操作
- 删除模板时弹出确认对话框
- 支持多个模板管理

### 搜索
- 搜索所有历史计划内容
- 500ms 防抖，避免频繁查询
- 结果显示日期、时间和内容

### 统计面板
- **已填写 X/48**：今天有多少时间段填了内容
- **任务进度 X/48**：今天已过的时间段数量
- 两个独立进度条，分别用蓝色和橙色标识

### 时间进度
- 顶部栏实时显示"今日已过 XX.XX%"
- 每秒更新，保留两位小数，平滑增长
- 右下角浮动按钮显示当前时间，点击滚动到当前时间段

---

## UI 设计

- iOS 26 液态玻璃风格
- 渐变背景：浅蓝 → 浅紫 → 浅橙
- 半透明卡片，柔和圆角
- 透明状态栏和导航栏
- 清晰通透，不暗沉

---

## 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI 框架 |
| Material3 | 设计组件库 |
| Room | 本地数据库持久化 |
| StateFlow | 响应式状态管理 |
| MVVM | 架构模式 |
| Coroutines + Flow | 异步数据流 |

---

## 项目结构

```
app/src/main/java/com/example/dailyplanner/
├── MainActivity.kt                    # 入口 Activity
├── data/
│   ├── PlanItem.kt                    # Room 实体（计划项 + 分类 + 标题）
│   ├── PlanTemplate.kt                # Room 实体（模板）
│   ├── PlanDao.kt                     # DAO 接口（增删改查 + 搜索 + 月历）
│   └── AppDatabase.kt                 # Room 数据库（含 Migration）
├── ui/
│   ├── PlanScreen.kt                  # 主界面（所有 Composable 组件）
│   │   ├── PlanScreen                 # 主屏幕
│   │   ├── GlassTopBar                # 顶部栏（日期 + 进度 + 功能按钮）
│   │   ├── GlassTimeSlotRow           # 时间段行（文本输入 + 分类）
│   │   ├── CalendarDialog             # 日历弹窗
│   │   ├── CopyPlanDialog             # 复制计划弹窗
│   │   ├── TemplateDialog             # 模板管理弹窗
│   │   ├── SearchDialog               # 搜索弹窗
│   │   ├── StatsDialog                # 统计弹窗
│   │   └── CategoryPickerDialog       # 分类选择弹窗
│   └── theme/
│       ├── Color.kt                   # 颜色定义
│       └── Theme.kt                   # Material3 主题 + 透明状态栏
└── viewmodel/
    └── PlanViewModel.kt               # 业务逻辑 ViewModel
```

---

## 架构设计

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐
│  PlanScreen  │────>│  PlanViewModel  │────>│   Room DAO   │
│  (Compose)   │<────│  (StateFlow)    │<────│  (Flow)      │
└─────────────┘     └─────────────────┘     └──────────────┘
```

- **单向数据流**：用户输入 → ViewModel → Room → Flow → UI 更新
- **输入防抖**：300ms 防抖写入 Room，避免每次按键都写库
- **编辑态隔离**：正在编辑的 slot 不受 Room Flow 回写影响，避免光标跳动
- **搜索防抖**：500ms 防抖查询，避免频繁数据库操作

---

## 数据库设计

### plan_items 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| date | String | 日期，格式 "2026-06-21" |
| slotIndex | Int | 时间段索引 0~47 |
| content | String | 计划内容 |
| category | String | 分类标识 |
| title | String | 日期标题 |

唯一索引：`(date, slotIndex)`

### plan_templates 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Int | 自增主键 |
| name | String | 模板名称 |
| slotsJson | String | JSON 格式的计划数据 |

### 数据库版本

- v1 → v2：新增 `title` 字段，删除 `isCompleted` 字段（含 Migration）

---

## 构建与运行

### 环境要求

- Android Studio Hedgehog (2023.1) 或更高版本
- JDK 17
- Android SDK 34
- 设备最低 Android 8.0 (API 26)

### 使用 Android Studio

1. 打开 `DailyPlanner` 文件夹
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 运行

### 使用命令行

```bash
# 编译 Debug APK
gradle assembleDebug

# 签名
zipalign -f -v 4 app/build/outputs/apk/debug/app-debug.apk app-aligned.apk
apksigner sign --ks release-key.jks --ks-pass pass:yourpassword --out app-release.apk app-aligned.apk
```

### 首次构建注意

项目不包含 `local.properties`（因为每台电脑 SDK 路径不同）。首次用 Android Studio 打开时会自动生成。命令行构建需手动创建：

```bash
echo "sdk.dir=/path/to/your/Android/Sdk" > local.properties
```

---

## 安装说明

1. 下载 `DailyPlanner-final.apk`
2. 传到手机（建议用 USB 数据线，不要用 QQ 传，可能损坏文件）
3. 点击安装，允许"未知来源"安装
4. 如提示安装失败，先卸载旧版本并清除应用数据

---

## 权限说明

本应用**不需要任何权限**：
- 无网络访问
- 无存储权限
- 无相机/麦克风
- 所有数据仅存储在本地 Room 数据库

---

## 已知限制

- 仅支持竖屏
- 不支持深色模式（仅浅色主题）
- 不支持数据导出/导入
- 不支持多设备同步
- 日历视图不支持农历

---

## 更新日志

### v1.0 (2026-06-21)
- 初始版本
- 48 个时间段管理
- 时间锁机制
- 日历视图
- 复制计划到未来 N 天
- 计划模板管理
- 搜索功能
- 统计面板
- 分类标签
- iOS 26 液态玻璃风格 UI

---

## 许可证

本项目仅供学习交流使用。
