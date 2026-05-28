package com.example.ui.screens

import com.example.ui.Translations
import androidx.activity.compose.BackHandler

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.TextStyle
import com.example.data.model.Lead
import com.example.data.model.AutomationRule
import com.example.data.model.ActivityLog
import com.example.data.model.PreloadedPdf
import com.example.ui.viewmodel.LeadViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadScoutDashboard(viewModel: LeadViewModel) {
    val leads by viewModel.leads.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val usernameDisplay by viewModel.userDisplayName.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedLead by viewModel.selectedLead.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val pushEnabledGlobal by viewModel.pushNotificationsEnabled.collectAsState()
    val scanNotifEnabledGlobal by viewModel.scanningNotificationsEnabled.collectAsState()
    val selectedRatingGlobal by viewModel.evaluationRating.collectAsState()
    val selectedFeedbackGlobal by viewModel.evaluationFeedback.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Radar, 1: Map, 2: Pitch Studio, 3: Automations, 4: Work-Log
    var showPortalOnly by remember { mutableStateOf(true) }
    var showAddLeadDialog by remember { mutableStateOf(false) }
    var topRightMenuExpanded by remember { mutableStateOf(false) }
    var globalShowSettingsDialog by remember { mutableStateOf(false) }
    var globalShowContractsDialog by remember { mutableStateOf(false) }
    var globalShowSupportDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Observe notification state and trigger SnackBar or local HUD
    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissNotification()
        }
    }

    BackHandler(enabled = (activeTab != 0 || selectedLead != null || !showPortalOnly)) {
        if (selectedLead != null) {
            viewModel.selectLead(null)
        } else if (!showPortalOnly) {
            showPortalOnly = true
        } else {
            activeTab = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    if (activeTab != 0 || selectedLead != null || !showPortalOnly) {
                        IconButton(
                            onClick = {
                                if (selectedLead != null) {
                                    viewModel.selectLead(null)
                                } else if (!showPortalOnly) {
                                    showPortalOnly = true
                                } else {
                                    activeTab = 0
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Geri Dön",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable {
                                activeTab = 0
                                viewModel.selectLead(null)
                                showPortalOnly = true
                            }
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                  text = usernameDisplay.ifEmpty { "Mustafa Enes" },
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 18.sp,
                                  fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                  text = if (lang == "tr") "Keşif Portalı" else "Outreach Portal",
                                  fontSize = 11.sp,
                                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    // Home quick jump button
                    IconButton(
                        onClick = {
                            activeTab = 0
                            viewModel.selectLead(null)
                            showPortalOnly = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Ana Sayfa"
                        )
                    }

                    // Profile quick button
                    IconButton(
                        onClick = {
                            activeTab = 4
                            viewModel.selectLead(null)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Kullanıcı Profili"
                        )
                    }

                    // Modern "çizgi" element menu (Fixes the right top menu bug!)
                    Box {
                        IconButton(
                            onClick = { topRightMenuExpanded = true },
                            modifier = Modifier.testTag("top_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu, // HAMBURGER DRAWER ICON - THREE LINES!
                                contentDescription = "Menü Seçenekleri"
                            )
                        }

                        DropdownMenu(
                            expanded = topRightMenuExpanded,
                            onDismissRequest = { topRightMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Oturum & Profil", fontSize = 13.sp) },
                                onClick = {
                                    activeTab = 4
                                    viewModel.selectLead(null)
                                    topRightMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Bildirimler & Ayarlar", fontSize = 13.sp) },
                                onClick = {
                                    globalShowSettingsDialog = true
                                    topRightMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Coğrafi Harita", fontSize = 13.sp) },
                                onClick = {
                                    activeTab = 1
                                    viewModel.selectLead(null)
                                    topRightMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Sözleşmeler & KVKK", fontSize = 13.sp) },
                                onClick = {
                                    globalShowContractsDialog = true
                                    topRightMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("E-posta ile İletişim", fontSize = 13.sp) },
                                onClick = {
                                    globalShowSupportDialog = true
                                    topRightMenuExpanded = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("DB Verileri Sıfırla", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.clearHistory()
                                    topRightMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = {
                        activeTab = 0
                        viewModel.selectLead(null)
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = Translations.get("scout_radar", lang)) },
                    label = { Text(Translations.get("scout_radar", lang)) },
                    modifier = Modifier.testTag("tab_radar")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = {
                        activeTab = 1
                        viewModel.selectLead(null)
                    },
                    icon = { Icon(Icons.Default.Place, contentDescription = Translations.get("map_view", lang)) },
                    label = { Text(Translations.get("map_view", lang)) },
                    modifier = Modifier.testTag("tab_map")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = {
                        activeTab = 2
                        viewModel.selectLead(null)
                    },
                    icon = { Icon(Icons.Default.Email, contentDescription = Translations.get("pitch_studio", lang)) },
                    label = { Text(Translations.get("pitch_studio", lang)) },
                    modifier = Modifier.testTag("tab_pitch")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = {
                        activeTab = 3
                        viewModel.selectLead(null)
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = Translations.get("automations", lang)) },
                    label = { Text(Translations.get("automations", lang)) },
                    modifier = Modifier.testTag("tab_automation")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = {
                        activeTab = 4
                        viewModel.selectLead(null)
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = Translations.get("profile_settings", lang)) },
                    label = { Text(Translations.get("profile_settings", lang)) },
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(400)) { width -> width / 2 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(400)) { width -> -width / 2 } + fadeOut(animationSpec = tween(400)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(400)) { width -> -width / 2 } + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(400)) { width -> width / 2 } + fadeOut(animationSpec = tween(400)))
                    }
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> RadarTab(
                        viewModel = viewModel,
                        leads = leads,
                        selectedLead = selectedLead,
                        onAddNewLeadClick = { showAddLeadDialog = true },
                        isScanning = isScanning,
                        onShowOnMapClick = { activeTab = 1 },
                        showPortalOnly = showPortalOnly,
                        onShowPortalOnlyChange = { showPortalOnly = it }
                    )
                    1 -> TurkeyClientMap(
                        viewModel = viewModel
                    )
                    2 -> PitchStudioTab(
                        viewModel = viewModel,
                        selectedLead = selectedLead,
                        leads = leads
                    )
                    3 -> AutomationTab(
                        viewModel = viewModel,
                        rules = rules
                    )
                    4 -> ProfileSettingsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAddLeadDialog) {
        AddLeadDialog(
            onDismiss = { showAddLeadDialog = false },
            onSave = { name, website, email, notes, score ->
                viewModel.addManualLead(name, website, email, notes, score)
                showAddLeadDialog = false
            }
        )
    }

    if (globalShowSettingsDialog) {
        var curPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }
        var feedbackCommentInput by remember { mutableStateOf(selectedFeedbackGlobal) }

        AlertDialog(
            onDismissRequest = { globalShowSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Bildirim & Uygulama Ayarları", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("BİLDİRİM VE TERCİHLER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anlık Sistem Bildirimleri", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Uygulama bildirimlerini push edin.", fontSize = 9.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = pushEnabledGlobal,
                            onCheckedChange = { viewModel.pushNotificationsEnabled.value = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hassas Tarama Bildirimleri", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Yeni B2B siteleri taranırken uyarın.", fontSize = 9.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = scanNotifEnabledGlobal,
                            onCheckedChange = { viewModel.scanningNotificationsEnabled.value = it }
                        )
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Text("GÜVENLİK VE PAROLA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = curPassInput,
                        onValueChange = { curPassInput = it },
                        label = { Text("Mevcut Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("Yeni Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Yeni Şifre Onayla") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (newPassInput != confirmPassInput) {
                                return@Button
                            }
                            if (viewModel.changePassword(curPassInput, newPassInput)) {
                                curPassInput = ""
                                newPassInput = ""
                                confirmPassInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Şifre Değiştir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Text("UYGULAMAYI DEĞERLENDİRİN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= selectedRatingGlobal) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { viewModel.submitEvaluation(star, feedbackCommentInput) }
                                    .padding(2.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = feedbackCommentInput,
                        onValueChange = { feedbackCommentInput = it },
                        label = { Text("Geri Bildiriminiz") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.submitEvaluation(if (selectedRatingGlobal == 0) 5 else selectedRatingGlobal, feedbackCommentInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yorumu Gönder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { globalShowSettingsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    if (globalShowContractsDialog) {
        var localContractTitle by remember { mutableStateOf<String?>(null) }
        var localContractBody by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { globalShowContractsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Yasal Sözleşmeler ve KVKK", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Lütfen okumak istediğiniz belgeyi seçin:", fontSize = 12.sp, color = Color.Gray)

                    Button(
                        onClick = {
                            localContractTitle = "Kullanım Sözleşmesi (EULA)"
                            localContractBody = "Clyve app siber arama ve dijital pazarlama otomasyon yazılımının tüm hakları saklıdır. Bu uygulama aracılığıyla taranan B2B kurumsal internet siteleri, halka açık veriyi işlemektedir. Alıcı, siber tarayıcıyı yürütürken spam gönderimi, yetkisiz güvenlik sızması gibi usulsüz adımlardan kaçınacağını ve B2B mevzuatına riayet edeceğini taahhüt eder."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kullanım Sözleşmesi (EULA)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    Button(
                        onClick = {
                            localContractTitle = "KVKK Aydınlatma Metni"
                            localContractBody = "6698 Sayılı Kişisel Verilerin Korunması Kanunu gereğince, Clyve app mobil uygulaması kullanıcıların ad-soyad, e-posta, lisans durumu ve erişim sağladığı IP adresini yerel SQLite veritabanında şifreli olarak yedekler. Çekilen bu veriler üçüncü şahıslara satılmaz, sadece hesap oturumlarının bütünlüğü ve hırsızlık tespiti için tutulur."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("KVKK Bilgilendirme Politikası", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    Button(
                        onClick = {
                            localContractTitle = "IP Veri Saklama ve Silme Taahhüdü"
                            localContractBody = "IP adresleri siber sızma testleri ve KVKK uyumluluk denetimleri çerçevesinde yerel SQLite tablosunda kriptolu saklanır. Hesabınızın silinmesi durumunda tüm IP kayıtlarınız 72 saatlik nadas bekleme havuzuna alınır. Havuz süresi dolduğunda yedekler 6 ay boyunca soğuk saklama kutusunda korunacak ve süre bitiminde sistem tarafından hiçbir kalıntı bırakılmaksızın sonsuza dek tamamen temizlenecektir."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("IP Saklama Koşulları", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    if (localContractTitle != null) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(localContractTitle ?: "", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(localContractBody ?: "", fontSize = 9.sp, lineHeight = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { globalShowContractsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    if (globalShowSupportDialog) {
        var supportSubjectInput by remember { mutableStateOf("") }
        var supportMessageInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { globalShowSupportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Destek Talebi İlet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Sorularınızı doğrudan destek ekibimize iletin.", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = supportSubjectInput,
                        onValueChange = { supportSubjectInput = it },
                        label = { Text("Konu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = supportMessageInput,
                        onValueChange = { supportMessageInput = it },
                        label = { Text("Sorununuz / Mesajınız") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (viewModel.dispatchSupportTicket(supportSubjectInput, supportMessageInput)) {
                                supportSubjectInput = ""
                                supportMessageInput = ""
                                globalShowSupportDialog = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Destek Talebini İlet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Doğrudan E-Posta: destek@clyweb.com",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { globalShowSupportDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

// ==========================================
// STAR COCKPIT BACKGROUND CANVAS
// ==========================================
@Composable
fun StarBackgroundCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val random = java.util.Random(1337)
        for (i in 0..65) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val radius = random.nextFloat() * 2.5f + 1.2f
            val alpha = random.nextFloat() * 0.4f + 0.4f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = alpha),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

// ==========================================
// TAB 0: SCOUT RADAR SCREEN
// ==========================================
@Composable
fun RadarTab(
    viewModel: LeadViewModel,
    leads: List<Lead>,
    selectedLead: Lead?,
    onAddNewLeadClick: () -> Unit,
    isScanning: Boolean,
    onShowOnMapClick: () -> Unit,
    showPortalOnly: Boolean,
    onShowPortalOnlyChange: (Boolean) -> Unit
) {
    val lang by viewModel.appLanguage.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Synchronize empty state and initial scanner text
    LaunchedEffect(leads.isEmpty()) {
        if (leads.isEmpty()) {
            onShowPortalOnlyChange(true)
        }
    }

    LaunchedEffect(Unit) {
        if (searchInput.isEmpty()) {
            searchInput = "Avrasya Hotel"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle space physics sky backdrop
        StarBackgroundCanvas(modifier = Modifier.fillMaxSize())

        if (showPortalOnly) {
            // CENTRAL SPACE SEARCH PORTAL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    // Futuristic Glowing Metallic Cly Web Title
                    Text(
                        text = "Cly Web",
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFF00E5FF), // Cyber-cyan
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 2.dp),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF00ADB5).copy(alpha = 0.9f),
                                blurRadius = 25f
                            )
                        )
                    )

                    // Cyber Subtitle
                    Text(
                        text = "DEEP COGNITIVE SPACE RADAR CLIENT SEARCH STATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Portal cockpit input console card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CORE TRANSPONDER QUANTUM WAVE SCAN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Search bar
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = { Text(if (lang == "tr") "Örn. Avrasya Hotel, Trabzon Restoranları..." else "E.g., Avrasya Hotel, Restaurants in Trabzon...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_input_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Scan Icon", tint = MaterialTheme.colorScheme.primary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f)
                                )
                            )

                            // Engage scan action button
                            Button(
                                onClick = {
                                    if (searchInput.trim().isNotEmpty()) {
                                        focusManager.clearFocus()
                                        viewModel.scanLeads(searchInput)
                                        onShowPortalOnlyChange(false) // Unlocks dashboard view upon scan initiates
                                    }
                                },
                                enabled = !isScanning && searchInput.trim().isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("scan_trigger_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            ) {
                                if (isScanning) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                        Text(Translations.get("locking_targets", lang), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text(Translations.get("engage_radar", lang), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }
                            }

                            // Tips and manual scout options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Translations.get("organic_pipelines_ready", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onAddNewLeadClick,
                                    modifier = Modifier.testTag("add_manual_trigger")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Translations.get("add_manual_btn", lang), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Preloaded galactic search coordinates
                    Text(
                        text = Translations.get("presets_label", lang),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        listOf(
                            if (lang == "tr") "Avrasya Otel 🛰️" else "Avrasya Hotel 🛰️",
                            if (lang == "tr") "Trabzon Diş Hekimleri 🌌" else "Trabzon Dentists 🌌",
                            if (lang == "tr") "İzmir Kafe 🚀" else "Izmir Cafe 🚀"
                        ).forEach { target ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable {
                                    searchInput = target.replace(Regex("[🛰️🌌🚀\\s]+$"), "").trim()
                                }
                            ) {
                                Text(
                                    text = target,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Enter space station logs and database button
                    if (leads.isNotEmpty()) {
                        Button(
                            onClick = { onShowPortalOnlyChange(false) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "tr") "Veritabanı Terminaline Gir (${leads.size} taranan)" else "Enter Database Terminal (${leads.size} scoured)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // SPACE STATION TERMINAL (RESULTS VIEW)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cockpit status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = Translations.get("radar_cockpit", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${Translations.get("frequencies_locked", lang)}${leads.size}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translations.get("scouring_busy", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            } else {
                                Button(
                                    onClick = { onShowPortalOnlyChange(true) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Translations.get("new_search_btn", lang), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Split master detail cockpit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scoured contacts list column
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${Translations.get("scoured_prospects_count", lang)} (${leads.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (leads.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(Translations.get("no_scoured_prospects", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(leads) { lead ->
                                    LeadItemRow(
                                        lead = lead,
                                        isSelected = selectedLead?.id == lead.id,
                                        onClick = { viewModel.selectLead(lead) }
                                    )
                                }
                            }
                        }
                    }

                    // Prospect details card column
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        if (selectedLead != null) {
                            LeadDetailCard(
                                lead = selectedLead,
                                onDeleteClick = { viewModel.deleteLead(selectedLead) },
                                onCloseClick = { viewModel.selectLead(null) },
                                onShowOnMapClick = onShowOnMapClick,
                                lang = lang
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (lang == "tr") "Detayları görmek için listeden bir firma seçin" else "Select corporate details from the list to preview",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeadItemRow(
    lead: Lead,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lead_row_${lead.id}"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Circular Score Percent Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = getPercentageColor(lead.leadScorePercent).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${lead.leadScorePercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = getPercentageColor(lead.leadScorePercent)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = lead.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Website Tag
                    Surface(
                        color = if (lead.hasWebsite) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (lead.hasWebsite) "Web" else "No Web",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lead.hasWebsite) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Ads Tag
                    Surface(
                        color = if (lead.hasAds) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (lead.hasAds) "Ads" else "No Ads",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lead.hasAds) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Outreach tag status
                    Surface(
                        color = getStatusBgColor(lead.outreachStatus),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = lead.outreachStatus,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = getStatusTextColor(lead.outreachStatus),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LeadDetailCard(
    lead: Lead,
    onDeleteClick: () -> Unit,
    onCloseClick: () -> Unit,
    onShowOnMapClick: () -> Unit,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lead_detail_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.get("researcher_report", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onShowOnMapClick,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = Translations.get("haritada_goster", lang),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translations.get("haritada_goster", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onCloseClick,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Translations.get("geri_don", lang),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translations.get("geri_don", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = Translations.get("delete_lead", lang),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = lead.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Massive Opportunity Gauge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = getPercentageColor(lead.leadScorePercent).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${lead.leadScorePercent}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = getPercentageColor(lead.leadScorePercent)
                        )
                        Text(
                            text = "POTENTIAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailMetItem(
                        icon = Icons.Default.Share,
                        label = "Website Target:",
                        value = if (lead.websiteUrl.isNotEmpty()) lead.websiteUrl else "Missing (Website without site)",
                        valColor = if (lead.websiteUrl.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    DetailMetItem(
                        icon = Icons.Default.Warning,
                        label = "Ad Pipelines:",
                        value = if (lead.hasAds) "Active Campaigns Identified" else "No Active Advertising Detected",
                        valColor = if (lead.hasAds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    DetailMetItem(
                        icon = Icons.Default.Email,
                        label = "Primary Contact:",
                        value = lead.contactEmail,
                        valColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "RESEARCHER ANALYSIS MATRIX",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            // Dynamic Report Notes Box
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            text = lead.analysisNotes,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ready for outreach campaigns. Active rule triggers will intercept client response.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Quick Status Bottom Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operator: ${lead.createdBy}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = "Scanned: " + SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date(lead.lastActivityTime)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DetailMetItem(icon: ImageVector, label: String, value: String, valColor: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(14.dp)
                .offset(y = 2.dp)
        )
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 11.sp, color = valColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}


// ==========================================
// TAB 1: PITCH STUDIO & AUTO SIMULATOR
// ==========================================
@Composable
fun PitchStudioTab(
    viewModel: LeadViewModel,
    selectedLead: Lead?,
    leads: List<Lead>
) {
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sendToEmail by remember { mutableStateOf("") }

    // Synchronize drafts when client switch happens
    LaunchedEffect(selectedLead) {
        selectedLead?.let {
            subject = "Custom Marketing Audit Proposal for ${it.name}"
            sendToEmail = it.contactEmail
            body = "Hi team at ${it.name},\n\n" +
                    "I was reviewing Google listing pipelines and noticed some potential optimizations regarding your digital visibility.\n\n" +
                    "Currently, your business has a potential marketing expansion coefficient of ${it.leadScorePercent}%. " +
                    "Our specialist B2B outreach group would love to support you with professional Google/Meta advertisement integration and booking pipeline landing pages.\n\n" +
                    "Let us know if you would like a brief 10-minute briefing call.\n\n" +
                    "Regards,\n${viewModel.currentUser.value}\nB2B Lead Scout Workspace"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Strategic Cold Pitch Studio",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Client card selector
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select Target Client",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(leads) { item ->
                        val isSelected = selectedLead?.id == item.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectLead(item) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(getStatusBgColor(item.outreachStatus), CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text(item.contactEmail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${item.leadScorePercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = getPercentageColor(item.leadScorePercent))
                            }
                        }
                    }
                }
            }

            // Campaign details & simulator dispatch
            Card(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .testTag("pitch_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (selectedLead == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Choose a client from the left pane to initialize outreach proposals.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mailing Node Dispatcher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Drafting campaign targeting: ${selectedLead.name}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = sendToEmail,
                            onValueChange = { sendToEmail = it },
                            label = { Text("To (Destination Contact Email)") },
                            modifier = Modifier.fillMaxWidth().testTag("pitch_email_to"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject Line") },
                            modifier = Modifier.fillMaxWidth().testTag("pitch_subject"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Proposal Content") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("pitch_body"),
                            maxLines = 10
                        )

                        // Action rows
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    viewModel.sendManualEmail(selectedLead, subject, body)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_pitch_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Outreach")
                            }

                            // Dynamic reply simulator box
                            if (selectedLead.outreachStatus != "NEW") {
                                Button(
                                    onClick = { viewModel.simulateCustomerReply(selectedLead) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("simulate_reply_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate Lead Reply", fontSize = 12.sp)
                                }
                            }
                        }

                        if (selectedLead.outreachStatus == "NEW") {
                            Text(
                                text = "Send the initial outreach proposal to enable the Customer Response Simulator and trigger registered automations.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Lead status currently: [${selectedLead.outreachStatus}]. Press 'Simulate Lead Reply' to mock inbox updates and fire registered actions.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// TAB 2: AUTOMATION WORKFLOWS & FILES
// ==========================================
@Composable
fun AutomationTab(
    viewModel: LeadViewModel,
    rules: List<AutomationRule>
) {
    var showAddRule by remember { mutableStateOf(false) }

    val pdfFiles by viewModel.pdfFiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Outreach Automatons",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pre-arrange PDF deliveries or automatic subscribers log trigger sequences",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { showAddRule = true },
                modifier = Modifier.testTag("add_rule_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Configure Trigger Integration")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Rule")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Automation Recipes List
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Active Automation Recipes (${rules.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No rules configured. Tap Add Rule to intercept callbacks.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rules) { rule ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rule_card_${rule.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(rule.ruleName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteRule(rule.id, rule.ruleName) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    HorizontalDivider()

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextRow("Trigger Event:", "When Client replies to any outbound pitching sequence")
                                        TextRow(
                                            "Automated Action:",
                                            when (rule.actionType) {
                                                "SEND_PDF" -> "Send Predefined File Attachment"
                                                "SUBSCRIBE_SYSTEM" -> "Move target to Subscribers Group & Update CRM status"
                                                "EMAIL_OUT" -> "Mail customized automated reply template"
                                                else -> rule.actionType
                                            }
                                        )
                                        if (rule.pdfAttachmentName.isNotEmpty()) {
                                            TextRow("Attachment:", rule.pdfAttachmentName)
                                        }
                                        TextRow("Forward Destination:", rule.destinationEmail)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PDF pre-loaded asset locker
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PDF Proposal Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Simulated PDFs uploaded/generated and registered for campaigns:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    HorizontalDivider()

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pdfFiles) { pdf ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share, // Placeholder PDF indicator
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.fileName, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${pdf.sizeLabel} • registered successfully", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(pdf.description, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), lineHeight = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Simulated upload action
                    var newPdfName by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPdfName,
                            onValueChange = { newPdfName = it },
                            placeholder = { Text("proposal_name.pdf") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        IconButton(
                            onClick = {
                                if (newPdfName.isNotEmpty()) {
                                    val formattedName = if (newPdfName.endsWith(".pdf")) newPdfName else "$newPdfName.pdf"
                                    val currentList = viewModel.pdfFiles.value.toMutableList()
                                    currentList.add(PreloadedPdf(formattedName, "1.5 MB", "User generated campaign asset.", "2026-05-27"))
                                    viewModel.pdfFiles.value = currentList
                                    newPdfName = ""
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add PDF Asset", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddRule) {
        AddRuleDialog(
            pdfList = pdfFiles,
            onDismiss = { showAddRule = false },
            onSave = { name, action, pdf, subject, body, targetEmail ->
                viewModel.addNewRule(name, action, pdf, subject, body, targetEmail)
                showAddRule = false
            }
        )
    }
}

@Composable
fun TextRow(label: String, valText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
        Text(valText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}


// ==========================================
// TAB 3: WORKSPACE ACTIVITY LOGS
// ==========================================
@Composable
fun WorkspaceLogTab(
    viewModel: LeadViewModel,
    logs: List<ActivityLog>,
    currentUser: String,
    leadsCount: Int,
    rulesCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Shared Collaboration Center",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Metrics Banner card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricWidget(title = "Unified Leads", value = leadsCount.toString(), icon = Icons.Default.Search)
                MetricWidget(title = "Automations Active", value = rulesCount.toString(), icon = Icons.Default.Settings)
                MetricWidget(
                    title = "Converted (Subscribed)", 
                    value = viewModel.leads.value.count { it.outreachStatus == "SUBSCRIBED" }.toString(), 
                    icon = Icons.Default.CheckCircle
                )
                MetricWidget(title = "Workspace Members", value = "2", icon = Icons.Default.Person)
            }
        }

        // Active Workspace profiles card detail
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = getPercentageColor(85).copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("M", fontWeight = FontWeight.Bold, color = getPercentageColor(85))
                            }
                        }
                        Column {
                            Text("Mustafa (Owner)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("mustafaeenesozkaya@gmail.com", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    Text("Role: Business developer, defining rules, triggering email campaigns and subscription automation routines.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("P", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text("Partner Investigator (Enes Eren Dil)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("partner.prospector@gmail.com", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    Text("Role: Site researcher, uploading proposal PDFs, executing database scoping audits and syncing scoured Google lists.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Shared collaborative logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unified Sync Activity Stream",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear logs", fontSize = 12.sp)
                    }
                }

                HorizontalDivider()

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Workspace logged actions display here.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val tagColor = when (log.tag) {
                                    "SCAN" -> MaterialTheme.colorScheme.primary
                                    "EMAIL" -> MaterialTheme.colorScheme.secondary
                                    "RULE" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outline
                                }

                                Surface(
                                    color = tagColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(55.dp)
                                ) {
                                    Text(
                                        text = log.tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tagColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.actionDetails,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "By ${log.operatorName} • " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricWidget(title: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
    }
}


// ==========================================
// CORE HELPERS & UTILITY METHODS
// ==========================================
fun getPercentageColor(percentage: Int): Color {
    return when {
        percentage >= 85 -> Color(0xFF2E7D32) // Soft Green
        percentage >= 70 -> Color(0xFFEF6C00) // Soft Orange
        else -> Color(0xFFC62828) // Deep Crimson Red
    }
}

fun getStatusBgColor(status: String): Color {
    return when (status) {
        "NEW" -> Color(0xFFE3F2FD)
        "EMAIL_SENT" -> Color(0xFFFFF3E0)
        "REPLIED" -> Color(0xFFEDE7F6)
        "SUBSCRIBED" -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }
}

fun getStatusTextColor(status: String): Color {
    return when (status) {
        "NEW" -> Color(0xFF1565C0)
        "EMAIL_SENT" -> Color(0xFFE65100)
        "REPLIED" -> Color(0xFF4527A0)
        "SUBSCRIBED" -> Color(0xFF2E7D32)
        else -> Color(0xFF616161)
    }
}


// ==========================================
// CUSTOM DIALOGS & INTERACTIVE COMPONENTS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLeadDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("80") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Log Lead Manually", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business/Lead Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_lead_name")
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website (leave empty if none)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Contact Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Potential Lead Score %") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Researcher Audit Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                val scoreInt = score.toIntOrNull() ?: 80
                                onSave(name, website, email, notes, scoreInt)
                            }
                        },
                        modifier = Modifier.testTag("save_lead_btn")
                    ) {
                        Text("Record Lead")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    pdfList: List<PreloadedPdf>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("SEND_PDF") } // SEND_PDF, SUBSCRIBE_SYSTEM, EMAIL_OUT
    var selectedPdf by remember { mutableStateOf("") }
    var definedSubject by remember { mutableStateOf("RE: Digital visibility offer") }
    var definedBody by remember { mutableStateOf("Hi customer,\n\nWe received your reply. Here is the sales PDF brochure.") }
    var targetEmail by remember { mutableStateOf("mustafaeenesozkaya@gmail.com") }

    var expandedAction by remember { mutableStateOf(false) }
    var expandedPdf by remember { mutableStateOf(false) }

    // Auto select first PDF by default if empty
    LaunchedEffect(pdfList) {
        if (pdfList.isNotEmpty()) {
            selectedPdf = pdfList.first().fileName
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Configure Call-back Interceptor", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Recipe/Rule Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rule_name")
                )

                // Action dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedAction,
                    onExpandedChange = { expandedAction = !expandedAction },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = when (actionType) {
                            "SEND_PDF" -> "Send Predefined File Attachment"
                            "SUBSCRIBE_SYSTEM" -> "Move target to CRM Subscriber List"
                            "EMAIL_OUT" -> "Mail customized automated reply template"
                            else -> actionType
                        },
                        onValueChange = {},
                        label = { Text("Automated Trigger Rule") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAction) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAction,
                        onDismissRequest = { expandedAction = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Send Predefined File Attachment") },
                            onClick = { actionType = "SEND_PDF"; expandedAction = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Move target to CRM Subscriber List") },
                            onClick = { actionType = "SUBSCRIBE_SYSTEM"; expandedAction = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Mail customized automated reply template") },
                            onClick = { actionType = "EMAIL_OUT"; expandedAction = false }
                        )
                    }
                }

                if (actionType == "SEND_PDF") {
                    // PDF choice dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedPdf,
                        onExpandedChange = { expandedPdf = !expandedPdf },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedPdf,
                            onValueChange = {},
                            label = { Text("Target Proposal Checklist PDF") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPdf) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPdf,
                            onDismissRequest = { expandedPdf = false }
                        ) {
                            pdfList.forEach { doc ->
                                DropdownMenuItem(
                                    text = { Text(doc.fileName) },
                                    onClick = { selectedPdf = doc.fileName; expandedPdf = false }
                                )
                            }
                        }
                    }
                }

                if (actionType == "EMAIL_OUT") {
                    OutlinedTextField(
                        value = definedSubject,
                        onValueChange = { definedSubject = it },
                        label = { Text("Auto-reply Email Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = definedBody,
                        onValueChange = { definedBody = it },
                        label = { Text("Auto-reply Email Body") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                OutlinedTextField(
                    value = targetEmail,
                    onValueChange = { targetEmail = it },
                    label = { Text("Redirect Destination Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (ruleName.isNotEmpty()) {
                                onSave(
                                    ruleName,
                                    actionType,
                                    if (actionType == "SEND_PDF") selectedPdf else "",
                                    definedSubject,
                                    definedBody,
                                    targetEmail
                                )
                            }
                        },
                        modifier = Modifier.testTag("save_rule_btn")
                    ) {
                        Text("Active Rule")
                    }
                }
            }
        }
    }
}

// ==========================================
// CENTRAL PROFILE & PROFESSIONAL SETTINGS TAB
// ==========================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsTab(viewModel: LeadViewModel) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val usernameDisplay by viewModel.userDisplayName.collectAsState()
    val userPassword by viewModel.userPassword.collectAsState()
    val userIp by viewModel.userSimulatedIp.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    // Preferences states
    val pushEnabled by viewModel.pushNotificationsEnabled.collectAsState()
    val scanNotifEnabled by viewModel.scanningNotificationsEnabled.collectAsState()
    val selectedRating by viewModel.evaluationRating.collectAsState()
    val selectedFeedback by viewModel.evaluationFeedback.collectAsState()
    val deletionTime by viewModel.accountDeletionScheduledTime.collectAsState()

    // Activity Logs
    val systemLogs by viewModel.logs.collectAsState()

    // Local UI control states
    var curPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }

    var feedbackCommentInput by remember { mutableStateOf(selectedFeedback) }
    var supportSubjectInput by remember { mutableStateOf("") }
    var supportMessageInput by remember { mutableStateOf("") }

    var activeContractDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeContractDialogBody by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    // Dynamic timer ticker to show remaining hours for deletion countdown
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deletionTime) {
        if (deletionTime != null) {
            while (true) {
                currentTimeMillis = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    // Interactive Sign In or Display Page
    if (!isUserLoggedIn) {
        // SLEEK LUXURY LOGIN PORTAL FOR CLY WEB
        var emailInput by remember { mutableStateOf("mustafaeenesozkaya@gmail.com") }
        var usernameInput by remember { mutableStateOf("Mustafa Enes Özkaya") }
        var passInput by remember { mutableStateOf("Password123") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern stylized crown logo
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Text(
                        text = "Cly Web Giriş Paneli",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Yerel e-posta şifrenizi girin veya anında Google Identity ile bağlanın.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("E-posta Adresi") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Kullanıcı Adı") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text("Parola") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            viewModel.loginWithEmail(emailInput, usernameInput, passInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_btn_submit")
                    ) {
                        Text("Güvenli İşlem Kilidini Aç", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // GOOGLE CONNECT PORTAL SECTION
                    OutlinedButton(
                        onClick = { viewModel.loginWithGoogle() },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        border = BorderStroke(1.2.dp, Color(0xFFEA4335).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_btn_google")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Google Hesabı ile Bağlan", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.loginWithEmail("mustafaeenesozkaya@gmail.com", "Mustafa", "Password123")
                        }
                    ) {
                        Text("Bypass Girişi (Hızlı Test)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    } else {
        // FULL PROFILE SETTINGS DECK
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // USER ID AVATAR SUMMARY CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = usernameDisplay.take(2).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = usernameDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = Color(0xFF10B981)) { 
                                Text("AKTİF", fontSize = 8.sp, color = Color.White) 
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("B2B İş Ortağı", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Logout Button
                    IconButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Oturumu Kapat", tint = Color.Red)
                    }
                }
            }

            val isAdmin by viewModel.isAdmin.collectAsState()

            if (isAdmin) {
                // EXCLUSIVE PRIVILEGED ADMINISTRATOR CONSOLE
                var showCopiedJsonPrompt by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = Translations.get("admin_audit_panel", lang),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = Translations.get("admin_audit_desc", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )

                        // WEB WINDOWS / MAC ADMIN HUB ACCESS POINT
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (lang == "tr") "🖥️ BİLGİSAYAR WEB ADMİN PANELİ" else "🖥️ COMPUTER WEB ADMIN HUB",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = if (lang == "tr") 
                                        "Bu telefondaki B2B verilerini, dökümanları ve IP loglarını bilgisayarınızdan kontrol edebilirsiniz. Telefonunuz ile aynı ağdaki (Wi-Fi) bilgisayarınızın tarayıcısını açıp şu adresi girin:" 
                                        else "Verify scanned documents, crawler files and database logs directly from your PC browser. Ensure your PC is on the same local network and visit:",
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "http://localhost:9090",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Text(
                                    text = if (lang == "tr") "*Not: Bulut konteyner ortamında veya emülatörde çalışırken bilgisayarınızdan doğrudan localhost:9090 adresini kullanarak erişebilirsiniz." else "*Note: Standard emulator forwarded port binds are available directly over localhost:9090 on this developer workstation.",
                                    fontSize = 8.5.sp,
                                    lineHeight = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Text(
                            text = Translations.get("admin_active_users", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // USER METRICS FOR DISPUTES & CRAWLER CHECKS (Exactly as requested!)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("1. mustafaeenesozkaya@gmail.com\n   · Ad: Mustafa Enes Özkaya\n   · Rol: Kurucu / Super Admin\n   · IP: $userIp\n   · KVKK Onay: ENGELLENEMEZ EVET\n   · Durum: Yetkilendirildi", fontSize = 10.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // DEVICE MARKS TRACKING FOR CYBER RADAR
                        Text(
                            text = Translations.get("admin_device_audit", lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Translations.get("admin_ip_disclaimer", lang),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 12.sp
                        )

                        Button(
                            onClick = {
                                showCopiedJsonPrompt = !showCopiedJsonPrompt
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Translations.get("admin_export_btn", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showCopiedJsonPrompt) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = Translations.get("admin_export_success", lang),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Raw copyable format block
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = """{"legal_audit_version":"1.0.4","export_timestamp":${System.currentTimeMillis()},"admin_operators":[{"email":"mustafaeenesozkaya@gmail.com","name":"Mustafa Enes Özkaya","role":"Super Admin","session_ip":"$userIp","device_model":"Android Emulator Pro"}],"registered_compliance_status":{"eula_accepted":true,"kvkk_accepted":true,"ip_vault_checked":true},"local_leads_count":12,"system_integrity":"SECURE_AUDIT_SIGNED"}""",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = Color(0xFF00FF66)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showLogsDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Veritabanı Güvenlik ve IP Loglarını İncele", fontSize = 11.sp)
                        }
                    }
                }
            }

            // NOTIFICATIONS & APP PREFERENCES CONTROLS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(Translations.get("language_selection", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.appLanguage.value = "tr" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "tr") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Translations.get("select_tr", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.appLanguage.value = "en" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Translations.get("select_en", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(Translations.get("system_preferences", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("system_notifications", lang), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text("Fırsat durumları ve müşteri teklif yanıtlarını push edin.", fontSize = 9.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = pushEnabled,
                            onCheckedChange = { viewModel.pushNotificationsEnabled.value = it }
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("scanning_notifications", lang), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text("Yeni B2B siteleri analiz edilirken uyarın.", fontSize = 9.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = scanNotifEnabled,
                            onCheckedChange = { viewModel.scanningNotificationsEnabled.value = it }
                        )
                    }
                }
            }

            // PASSWORD CHANGE PANEL
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Parolayı Güncelle", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = curPassInput,
                        onValueChange = { curPassInput = it },
                        label = { Text("Mevcut Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("Yeni Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Yeni Şifre Onayla") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newPassInput != confirmPassInput) {
                                viewModel.dismissNotification()
                                viewModel.loginWithEmail(userEmail, usernameDisplay, userPassword) // Trigger message
                                return@Button
                            }
                            if (viewModel.changePassword(curPassInput, newPassInput)) {
                                curPassInput = ""
                                newPassInput = ""
                                confirmPassInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Şifre Değiştirme Talebi Gönder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // APP EVALUATION (RATING FORM)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Cly Web'i Değerlendirin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Görüşlerinizi bizimle paylaşıp uygulamayı oylayın.", fontSize = 10.sp, color = Color.Gray)

                    // 5-Star Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= selectedRating) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { viewModel.submitEvaluation(star, feedbackCommentInput) }
                                    .padding(2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = feedbackCommentInput,
                        onValueChange = { feedbackCommentInput = it },
                        label = { Text("Geribildirim Mesajınız") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (selectedRating == 0) {
                                viewModel.submitEvaluation(5, feedbackCommentInput)
                            } else {
                                viewModel.submitEvaluation(selectedRating, feedbackCommentInput)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Değerlendirmeyi Kaydet ve Gönder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SECURITY CONTRACT PANEL (AGREEMENTS)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("KVKK & Kullanıcı Sözleşmeleri", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Uygulamanın yasal uyumluluk belgelerini aşağıdan okuyup onaylayabilirsiniz.", fontSize = 10.sp, color = Color.Gray)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeContractDialogTitle = "Uygulama Kullanım Sözleşmesi (EULA)"
                                activeContractDialogBody = "Cly Web siber arama ve dijital pazarlama otomasyon yazılımının tüm hakları saklıdır. Bu uygulama aracılığıyla taranan B2B kurumsal internet siteleri, halka açık veriyi işlemektedir. Alıcı, siber tarayıcıyı yürütürken spam gönderimi, yetkisiz güvenlik sızması gibi usulsüz adımlardan kaçınacağını ve B2B mevzuatına riayet edeceğini taahhüt eder."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kullanım Sözleşmesi", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        Button(
                            onClick = {
                                activeContractDialogTitle = "KVKK Kişisel Verilerin Güvenliği Politikası"
                                activeContractDialogBody = "6698 Sayılı Kişisel Verilerin Korunması Kanunu gereğince, Cly Web mobil uygulaması kullanıcıların ad-soyad, e-posta, lisans durumu ve erişim sağladığı IP adresini yerel SQLite veritabanında şifreli olarak yedekler. Çekilen bu veriler üçüncü şahıslara satılmaz, sadece hesap oturumlarının bütünlüğü ve hırsızlık tespiti için tutulur."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("KVKK Politikası", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        Button(
                            onClick = {
                                activeContractDialogTitle = "IP Veri Saklama ve Silme Taahhüdü"
                                activeContractDialogBody = "IP adresleri siber sızma testleri ve KVKK uyumluluk denetimleri çerçevesinde yerel SQLite tablosunda kriptolu saklanır. Hesabınızın silinmesi durumunda tüm IP kayıtlarınız 72 saatlik nadas bekleme havuzuna alınır. Havuz süresi dolduğunda yedekler 6 ay boyunca soğuk saklama kutusunda korunacak ve süre bitiminde sistem tarafından hiçbir kalıntı bırakılmaksızın sonsuza dek tamamen temizlenecektir."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("IP Saklama Koşulları", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            // EMAIL SUPPORT INBOX FORM (Müşteri Destek Formu)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Cly Web Teknik Destek Masası", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Müşteri temsilcilerine doğrudan e-posta ile iletilecek destek talebini yazın.", fontSize = 10.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = supportSubjectInput,
                        onValueChange = { supportSubjectInput = it },
                        label = { Text("E-Posta Destek Konusu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = supportMessageInput,
                        onValueChange = { supportMessageInput = it },
                        label = { Text("Teknik Mesajınız / Sorununuz") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (viewModel.dispatchSupportTicket(supportSubjectInput, supportMessageInput)) {
                                supportSubjectInput = ""
                                supportMessageInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("E-Posta Destek Talebi İlet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Doğrudan E-Posta: destek@clyweb.com",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 72-HOUR ACCOUNT DELETION MECHANISM (KVKK COMPLIANCE)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (deletionTime != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, if (deletionTime != null) Color.Red else Color.LightGray.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hesap ve Veri Temizleme Köşesi", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (deletionTime != null) Color.Red else Color.White)

                    if (deletionTime == null) {
                        Text(
                            text = "Kanuni Hesap Silme Politikası: Hesabınızı sildiğinizde, 72 saatlik bir ön güvenlik bekleme süresi başlar. Bu sürenin ardından tüm lisans, IP ve B2B verileriniz otomatik yedeklemelerde 6 ay muhafaza edildikten sonra KVKK uyarınca kalıcı silinir.",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hesabımı Kalıcı Olarak Silme İşlemini Başlat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Deletion queue active countdown
                        val timeLeftMillis = deletionTime!! - currentTimeMillis
                        val hoursLeft = if (timeLeftMillis > 0) timeLeftMillis / (1000 * 60 * 60) else 0L
                        val minutesLeft = if (timeLeftMillis > 0) (timeLeftMillis / (1000 * 60)) % 60 else 0L
                        val secondsLeft = if (timeLeftMillis > 0) (timeLeftMillis / 1000) % 60 else 0L

                        Surface(
                            color = Color.DarkGray,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "⚠️ HESAP SİLME İŞLEMİ SIRAYA ALINDI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Red
                                )
                                Text(
                                    text = "Güvenli Kalan Zaman: ${hoursLeft} S, ${minutesLeft} D, ${secondsLeft} S",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = "72 saatlik ön hold kuralı devrededir. Bilgiler 6 ay boyunca şifreli yedek kasada korunduktan sonra otomatik temizlenecektir.",
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.cancelAccountDeletion() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hesap Silme Talebini İptal Et ve Güvende Kal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

    // Contracts Popup Dialog
    if (activeContractDialogTitle != null) {
        AlertDialog(
            onDismissRequest = {
                activeContractDialogTitle = null
                activeContractDialogBody = null
            },
            title = { Text(activeContractDialogTitle ?: "") },
            text = { Text(activeContractDialogBody ?: "", fontSize = 12.sp, lineHeight = 16.sp) },
            confirmButton = {
                TextButton(onClick = {
                    activeContractDialogTitle = null
                    activeContractDialogBody = null
                }) {
                    Text("Okudum, Onaylıyorum")
                }
            }
        )
    }

    // 72h Deletion Prompt alert confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hesap Silme İşlemini Onaylıyor musunuz?") },
            text = {
                Text(
                    "Hesabınız ve tüm ilişkili B2B verileriniz için 72 saatlik kalkan hold havuzu oluşturulacaktır. Silme emrini dilediğiniz an bu ekrandan iptal edebilirsiniz. 72 saat sonunda veriler siber yedek kutusunda 6 ay muhafaza edilip otomatik imha edilir. Devam etmek istiyor musunuz?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    onClick = {
                        viewModel.scheduleAccountDeletion()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Evet, Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Security Audit Database Terminal logs popup dialog
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981))
                    Text("SQLite Ağ ve Güvenlik Denetleme Kaydı", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SQLite veri tabanına işlenen son şifreli güvenlik ve IP işlem kayıtları:", fontSize = 10.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        val authLogs = systemLogs.filter { log -> log.tag == "AUTH" || log.tag == "SECURITY" || log.tag == "SUPPORT" || log.tag == "FEEDBACK" }
                        if (authLogs.isEmpty()) {
                            Text(
                                "Henüz bir güvenlik log kaydı işlenmedi. İşlemler gerçekleştikçe IP adresleri buraya kilitlenecektir.",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                authLogs.forEach { log ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "[${log.tag ?: "SEC"}] ${log.operatorName}",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Veri Depolandı",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                        Text(
                                            text = log.actionDetails,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.LightGray,
                                            lineHeight = 12.sp
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
}
