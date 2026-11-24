package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.domain.model.AppLanguage
import com.example.domain.model.Priority
import com.example.domain.model.RoutineCategory
import com.example.domain.model.TimeBlock
import com.example.domain.model.TimeOfDayBucket
import java.util.Locale

@Immutable
data class AppStrings(
    val isArabic: Boolean,
    val navToday: String,
    val navRoutines: String,
    val navCalendar: String,
    val navInsights: String,
    val navSettings: String,
    // Today
    val todayTitle: String,
    val todaySubtitle: String,
    val quickAddHint: String,
    val addTask: String,
    val templates: String,
    val saveAsTemplate: String,
    val rolloverBannerTitle: String,
    val rolloverBannerAction: String,
    val rolloverAll: String,
    val rolloverSelected: String,
    val dueTasksSection: String,
    val completedSection: String,
    val todayRoutinesSection: String,
    val emptyTodayTitle: String,
    val emptyTodaySubtitle: String,
    val undo: String,
    val taskCompletedMsg: String,
    val subtasksLabel: String,
    val addSubtaskHint: String,
    val dragToReorder: String,
    val focusNow: String,
    val repeatedlyPostponed: String,
    val editTask: String,
    val newTask: String,
    val titleLabel: String,
    val notesLabel: String,
    val priorityLabel: String,
    val timeBlockLabel: String,
    val tagsLabel: String,
    val tagsHint: String,
    val dueDateLabel: String,
    val dueTimeLabel: String,
    val clearTime: String,
    val reminderLabel: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val todayWord: String,
    val tomorrowWord: String,
    val noDateWord: String,
    // Routines
    val routinesTitle: String,
    val newRoutine: String,
    val editRoutine: String,
    val categoryLabel: String,
    val activeDaysLabel: String,
    val pauseRoutine: String,
    val resumeRoutine: String,
    val pausedBadge: String,
    val dayVariationTitle: String,
    val variationHint: String,
    val durationMinutesLabel: String,
    val streakDays: String,
    val graceShieldActive: String,
    val completionRateLabel: String,
    val groupedRemindersInfo: String,
    val testGroupedNotification: String,
    val emptyRoutinesTitle: String,
    val emptyRoutinesSubtitle: String,
    // Calendar
    val calendarTitle: String,
    val viewDay: String,
    val viewWeek: String,
    val viewMonth: String,
    val workloadLight: String,
    val workloadModerate: String,
    val workloadHeavy: String,
    val quickReschedule: String,
    val unscheduledPool: String,
    val assignToSelectedDate: String,
    val emptyCalendarDay: String,
    val emptyUnscheduled: String,
    // Focus
    val focusTitle: String,
    val focusSessionTab: String,
    val breakSessionTab: String,
    val linkedTaskLabel: String,
    val noTaskLinked: String,
    val selectTaskToLink: String,
    val startFocus: String,
    val pauseFocus: String,
    val resumeFocus: String,
    val resetFocus: String,
    val completeLinkedTask: String,
    val flexibleBreak: String,
    val extendFiveMin: String,
    val sessionLogTitle: String,
    val emptySessionLog: String,
    val minutesShort: String,
    // Insights
    val insightsTitle: String,
    val weeklyPeriod: String,
    val monthlyPeriod: String,
    val filterAll: String,
    val filterTasks: String,
    val filterRoutines: String,
    val allTags: String,
    val completionRateCard: String,
    val vsPreviousPeriod: String,
    val focusMinutesCard: String,
    val bestStreakCard: String,
    val dailyTrendTitle: String,
    val timeOfDayTitle: String,
    val postponedAlertsTitle: String,
    val noPostponedAlerts: String,
    val breakIntoSubtasks: String,
    // Settings
    val settingsTitle: String,
    val appearanceSection: String,
    val themeModeLabel: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val darkContrastLabel: String,
    val contrastBalanced: String,
    val contrastHigh: String,
    val contrastOled: String,
    val fontScaleLabel: String,
    val fontPreviewSampleTitle: String,
    val fontPreviewSampleBody: String,
    val minimalistModeLabel: String,
    val minimalistModeDesc: String,
    val languageLabel: String,
    val langSystem: String,
    val langEnglish: String,
    val langArabic: String,
    val notificationsSection: String,
    val notificationPermissionTitle: String,
    val notificationPermissionDesc: String,
    val grantPermission: String,
    val permissionGranted: String,
    val groupedRoutineToggle: String,
    val groupedRoutineDesc: String,
    val smartSnoozeLabel: String
) {
    fun priorityName(p: Priority): String = when (p) {
        Priority.LOW -> if (isArabic) "منخفضة" else "Low"
        Priority.MEDIUM -> if (isArabic) "متوسطة" else "Medium"
        Priority.HIGH -> if (isArabic) "عالية" else "High"
        Priority.URGENT -> if (isArabic) "عاجل" else "Urgent"
    }

    fun timeBlockName(tb: TimeBlock): String = when (tb) {
        TimeBlock.ANYTIME -> if (isArabic) "أي وقت" else "Anytime"
        TimeBlock.MORNING -> if (isArabic) "الصباح" else "Morning"
        TimeBlock.AFTERNOON -> if (isArabic) "الظهيرة" else "Afternoon"
        TimeBlock.EVENING -> if (isArabic) "المساء" else "Evening"
    }

    fun routineCategoryName(cat: RoutineCategory): String = when (cat) {
        RoutineCategory.MORNING -> if (isArabic) "صباحي" else "Morning"
        RoutineCategory.EVENING -> if (isArabic) "مسائي" else "Evening"
        RoutineCategory.STUDY_WORK -> if (isArabic) "دراسة وعمل" else "Study & Work"
        RoutineCategory.CUSTOM -> if (isArabic) "مخصص" else "Custom"
    }

    fun timeOfDayName(bucket: TimeOfDayBucket): String = when (bucket) {
        TimeOfDayBucket.MORNING -> if (isArabic) "الصباح" else "Morning"
        TimeOfDayBucket.AFTERNOON -> if (isArabic) "الظهيرة" else "Afternoon"
        TimeOfDayBucket.EVENING -> if (isArabic) "المساء" else "Evening"
        TimeOfDayBucket.NIGHT -> if (isArabic) "الليل" else "Night"
    }

    fun dayShort(isoDayOfWeek: Int): String = if (isArabic) {
        when (isoDayOfWeek) {
            1 -> "ن"
            2 -> "ث"
            3 -> "ر"
            4 -> "خ"
            5 -> "ج"
            6 -> "س"
            7 -> "ح"
            else -> ""
        }
    } else {
        when (isoDayOfWeek) {
            1 -> "M"
            2 -> "T"
            3 -> "W"
            4 -> "T"
            5 -> "F"
            6 -> "S"
            7 -> "S"
            else -> ""
        }
    }

    fun dayFull(isoDayOfWeek: Int): String = if (isArabic) {
        when (isoDayOfWeek) {
            1 -> "الإثنين"
            2 -> "الثلاثاء"
            3 -> "الأربعاء"
            4 -> "الخميس"
            5 -> "الجمعة"
            6 -> "السبت"
            7 -> "الأحد"
            else -> ""
        }
    } else {
        when (isoDayOfWeek) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }
    }
}

val EnglishStrings = AppStrings(
    isArabic = false,
    navToday = "Today",
    navRoutines = "Routines",
    navCalendar = "Calendar",
    navInsights = "Insights",
    navSettings = "Settings",
    todayTitle = "Today",
    todaySubtitle = "Clear focus, calm flow",
    quickAddHint = "Quick add: e.g. \"Finish report tomorrow 4pm #work !high\"",
    addTask = "Add",
    templates = "Templates",
    saveAsTemplate = "Save Template",
    rolloverBannerTitle = "unfinished tasks from earlier",
    rolloverBannerAction = "Rollover",
    rolloverAll = "Move All to Today",
    rolloverSelected = "Move Selected",
    dueTasksSection = "Today's Tasks",
    completedSection = "Completed",
    todayRoutinesSection = "Today's Routines",
    emptyTodayTitle = "Your day is open",
    emptyTodaySubtitle = "Type naturally above or pick a template to plan your first task.",
    undo = "Undo",
    taskCompletedMsg = "Task completed",
    subtasksLabel = "Subtasks",
    addSubtaskHint = "Add step & press Done...",
    dragToReorder = "Drag to reorder",
    focusNow = "Focus",
    repeatedlyPostponed = "Postponed ×",
    editTask = "Edit Task",
    newTask = "New Task",
    titleLabel = "Title",
    notesLabel = "Notes",
    priorityLabel = "Priority",
    timeBlockLabel = "Time Block",
    tagsLabel = "Tags",
    tagsHint = "Comma separated (e.g. work, study)",
    dueDateLabel = "Date",
    dueTimeLabel = "Time",
    clearTime = "Clear Time",
    reminderLabel = "Reminder",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    todayWord = "Today",
    tomorrowWord = "Tomorrow",
    noDateWord = "Unscheduled",
    routinesTitle = "Routines & Habits",
    newRoutine = "New Routine",
    editRoutine = "Edit Routine",
    categoryLabel = "Category",
    activeDaysLabel = "Active Days",
    pauseRoutine = "Pause",
    resumeRoutine = "Resume",
    pausedBadge = "Paused",
    dayVariationTitle = "Day-Specific Variations",
    variationHint = "Custom note for this day",
    durationMinutesLabel = "Minutes",
    streakDays = "day streak",
    graceShieldActive = "Grace day active",
    completionRateLabel = "Overall",
    groupedRemindersInfo = "Grouped reminders bundle routines within 30m into one calm alert.",
    testGroupedNotification = "Test Grouped Alert",
    emptyRoutinesTitle = "Build steady momentum",
    emptyRoutinesSubtitle = "Create a morning, evening, or study routine with gentle streak protection.",
    calendarTitle = "Calendar & Timeline",
    viewDay = "Day",
    viewWeek = "Week",
    viewMonth = "Month",
    workloadLight = "Light",
    workloadModerate = "Steady",
    workloadHeavy = "Heavy",
    quickReschedule = "Reschedule",
    unscheduledPool = "Unscheduled Pool",
    assignToSelectedDate = "Schedule Here",
    emptyCalendarDay = "No tasks scheduled for this date",
    emptyUnscheduled = "Every task has a place on your timeline",
    focusTitle = "Focus Mode",
    focusSessionTab = "Focus",
    breakSessionTab = "Flexible Break",
    linkedTaskLabel = "Linked Task",
    noTaskLinked = "Free Focus Session",
    selectTaskToLink = "Select Task",
    startFocus = "Start",
    pauseFocus = "Pause",
    resumeFocus = "Resume",
    resetFocus = "Reset",
    completeLinkedTask = "Complete Task",
    flexibleBreak = "Take 5m Break",
    extendFiveMin = "+5m",
    sessionLogTitle = "Session Log",
    emptySessionLog = "Complete a focus sprint to record your session log.",
    minutesShort = "min",
    insightsTitle = "Insights & Analytics",
    weeklyPeriod = "Weekly",
    monthlyPeriod = "Monthly",
    filterAll = "All",
    filterTasks = "Tasks",
    filterRoutines = "Routines",
    allTags = "All Tags",
    completionRateCard = "Completion Rate",
    vsPreviousPeriod = "vs previous period",
    focusMinutesCard = "Focus Time",
    bestStreakCard = "Best Streak",
    dailyTrendTitle = "Daily Completion Trend",
    timeOfDayTitle = "Peak Productivity Hours",
    postponedAlertsTitle = "Repeatedly Postponed",
    noPostponedAlerts = "No repeatedly postponed tasks — great follow-through!",
    breakIntoSubtasks = "Add Subtask / Focus",
    settingsTitle = "Settings & Comfort",
    appearanceSection = "Appearance & Visual Comfort",
    themeModeLabel = "Theme Mode",
    themeSystem = "System",
    themeLight = "Light",
    themeDark = "Dark",
    darkContrastLabel = "Dark Mode Contrast",
    contrastBalanced = "Balanced",
    contrastHigh = "High Contrast",
    contrastOled = "OLED Black",
    fontScaleLabel = "Font Size Scale",
    fontPreviewSampleTitle = "Live Preview: Review architecture notes",
    fontPreviewSampleBody = "Comfortable spacing and legible contrast at your selected scale.",
    minimalistModeLabel = "Minimalist Mode",
    minimalistModeDesc = "Hide extra badges and banners for distraction-free planning",
    languageLabel = "Language & Direction (RTL/LTR)",
    langSystem = "System",
    langEnglish = "English",
    langArabic = "العربية",
    notificationsSection = "Reminders & Smart Snooze",
    notificationPermissionTitle = "Notification Permission",
    notificationPermissionDesc = "Enable reminders for scheduled tasks and grouped routines",
    grantPermission = "Enable",
    permissionGranted = "Enabled",
    groupedRoutineToggle = "Grouped Routine Reminders",
    groupedRoutineDesc = "Bundle nearby routine reminders into one clean notification",
    smartSnoozeLabel = "Default Smart Snooze"
)

val ArabicStrings = AppStrings(
    isArabic = true,
    navToday = "اليوم",
    navRoutines = "الروتين",
    navCalendar = "التقويم",
    navInsights = "الإحصاءات",
    navSettings = "الإعدادات",
    todayTitle = "اليوم",
    todaySubtitle = "تركيز واضح وتدفق هادئ",
    quickAddHint = "إضافة سريعة: مثلاً \"مراجعة التقرير غداً الساعة 5 مساءً #عمل عاجل\"",
    addTask = "إضافة",
    templates = "القوالب",
    saveAsTemplate = "حفظ كقالب",
    rolloverBannerTitle = "مهام غير مكتملة من أيام سابقة",
    rolloverBannerAction = "ترحيل",
    rolloverAll = "ترحيل الكل لليوم",
    rolloverSelected = "ترحيل المحدد",
    dueTasksSection = "مهام اليوم",
    completedSection = "المكتملة",
    todayRoutinesSection = "روتين اليوم",
    emptyTodayTitle = "يومك جاهز للتخطيط",
    emptyTodaySubtitle = "اكتب مهمتك بصياغة طبيعية بالأعلى أو اختر قالباً جاهزاً للبدء.",
    undo = "تراجع",
    taskCompletedMsg = "تم إنجاز المهمة",
    subtasksLabel = "الخطوات الفرعية",
    addSubtaskHint = "أضف خطوة واضغط تم...",
    dragToReorder = "اسحب لإعادة الترتيب",
    focusNow = "تركيز",
    repeatedlyPostponed = "مؤجلة ×",
    editTask = "تعديل المهمة",
    newTask = "مهمة جديدة",
    titleLabel = "العنوان",
    notesLabel = "الملاحظات",
    priorityLabel = "الأولوية",
    timeBlockLabel = "الفترة الزمنية",
    tagsLabel = "الوسوم",
    tagsHint = "مفصولة بفاصلة (مثل: عمل، دراسة)",
    dueDateLabel = "التاريخ",
    dueTimeLabel = "الوقت",
    clearTime = "إزالة الوقت",
    reminderLabel = "التذكير",
    save = "حفظ",
    cancel = "إلغاء",
    delete = "حذف",
    todayWord = "اليوم",
    tomorrowWord = "غداً",
    noDateWord = "غير مجدولة",
    routinesTitle = "الروتين والعادات",
    newRoutine = "روتين جديد",
    editRoutine = "تعديل الروتين",
    categoryLabel = "التصنيف",
    activeDaysLabel = "أيام التكرار",
    pauseRoutine = "إيقاف مؤقت",
    resumeRoutine = "استئناف",
    pausedBadge = "متوقف مؤقتاً",
    dayVariationTitle = "تخصيص حسب اليوم",
    variationHint = "ملاحظة مخصصة لهذا اليوم",
    durationMinutesLabel = "دقيقة",
    streakDays = "أيام متتالية",
    graceShieldActive = "يوم تسامح متاح",
    completionRateLabel = "الإنجاز الكلي",
    groupedRemindersInfo = "يتم تجميع تنبيهات الروتين المتقاربة خلال ٣٠ دقيقة في إشعار واحد مريح.",
    testGroupedNotification = "تجربة الإشعار المجمّع",
    emptyRoutinesTitle = "ابنِ عادات مستدامة",
    emptyRoutinesSubtitle = "أنشئ روتيناً صباحياً أو مسائياً أو دراسياً مع حماية مرنة للسلسلة.",
    calendarTitle = "التقويم والخط الزمني",
    viewDay = "يومي",
    viewWeek = "أسبوعي",
    viewMonth = "شهري",
    workloadLight = "خفيف",
    workloadModerate = "متوازن",
    workloadHeavy = "مزدحم",
    quickReschedule = "إعادة جدولة",
    unscheduledPool = "مهام غير محددة بوقت",
    assignToSelectedDate = "جدولة هنا",
    emptyCalendarDay = "لا توجد مهام مجدولة في هذا اليوم",
    emptyUnscheduled = "كل المهام موزعة على خطك الزمني",
    focusTitle = "وضع التركيز",
    focusSessionTab = "تركيز",
    breakSessionTab = "استراحة مرنة",
    linkedTaskLabel = "المهمة المرتبطة",
    noTaskLinked = "جلسة تركيز حرة",
    selectTaskToLink = "اختر مهمة",
    startFocus = "بدء",
    pauseFocus = "إيقاف مؤقت",
    resumeFocus = "متابعة",
    resetFocus = "إعادة ضبط",
    completeLinkedTask = "إتمام المهمة",
    flexibleBreak = "استراحة ٥ دقائق",
    extendFiveMin = "+٥ د",
    sessionLogTitle = "سجل الجلسات",
    emptySessionLog = "أكمل جلسة تركيز لتسجيلها في السجل الزمني للمهمة.",
    minutesShort = "د",
    insightsTitle = "الإحصاءات والتقارير",
    weeklyPeriod = "أسبوعي",
    monthlyPeriod = "شهري",
    filterAll = "الكل",
    filterTasks = "المهام",
    filterRoutines = "الروتين",
    allTags = "كل الوسوم",
    completionRateCard = "نسبة الإنجاز",
    vsPreviousPeriod = "مقارنة بالفترة السابقة",
    focusMinutesCard = "دقائق التركيز",
    bestStreakCard = "أطول سلسلة",
    dailyTrendTitle = "مؤشر الإنجاز اليومي",
    timeOfDayTitle = "توزيع الإنجاز خلال اليوم",
    postponedAlertsTitle = "مهام متكررة التأجيل",
    noPostponedAlerts = "لا توجد مهام متكررة التأجيل — التزام ممتاز!",
    breakIntoSubtasks = "إضافة خطوات / تركيز",
    settingsTitle = "التخصيص والراحة البصرية",
    appearanceSection = "المظهر وتباين العرض",
    themeModeLabel = "وضع المظهر",
    themeSystem = "النظام",
    themeLight = "فاتح",
    themeDark = "داكن",
    darkContrastLabel = "تباين الوضع الليلي",
    contrastBalanced = "متوازن",
    contrastHigh = "تباين عالٍ",
    contrastOled = "أسود OLED",
    fontScaleLabel = "حجم الخط والمعاينة",
    fontPreviewSampleTitle = "معاينة فورية: مراجعة ملاحظات المشروع",
    fontPreviewSampleBody = "تباعد مريح وقراءة واضحة بالحجم الذي اخترته الآن.",
    minimalistModeLabel = "وضع الواجهة المبسطة",
    minimalistModeDesc = "إخفاء الشارات والبطاقات الثانوية لتركيز أهدأ",
    languageLabel = "اللغة والاتجاه (RTL / LTR)",
    langSystem = "لغة النظام",
    langEnglish = "English",
    langArabic = "العربية",
    notificationsSection = "التنبيهات والتأجيل الذكي",
    notificationPermissionTitle = "إذن الإشعارات",
    notificationPermissionDesc = "تفعيل تنبيهات المهام المجدولة وتجميع الروتين",
    grantPermission = "تفعيل",
    permissionGranted = "مفعّل",
    groupedRoutineToggle = "تجميع إشعارات الروتين",
    groupedRoutineDesc = "دمج تنبيهات الروتين المتقاربة زمنياً في إشعار واحد",
    smartSnoozeLabel = "مدة التأجيل الذكي الافتراضية"
)

fun resolveStrings(language: AppLanguage): AppStrings {
    val useArabic = when (language) {
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language == "ar"
    }
    return if (useArabic) ArabicStrings else EnglishStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }

@Composable
fun appStrings(): AppStrings = LocalStrings.current
