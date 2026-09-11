package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen navigation states
enum class AppScreen {
    MAIN,
    PASSCODE_LOCK,   // Ask passcode to access Version Management
    PASSCODE_RE_ENTER, // Re-authenticate passcode right before saving changes
    PASSWORD_SETUP,  // Config new passcode if not defined
    VERSION_MANAGEMENT,
    USER_GUIDE,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableIntStateOf(SystemManager.getThemeMode(context)) }
            
            val isDark = when (themeMode) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        KosherManagerApp(
                            currentThemeMode = themeMode,
                            onThemeChanged = { newMode ->
                                SystemManager.setThemeMode(context, newMode)
                                themeMode = newMode
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KosherManagerApp(currentThemeMode: Int, onThemeChanged: (Int) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize defaults on startup
    LaunchedEffect(Unit) {
        SystemManager.initializeDefaults(context)
        if (!SystemManager.isSimulationMode(context)) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SystemManager.requestRootPermission()
            }
        }
    }

    // App state
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordSetupState by remember { mutableStateOf("") }
    var passwordSetupConfirmState by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(SystemManager.fetchDeviceStatus(context)) }
    
    // State of selected inputs in Version Management config
    var selectedPresetConfig by remember { mutableIntStateOf(1) } // 1, 2, or 3
    var googleServicesConfig by remember { mutableStateOf(true) }
    
    // Drawer side-draw visibility
    var drawerOpen by remember { mutableStateOf(false) }
    
    // Popup dialogue controls
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessRebootDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var isApplyingChangesState by remember { mutableStateOf(false) }
    
    // Tracks screen history to go back safely using system back gesture
    val screenHistory = remember { mutableStateListOf<AppScreen>() }
    var isSimState by remember { mutableStateOf(SystemManager.isSimulationMode(context)) }
    
    fun navigateBack() {
        when (currentScreen) {
            AppScreen.PASSCODE_RE_ENTER -> currentScreen = AppScreen.VERSION_MANAGEMENT
            AppScreen.PASSWORD_SETUP -> currentScreen = AppScreen.MAIN
            else -> currentScreen = AppScreen.MAIN
        }
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
        drawerOpen = false
    }

    BackHandler(enabled = drawerOpen || currentScreen != AppScreen.MAIN) {
        if (drawerOpen) {
            drawerOpen = false
        } else {
            navigateBack()
        }
    }

    // Fetch device state regularly when main displays
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.MAIN) {
            val s = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SystemManager.fetchDeviceStatus(context)
            }
            status = s
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Header Toolbar representation
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side: Drawer Burger button (three lines) or Back button
                IconButton(
                    onClick = {
                        if (currentScreen == AppScreen.MAIN) {
                            drawerOpen = true
                        } else {
                            navigateBack()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                ) {
                    if (currentScreen == AppScreen.MAIN) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "פתח וילון צד",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "חזור",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Title center depending on active view
                val screenHeaderTitle = when (currentScreen) {
                    AppScreen.MAIN -> "בית"
                    AppScreen.PASSCODE_LOCK -> "נעילת מנהל"
                    AppScreen.PASSCODE_RE_ENTER -> "אימות נוסף"
                    AppScreen.PASSWORD_SETUP -> "הגדרת סיסמה"
                    AppScreen.VERSION_MANAGEMENT -> "ניהול גירסה כשרה"
                    AppScreen.USER_GUIDE -> "מדריך שימוש"
                    AppScreen.SETTINGS -> "הגדרות האפליקציה"
                    AppScreen.ABOUT -> "אודות המערכת"
                }
                
                Text(
                    text = screenHeaderTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif
                )

                // Left side: Small Circle button for quick About screen opening
                IconButton(
                    onClick = { navigateTo(AppScreen.ABOUT) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "אודות",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Screen switcher displaying beautifully with crossfade transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "ScreenSwitchAnimation"
                ) { screen ->
                    when (screen) {
                        AppScreen.MAIN -> MainDashboardScreen(
                            status = status,
                            onExploreGuide = { navigateTo(AppScreen.USER_GUIDE) },
                            onConfigureVersion = {
                                val hasPassword = SystemManager.getPassword(context) != null
                                if (hasPassword) {
                                    passwordInput = ""
                                    navigateTo(AppScreen.PASSCODE_LOCK)
                                } else {
                                    passwordSetupState = ""
                                    passwordSetupConfirmState = ""
                                    navigateTo(AppScreen.PASSWORD_SETUP)
                                }
                            }
                        )
                        AppScreen.PASSCODE_LOCK -> PasscodeVerificationScreen(
                            passwordValue = passwordInput,
                            onValueChange = { passwordInput = it },
                            onCancel = { navigateBack() },
                            onVerify = {
                                val savedPassword = SystemManager.getPassword(context)
                                if (passwordInput == savedPassword) {
                                    // Successfully authenticated, navigate to setup
                                    val devStatus = SystemManager.fetchDeviceStatus(context)
                                    selectedPresetConfig = if (devStatus.currentPreset != -1) devStatus.currentPreset else 1
                                    googleServicesConfig = devStatus.googleServicesEnabled
                                    navigateTo(AppScreen.VERSION_MANAGEMENT)
                                } else {
                                    Toast.makeText(context, "סיסמה לא נכונה, אנא נסה שוב", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        AppScreen.PASSCODE_RE_ENTER -> PasscodeVerificationScreen(
                            passwordValue = passwordInput,
                            onValueChange = { passwordInput = it },
                            subtitleMessage = "אנא הקש קוד ניהול שוב על מנת לאשר ולהחיל את השינויים במערכת:",
                            onCancel = { navigateBack() },
                            onVerify = {
                                val savedPassword = SystemManager.getPassword(context)
                                if (passwordInput == savedPassword) {
                                    // Trigger applying changes loader view
                                    isApplyingChangesState = true
                                    navigateTo(AppScreen.MAIN)
                                    showSaveConfirmDialog = false
                                    
                                    coroutineScope.launch {
                                        delay(1500) // Aesthetic delay for progress feel
                                        val runResult = SystemManager.saveAndApplyChanges(
                                            context = context,
                                            selectedPreset = selectedPresetConfig,
                                            enableGoogleServices = googleServicesConfig
                                        )
                                        isApplyingChangesState = false
                                        if (runResult.first) {
                                            showSuccessRebootDialog = true
                                        } else {
                                            showErrorDialog = runResult.second ?: "שגיאה לא ידועה במהלך כתיבת קבצי המערכת."
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "סיסמה שגויה. פעולת השמירה בוטלה.", Toast.LENGTH_LONG).show()
                                    navigateBack()
                                }
                            }
                        )
                        AppScreen.PASSWORD_SETUP -> PasswordConfigScreen(
                            passValue = passwordSetupState,
                            confirmValue = passwordSetupConfirmState,
                            onPassChange = { passwordSetupState = it },
                            onConfirmChange = { passwordSetupConfirmState = it },
                            onSave = {
                                if (passwordSetupState.length < 4) {
                                    Toast.makeText(context, "הסיסמה חייבת להיות לפחות 4 תווים", Toast.LENGTH_SHORT).show()
                                } else if (passwordSetupState != passwordSetupConfirmState) {
                                    Toast.makeText(context, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show()
                                } else {
                                    SystemManager.setPassword(context, passwordSetupState)
                                    Toast.makeText(context, "סיסמת מנהל הוגדרה בהצלחה!", Toast.LENGTH_SHORT).show()
                                    // Navigate to Version setup
                                    val devStatus = SystemManager.fetchDeviceStatus(context)
                                    selectedPresetConfig = if (devStatus.currentPreset != -1) devStatus.currentPreset else 1
                                    googleServicesConfig = devStatus.googleServicesEnabled
                                    navigateTo(AppScreen.VERSION_MANAGEMENT)
                                }
                            }
                        )
                        AppScreen.VERSION_MANAGEMENT -> VersionManagementScreen(
                            currentPreset = selectedPresetConfig,
                            onPresetSelected = { selectedPresetConfig = it },
                            googleEnabled = googleServicesConfig,
                            onGoogleToggled = { googleServicesConfig = it },
                            onSavePressed = {
                                showSaveConfirmDialog = true
                            },
                            onCancel = { navigateBack() }
                        )
                        AppScreen.USER_GUIDE -> UserGuideScreen()
                        AppScreen.SETTINGS -> SettingsScreen(
                            onConfirmResetPassword = { current, newPass ->
                                val savedPassword = SystemManager.getPassword(context)
                                if (savedPassword == null || current == savedPassword) {
                                    SystemManager.setPassword(context, newPass)
                                    true
                                } else {
                                    false
                                }
                            },
                            isSimulation = isSimState,
                            onSimulationToggled = { enabled ->
                                SystemManager.setSimulationMode(context, enabled)
                                isSimState = enabled
                                Toast.makeText(context, if (enabled) "מצב סימולציה הופעל" else "מצב פעולה הופעל", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    if (!enabled) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            SystemManager.requestRootPermission()
                                        }
                                    }
                                    val newStatus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        SystemManager.fetchDeviceStatus(context)
                                    }
                                    status = newStatus
                                }
                            },
                            currentThemeMode = currentThemeMode,
                            onThemeChanged = onThemeChanged
                        )
                        AppScreen.ABOUT -> AboutScreen()
                    }
                }
            }
        }

        // Action loading block window during writes
        if (isApplyingChangesState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "מבצע שינויים- המתן...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "התוכנה משכתבת את קבצי המחיצה הכשרה ורושמת שינויי הגדרה לשורש המערכת.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Custom Drawer Curtain (וילון צד) Sliding over content elegant RTL Drawer
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280))
        ) {
            // Sliding shade background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { drawerOpen = false }
            ) {
                // actual drawer sheet panel align right (Hebrew layout right-anchored)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)))
                        .align(Alignment.CenterStart) // CenterStart aligns to Right in Hebrew layout context
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(vertical = 36.dp, horizontal = 20.dp)
                    ) {
                        Column {
                            Text(
                                text = "תפריט מנהלים",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "מערכת הגדרה כשרה - M36",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Menu items
                    val drawerScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .verticalScroll(drawerScrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Large row 1: Version Management
                        DrawerLargeItem(
                            title = "ניהול גירסה כשרה",
                            subtitle = "שינוי פרמטרים והרשאות מערכת",
                            icon = Icons.Default.Lock,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val hasPassword = SystemManager.getPassword(context) != null
                                if (hasPassword) {
                                    passwordInput = ""
                                    navigateTo(AppScreen.PASSCODE_LOCK)
                                } else {
                                    passwordSetupState = ""
                                    passwordSetupConfirmState = ""
                                    navigateTo(AppScreen.PASSWORD_SETUP)
                                }
                            }
                        )

                        // Large row 2: User Guide
                        DrawerLargeItem(
                            title = "מדריך שימוש מפורט",
                            subtitle = "הוראות שימוש, באגים והתקנה פנימית",
                            icon = Icons.Default.PlayArrow,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = { navigateTo(AppScreen.USER_GUIDE) }
                        )

                        // Large row 3: App Store
                        DrawerLargeItem(
                            title = "חנות האפליקציות",
                            subtitle = "התקנת תוכנות מאושרות ממאגר כשר",
                            icon = Icons.Default.ShoppingCart,
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                drawerOpen = false
                                launchOfflineStore(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(30.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )

                        // Small row 1: Settings
                        DrawerSmallItem(
                            title = "הגדרות האפליקציה",
                            icon = Icons.Default.Settings,
                            onClick = { navigateTo(AppScreen.SETTINGS) }
                        )

                        // Small row 2: About
                        DrawerSmallItem(
                            title = "אודות / יצירת קשר",
                            icon = Icons.Default.Info,
                            onClick = { navigateTo(AppScreen.ABOUT) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Save Confirmation dialog
        if (showSaveConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showSaveConfirmDialog = false },
                title = {
                    Text(
                        text = "תתבצע שמירת שינויים",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                text = {
                    Text(
                        text = "המערכת תבצע שינוי קבצים ב-System והמכשיר יופעל מחדש מיד בסיום. האם להמשיך להחלת השינויים?",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            // Close this confirmation and prompt passcode again before writing
                            passwordInput = ""
                            navigateTo(AppScreen.PASSCODE_RE_ENTER)
                            showSaveConfirmDialog = false
                        }
                    ) {
                        Text("המשך", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSaveConfirmDialog = false }
                    ) {
                        Text("ביטול", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }

        // Reboot / Success Dialog
        if (showSuccessRebootDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessRebootDialog = false },
                title = {
                    Text(
                        text = "שינויי קבצים בוצעו בהצלחה!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "המערכת החילה את כל השינויים בצורה מקיפה ותקינה.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "כעת יבוצע אתחול מערכת (Reboot) על מנת לטעון את קבצי הסיסטם מחדש.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (SystemManager.isSimulationMode(context)) {
                            Text(
                                text = "* הערה: מכיוון שהדמיה פעילה, האפליקציה תדמה כעת אתחול בלבד.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        onClick = {
                            showSuccessRebootDialog = false
                            // Reboot
                            val ok = SystemManager.rebootDevice(context)
                            if (!ok) {
                                Toast.makeText(context, "שגיאה בביצוע אתחול (Reboot).", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                    Text("הפעל מחדש כעת", color = Color.Black)
                    }
                }
            )
        }

        // Error Dialog
        showErrorDialog?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { showErrorDialog = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "שגיאת מערכת ואיטיות רוט",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Right
                        )
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "שגיאה",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = errorMsg,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = { showErrorDialog = null }
                    ) {
                        Text("הבנתי", color = Color.White)
                    }
                }
            )
        }
    }
}

// Launches com.example.offlinestore1 application
fun launchOfflineStore(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.m36.store") ?: context.packageManager.getLaunchIntentForPackage("com.example.offlinestore1")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            // Fallback web url or informative Toast
            Toast.makeText(context, "חנות האפליקציות הכשרה חסרה או אינה מותקנת על מכשיר זה.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "נכשל בפתיחת חנות אפליקציות.", Toast.LENGTH_SHORT).show()
    }
}

// Dashboard component for the landing screen
@Composable
fun MainDashboardScreen(
    status: SystemManager.DeviceStatus,
    onExploreGuide: () -> Unit,
    onConfigureVersion: () -> Unit
) {
    val context = LocalContext.current
    val isSim = SystemManager.isSimulationMode(context)
    val coroutineScope = rememberCoroutineScope()
    
    // Quick interactive Guide elements built within the dashboard for visual fidelity
    val quickTabs = listOf("הקדמה", "הגדרה", "מידע", "באגים")
    var activeQuickTab by remember { mutableIntStateOf(0) }
    
    val quickTabTexts = listOf(
        "ברוכים הבאים למשחז הניהול הסיסטמי של נגן M36 הכשר שלך. המערכת מותקנת ומוגדרת ברמת הלינוקס והמחיצה הכשרה, ומאפשרת שליטה מלאה על תצורות המכשיר.",
        "באמצעות תפריט 'ניהול גירסה' מוגן הסיסמה, תוכל לקבוע את חוקי החסימה וההרשאות של המכשיר. החלת הגדרות משכתבת מחדש קבצי ROM קריטיים.",
        "המערכת משפרת כשרות על ידי הגדרות APK דינמיות: הפיכת קבצי וידאו ל-.apkr לא פעילים והגבלת ה-PackageInstaller כדי למנוע התקנות חיצוניות.",
        "במידה והשינויים לא פועלים, ודא תמיד שהמכשיר מותקן עם הרשאות מנהל מערכת רוט (Kernelsu / Magisk Daemon) מלאות, או העבר למצב הדמיה בהגדרות."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcoming Majestic Geometric Balance Typography Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ברוכים הבאים!",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface, // Deep high-contrast teal
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "נגן M36 כשר",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary, // Primary teal accent
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Section A: System Parameters Glassmorphic Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "פרמטרים של המערכת",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Version indicators (matching the exact pills from the design HTML)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VersionStateLine(
                            label = "סטטוס וידאו:",
                            isActive = status.videoEnabled,
                            activeLabel = "וידאו פעיל ומותר",
                            inactiveLabel = "ללא וידאו",
                            isGreenPositive = false // In our kosher concept, no video is protected (which we map visually)
                        )
                        
                        VersionStateLine(
                            label = "מדיניות התקנות:",
                            isActive = status.installationsEnabled,
                            activeLabel = "מאופשר",
                            inactiveLabel = "חסום",
                            isGreenPositive = false
                        )

                        VersionStateLine(
                            label = "שירותי גוגל:",
                            isActive = status.googleServicesEnabled,
                            activeLabel = "פעיל",
                            inactiveLabel = "מוסתר",
                            isGreenPositive = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Version ROM details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "גירסת מערכת:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "v0.0.1_beta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulator state label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "סביבת עבודה ריצה:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val capsuleBg = if (isSim) MaterialTheme.colorScheme.tertiaryContainer else if (status.isRooted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                        val capsuleColor = if (isSim) MaterialTheme.colorScheme.onTertiaryContainer else if (status.isRooted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        val capsuleLabel = if (isSim) "הדמיית סימולטור" else if (status.isRooted) "רוט (Root) מזהה" else "אין רוט (גישה נעולה)"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = capsuleBg),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, capsuleColor.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = capsuleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = capsuleColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section B: Interactive Quick Guide Bento Card directly representing HTML UI
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "מדריך מהיר",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Interactive Tab row representing exact Design HTML look
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(BorderStroke(0.dp, Color.Transparent)), // clear underline in items
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickTabs.forEachIndexed { idx, tab ->
                            val isActive = (activeQuickTab == idx)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeQuickTab = idx }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(3.dp)
                                        .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                )
                            }
                        }
                    }

                    // Explanatory Paragraph text
                    AnimatedContent(
                        targetState = quickTabTexts[activeQuickTab],
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "TabContentAnimation"
                    ) { text ->
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Read More buttons and launcher bullets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini layered avatar bubbles from the Geometric HTML
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }

                        // Text link / indicator button
                        TextButton(
                            onClick = onExploreGuide,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "המשך קריאה ←",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section C: Wide Primary Launch Access Actions
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Button with custom HTML gradient style representing entry
                Button(
                    onClick = onConfigureVersion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary, // Teal 600
                                        MaterialTheme.colorScheme.secondary  // Teal 400
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "כניסה למרחב 'ניהול גירסה כשרה'",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Footer lines mirroring the HTML footers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "מערכת ניהול גרסה - M36 KOSHER EDITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "© כל הזכויות שמורות למפתח י.מ. מאואס",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

// Customized State Component Line mirroring the exact pills from the design HTML
@Composable
fun VersionStateLine(
    label: String,
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    isGreenPositive: Boolean = true
) {
    // Standard color states from the HTML
    val badgeBg = if (isActive == isGreenPositive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
    val badgeText = if (isActive == isGreenPositive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(badgeBg)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isActive) activeLabel else inactiveLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = badgeText,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Passcode numeric entry screen protecting Version Management
@Composable
fun PasscodeVerificationScreen(
    passwordValue: String,
    onValueChange: (String) -> Unit,
    subtitleMessage: String = "כדי לגשת לאפשרויות ניהול המערכת, אנא הקלד את סיסמת מנהל המערכת שלך:",
    onCancel: () -> Unit,
    onVerify: () -> Unit
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .verticalScroll(scrollState)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "מנעול",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "נדרש אימות סיסמה",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitleMessage,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordValue,
                onValueChange = onValueChange,
                label = { Text("סיסמת מנהל") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Confirm button
                Button(
                    onClick = onVerify,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("אמת סיסמה", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Dismiss button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ביטול", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// Configures management passcode when first run or no passcode saved yet
@Composable
fun PasswordConfigScreen(
    passValue: String,
    confirmValue: String,
    onPassChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .verticalScroll(scrollState)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "קביעת סיסמה",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "הגדרת סיסמה ראשונית",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "טרם נקבעה סיסמה לניהול הנגן הכשר. אנא קבע סיסמה ספרתית ייחודית כעת כדי למנוע יציאה בלתי רצויה מחוקי הכשרות:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = passValue,
                onValueChange = onPassChange,
                label = { Text("הזן סיסמה חדשה (מינימום 4 ספרות)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmValue,
                onValueChange = onConfirmChange,
                label = { Text("אמת את הסיסמה החדשה") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("שמור והמשך לניהול", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Version Management Customizer Screen (with 3 single choices and 1 additive checkbox toggle)
@Composable
fun VersionManagementScreen(
    currentPreset: Int,
    onPresetSelected: (Int) -> Unit,
    googleEnabled: Boolean,
    onGoogleToggled: (Boolean) -> Unit,
    onSavePressed: () -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "בחר תצורת גרסה (לבחירה אחת בלבד):",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Choice 1: With video and installations
                val isSelected1 = (currentPreset == 1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetSelected(1) }
                        .border(
                            width = if (isSelected1) 2.dp else 1.dp,
                            color = if (isSelected1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isSelected1) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected1,
                            onClick = { onPresetSelected(1) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "בחירה אחת: עם וידאו והתקנות",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "מנהל קבצים עם וידאו מובנה, עורך קבצי שירים ומאפשר התקנות חופשיות במערכת (מעביר מנתיב 7 לנתיב 6).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Choice 2: With video, no installations
                val isSelected2 = (currentPreset == 2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetSelected(2) }
                        .border(
                            width = if (isSelected2) 2.dp else 1.dp,
                            color = if (isSelected2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isSelected2) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected2,
                            onClick = { onPresetSelected(2) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "בחירה שניה: עם וידאו ללא התקנות",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "נגן עם וידאו פתוח ועורך שירים מאופשר, אך חסום לחלוטין לכל התקנה APK חיצונית).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Choice 3: Without video, without installations
                val isSelected3 = (currentPreset == 3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetSelected(3) }
                        .border(
                            width = if (isSelected3) 2.dp else 1.dp,
                            color = if (isSelected3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isSelected3) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected3,
                            onClick = { onPresetSelected(3) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "בחירה שלישית: ללא וידאו ללא התקנות",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "מנטרל לגמרי וידאו ומסיר את העורך שירים מהחנות, שימו לב - יש להסיר את ההתקנה של העורך שירים",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Additive selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoogleToggled(!googleEnabled) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "בחירה אופציונלית: עם שירותי גוגל",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "הוספה של שירותי Google Play (GmsCore) במכשיר. אם כבוי, אין שירותי גוגל.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = googleEnabled,
                    onCheckedChange = { onGoogleToggled(it) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSavePressed,
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "שמור", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("שמירה והחלה", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(0.8f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("ביטול", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

// User Guide screen with modern custom Tab selectors matching requirements exactly
@Composable
fun UserGuideScreen() {
    val sectionsList = listOf(
        "הקדמה",
        "הגדרה ראשונית",
        "מידע נחוץ",
        "באגים אפשריים",
        "זכויות יוצרים"
    )
    
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab card selector top of the screen: custom inline bottom line tabs as requested by design instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sectionsList.forEachIndexed { idx, label ->
                    val isActive = (activeTabIndex == idx)
                    Column(
                        modifier = Modifier
                            .clickable { activeTabIndex = idx }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(3.3.dp)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(1.5.dp)
                                )
                        )
                    }
                }
            }
        }

        // Detailed section informational booklet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeLabel = sectionsList[activeTabIndex]
                    
                    Text(
                        text = "נושא המדריך: $activeLabel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    
                    // Displaying contents
                    val guideText = when(activeTabIndex) {
                        0 -> "ברוכים הבאים לאפליקציית ניהול נגן m36 כשר.\n\nכלי מתקדם זה פותח במיוחד כדי להעניק לך שליטה מלאה בפרמטרי הכשרות, חבילות ההתקנה, החומרה והשירותים של הנגן.\n\nמטרת האפליקציה היא לאפשר התאמה מושלמת בין רצון המשתמש לתצורה הכשרה של מכשיר ה-Android שברשותו, מבלי להסתבך עם הגדרות וקודים מורכבים במחשב."
                        1 -> "הנחיות לביצוע הגדרה ראשונית:\n\n1. עם אתחול האפליקציה לראשונה, מומלץ להיכנס לתפריט 'הגדרות' ולקבוע סיסמת מנהל ראשונית.\n\n2. סיסמת המנהל תמנע מילדים או משתמשים אחרים לערוך שינויים בכשרות המכשיר.\n\n3. שמור את הסיסמה במקום בטוח. היא תידרש ממך בכל כניסה לתפריט השינויים ובכל פעם שתתבצע שמירת שינויים במחיצות ה-System."
                        2 -> "מידע טכני נחוץ למנהל ה-ROM:\n\n* האפליקציה פועלת ברמת המערכת ומשנה את שמות קבצי ה-APK באנדרואיד על ידי שינוי הסיומת שלהם מ-.apk ל-.apkr (ובכך משביתה אותם לחלוטין ברמת הלינוקס).\n\n* קובצי ה-PackageInstaller מוחלפים בין גרסת המקור (הרשאות פתוחות להתקנה) לבין הגרסה הערוכה החסומה, המונעת כל התקנה ומניחה הגבלת חומרה איתנה שאינה ניתנת לעקיפה."
                        3 -> "שאלות נפוצות ובאגים אפשריים:\n\n* בעיה: נכשל שמירה ורשום שגיאת permissions?\nפתרון: ודא כי המכשיר מחזיק הרשאות רוט מלאות (Magisk/KernelSU Superuser) ושהאפליקציה הותקנה כמחיצת מערכת (System App) מתאימה.\n\n* בעיה: חנות האפליקציות לא מותקנת?\nפתרון: אפשרות 'התקנות' חנות דורשת שקובץ החנות com.example.offlinestore1 יהיה קיים מראש במכשיר."
                        else -> "כל הזכויות שמורות לפיתוח ה-ROM הכשר של נגן m36.\n\nמנהל הפיתוח והתיקונים: י.מ. מאואס\nמסייעים, יועצי כשרות ואבטחת מידע פנימית: חברי פורום מתמחים טופ.\n\nיישר כוח לכל העוסקים במלאכה לטובת חינוך כשר, טהור ואיכותי."
                    }
                    
                    Text(
                        text = guideText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

// Application Settings row
@Composable
fun SettingsScreen(
    onConfirmResetPassword: (String, String) -> Boolean,
    isSimulation: Boolean,
    onSimulationToggled: (Boolean) -> Unit,
    currentThemeMode: Int,
    onThemeChanged: (Int) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme parameter configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "עיצוב המערכת",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { onThemeChanged(0) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentThemeMode == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (currentThemeMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("בהיר", color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        OutlinedButton(
                            onClick = { onThemeChanged(1) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentThemeMode == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (currentThemeMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("כהה", color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        OutlinedButton(
                            onClick = { onThemeChanged(-1) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentThemeMode == -1) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (currentThemeMode == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("מערכת", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        
        // Simulator parameter configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "פרמטרי מערכת ופיתוח",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.primaryContainer)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "מצב הדמיה (סימולטור)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "הפעל הדמיה כדי לבצע שינויי קבצים באופן וירטואלי (במידה והמכשיר חסר רוט או לצרכי הדגמה ותצוגה)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = isSimulation,
                            onCheckedChange = { onSimulationToggled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        // Change Password form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "שינוי / עדכון סיסמת ניהול",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.primaryContainer)

                    val alreadyHasPassword = SystemManager.getPassword(context) != null

                    if (alreadyHasPassword) {
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("הזן סיסמה נוכחית") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("סיסמת ניהול חדשה (ספרות)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newPass.length < 4) {
                                Toast.makeText(context, "סיסמה חייבת להיות לפחות 4 תווים", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = onConfirmResetPassword(currentPass, newPass)
                                if (success) {
                                    currentPass = ""
                                    newPass = ""
                                    Toast.makeText(context, "סיסמת המנהל עודכנה בהצלחה!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "הסיסמה הנוכחית איננה נכונה!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("עדכן סיסמה", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// About application screen displaying contact credentials
@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "פרטים",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "מנהל נגן m36 כשר",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "גרסה v0.0.1_beta",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AboutContactRow(label = "יוצר ומפתח:", value = "י.מ. מאואס")
                AboutContactRow(label = "תמיכה ויעוץ טכני:", value = "Google AI studio")
                AboutContactRow(label = "דואר אלקטרוני לפניות:", value = "ysrlmyrmws1@gmail.com")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            Text(
                text = "אפליקציה זו מיועדת עבור שילוב במערכת (ROM Component) של נגנים מסוג M36 המורצים על אנדרואיד כשר, ומבוססת על הרשאות רוט מלאות. שינוי קבצי סיסטם על ידי האפליקציה חוסם התקנות ווידאו ישירות מדרגת לינוקס נמוכה.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun AboutContactRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
    }
}

// Supporting items for Custom Drawer Layout with Geometric Bullets (from the design HTML)
@Composable
fun DrawerLargeItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tiny Teal dot / indicator from sidebar item code: `<div class="w-2 h-2 rounded-full bg-teal-500"></div>`
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DrawerSmallItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
        )
        
        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
