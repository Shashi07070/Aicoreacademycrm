package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Course
import com.example.data.model.Student
import com.example.ui.TuitionViewModel
import com.example.ui.dialogs.AddCourseDialog
import com.example.ui.dialogs.AddStudentDialog
import com.example.ui.dialogs.AdminSettingsDialog
import com.example.ui.dialogs.RecordFeeDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeesScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.StudentDetailSheet
import com.example.ui.screens.StudentsScreen
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: TuitionViewModel = viewModel()
                TuitionAppRoot(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    COURSES("Courses", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    STUDENTS("Students", Icons.Filled.People, Icons.Outlined.People),
    FEES("Fees", Icons.Filled.Payments, Icons.Outlined.Payments),
    ATTENDANCE("Attendance", Icons.Filled.EventNote, Icons.Outlined.EventNote),
    SCHEDULE("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionAppRoot(viewModel: TuitionViewModel) {
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Dialog & Sheet States
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showRecordFeeDialog by remember { mutableStateOf(false) }
    var showAdminSettingsDialog by remember { mutableStateOf(false) }

    var studentForFeeRecord by remember { mutableStateOf<Student?>(null) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var activeStudentDetail by remember { mutableStateOf<Student?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    AnimatedContent(
        targetState = isLocked,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "LockTransition"
    ) { locked ->
        if (locked) {
            LockScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color.White,
                        modifier = Modifier.width(310.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Slide-out Menu Header: "AI CORE ACADEMY" Logo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "AI CORE ACADEMY",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF111827)
                                    )
                                    Text(
                                        text = "Educational CRM • Control Vault",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFFE5E7EB),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Text(
                                text = "CORE NAVIGATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )

                            // Navigation Items with simple outline icons: Dashboard, Courses, Students, Fees, Attendance, Schedule
                            NavigationTab.values().forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) BrandRed else Color(0xFF4B5563)
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        selectedTab = tab
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = BrandRedLight,
                                        selectedTextColor = BrandRed,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedTextColor = Color(0xFF374151)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(vertical = 3.dp)
                                        .testTag("drawer_nav_${tab.name.lowercase()}")
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            HorizontalDivider(
                                color = Color(0xFFE5E7EB),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Footer: Admin Security Info & Lock Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SESSION: SECURE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF16A34A)
                                    )
                                    Text(
                                        text = "Master Admin (admin)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            showAdminSettingsDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Security,
                                            contentDescription = "Security",
                                            tint = Color(0xFF6B7280),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            viewModel.lock()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = "Lock CRM",
                                            tint = BrandRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "AI CORE ACADEMY",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.3.sp
                                            ),
                                            color = Color(0xFF111827)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = BrandRedLight,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = selectedTab.title.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                ),
                                                color = BrandRed,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    // Slide-out Menu Trigger Button
                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Open Sidebar Menu",
                                            tint = Color(0xFF111827)
                                        )
                                    }
                                },
                                actions = {
                                    // Admin Security Settings Button
                                    IconButton(
                                        onClick = { showAdminSettingsDialog = true },
                                        modifier = Modifier.testTag("admin_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Admin Security Settings",
                                            tint = Color(0xFF6B7280),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Quick Lock Button
                                    IconButton(
                                        onClick = { viewModel.lock() },
                                        modifier = Modifier.testTag("quick_lock_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Lock CRM",
                                            tint = BrandRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.White
                                )
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        }
                    },
                    bottomBar = {
                        if (!isExpandedScreen) {
                            Column {
                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                                NavigationBar(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                                    containerColor = Color.White,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationTab.values().forEach { tab ->
                                        val isSelected = selectedTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { selectedTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = BrandRed,
                                                selectedTextColor = BrandRed,
                                                indicatorColor = BrandRedLight,
                                                unselectedIconColor = Color(0xFF6B7280),
                                                unselectedTextColor = Color(0xFF6B7280)
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        when (selectedTab) {
                            NavigationTab.STUDENTS, NavigationTab.DASHBOARD -> {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        editingStudent = null
                                        showAddStudentDialog = true
                                    },
                                    containerColor = BrandRed,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    text = { Text("Enroll Student", fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("add_student_fab")
                                )
                            }
                            NavigationTab.COURSES -> {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        editingCourse = null
                                        showAddCourseDialog = true
                                    },
                                    containerColor = BrandRed,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    text = { Text("Add Course", fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("add_course_fab")
                                )
                            }
                            NavigationTab.FEES -> {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        studentForFeeRecord = null
                                        showRecordFeeDialog = true
                                    },
                                    containerColor = BrandRed,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                    text = { Text("Record Fee", fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("record_fee_fab")
                                )
                            }
                            else -> Unit
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpandedScreen) {
                            NavigationRail(
                                modifier = Modifier.fillMaxHeight(),
                                containerColor = Color.White
                            ) {
                                NavigationTab.values().forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) BrandRed else Color(0xFF6B7280)
                                            )
                                        },
                                        label = { Text(tab.title) },
                                        modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                NavigationTab.DASHBOARD -> {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToStudents = { selectedTab = NavigationTab.STUDENTS },
                                        onNavigateToCourses = { selectedTab = NavigationTab.COURSES },
                                        onNavigateToFees = { selectedTab = NavigationTab.FEES },
                                        onNavigateToAttendance = { selectedTab = NavigationTab.ATTENDANCE },
                                        onAddStudentClick = {
                                            editingStudent = null
                                            showAddStudentDialog = true
                                        },
                                        onAddCourseClick = {
                                            editingCourse = null
                                            showAddCourseDialog = true
                                        },
                                        onRecordFeeClick = { student ->
                                            studentForFeeRecord = student
                                            showRecordFeeDialog = true
                                        },
                                        onStudentClick = { student ->
                                            activeStudentDetail = student
                                        }
                                    )
                                }
                                NavigationTab.COURSES -> {
                                    CoursesScreen(
                                        viewModel = viewModel,
                                        onEditCourse = { course ->
                                            editingCourse = course
                                            showAddCourseDialog = true
                                        }
                                    )
                                }
                                NavigationTab.STUDENTS -> {
                                    StudentsScreen(
                                        viewModel = viewModel,
                                        onStudentClick = { student ->
                                            activeStudentDetail = student
                                        }
                                    )
                                }
                                NavigationTab.FEES -> {
                                    FeesScreen(
                                        viewModel = viewModel,
                                        onRecordFeeClick = { student ->
                                            studentForFeeRecord = student
                                            showRecordFeeDialog = true
                                        },
                                        onStudentClick = { student ->
                                            activeStudentDetail = student
                                        }
                                    )
                                }
                                NavigationTab.ATTENDANCE -> {
                                    AttendanceScreen(viewModel = viewModel)
                                }
                                NavigationTab.SCHEDULE -> {
                                    ScheduleScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            courses = courses,
            editingStudent = editingStudent,
            onDismiss = {
                showAddStudentDialog = false
                editingStudent = null
            },
            onSaveStudent = { name, courseId, phone, email, address, totalFee, initialPayment, paymentMode, notes ->
                viewModel.addStudent(
                    name = name,
                    courseId = courseId,
                    phone = phone,
                    email = email,
                    address = address,
                    totalFee = totalFee,
                    initialPayment = initialPayment,
                    paymentMode = paymentMode,
                    notes = notes
                )
            },
            onUpdateStudent = { updatedStudent ->
                viewModel.updateStudent(updatedStudent)
            }
        )
    }

    // Add / Edit Course Dialog
    if (showAddCourseDialog) {
        AddCourseDialog(
            editingCourse = editingCourse,
            onDismiss = {
                showAddCourseDialog = false
                editingCourse = null
            },
            onSaveCourse = { name, subject, feeAmount, timingSchedule, description ->
                viewModel.addCourse(
                    name = name,
                    subject = subject,
                    feeAmount = feeAmount,
                    timingSchedule = timingSchedule,
                    description = description
                )
            },
            onUpdateCourse = { updatedCourse ->
                viewModel.updateCourse(updatedCourse)
            }
        )
    }

    // Record Fee Dialog
    if (showRecordFeeDialog) {
        RecordFeeDialog(
            students = students,
            preselectedStudent = studentForFeeRecord,
            onDismiss = {
                showRecordFeeDialog = false
                studentForFeeRecord = null
            },
            onRecordFee = { studentId, amount, paymentMode, receiptNo, notes ->
                viewModel.recordPayment(
                    studentId = studentId,
                    amount = amount,
                    paymentMode = paymentMode,
                    receiptNo = receiptNo,
                    notes = notes
                )
            }
        )
    }

    // Admin Settings Dialog
    if (showAdminSettingsDialog) {
        AdminSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showAdminSettingsDialog = false }
        )
    }

    // Student Detail Bottom Sheet
    if (activeStudentDetail != null) {
        val currentDetailStudent = students.find { it.id == activeStudentDetail?.id } ?: activeStudentDetail!!
        val currentCourse = courses.find { it.id == currentDetailStudent.courseId }
        StudentDetailSheet(
            student = currentDetailStudent,
            course = currentCourse,
            sheetState = bottomSheetState,
            viewModel = viewModel,
            onDismiss = { activeStudentDetail = null },
            onEditStudent = { student ->
                editingStudent = student
                showAddStudentDialog = true
            },
            onRecordPayment = { student ->
                studentForFeeRecord = student
                showRecordFeeDialog = true
            }
        )
    }
}
