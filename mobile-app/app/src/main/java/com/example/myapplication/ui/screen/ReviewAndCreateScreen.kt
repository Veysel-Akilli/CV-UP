@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.myapplication.ui.screen

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapplication.utils.saveAsPdfAts
import com.example.myapplication.viewmodel.DocumentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


private val AppLinkColor = Color(0xFF64B5F6)
private val AppAccentLight = Color(0xFF90CAF9)
private val AppPrimary = Color(0xFF1565C0)
private val AppScreenGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0F2027),
        Color(0xFF203A43),
        Color(0xFF2C5364)
    )
)

@Composable
private fun appPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = AppPrimary,
    contentColor = Color.White,
    disabledContainerColor = AppPrimary.copy(alpha = 0.4f),
    disabledContentColor = Color.White.copy(alpha = 0.7f)
)

@Composable
private fun appFilterChipColors() =
    FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = AppPrimary.copy(alpha = 0.18f),
        selectedLabelColor = AppPrimary,
        selectedLeadingIconColor = AppPrimary
    )

@Composable
private fun appAssistChipColors() =
    AssistChipDefaults.assistChipColors(
        containerColor = AppAccentLight.copy(alpha = 0.14f),
        labelColor = AppPrimary,
        leadingIconContentColor = AppPrimary
    )


@Composable
fun ReviewAndCreateScreen(
    token: String,
    navController: NavController,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }

    var localPreview by rememberSaveable { mutableStateOf("") }
    val versions = remember { mutableStateListOf<String>() }

    var mode by rememberSaveable { mutableStateOf("ATS") }
    var fontScale by rememberSaveable { mutableFloatStateOf(1.0f) }

    var showSheet by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    LaunchedEffect(viewModel.generatedContent) {
        if (viewModel.generatedContent.isNotBlank()) {
            localPreview = viewModel.generatedContent
            if (versions.isEmpty() || versions.last() != localPreview) {
                versions += localPreview
                while (versions.size > 5) versions.removeAt(0)
            }
        }
    }

    DisposableEffect(Unit) { onDispose { localPreview = "" } }

    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CV Önizleme", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (localPreview.isBlank()) "İçerik hazır değil" else "İçerik hazır – düzenleyip dışa aktarın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.Home) }) {
                        Icon(Icons.Outlined.Home, contentDescription = "Ana Sayfa", tint = AppLinkColor)
                    }
                },
                actions = {
                    IconButton(onClick = { overflowMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Daha fazla", tint = AppLinkColor)
                    }
                    DropdownMenu(expanded = overflowMenu, onDismissRequest = { overflowMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Yeniden Oluştur (AI)") },
                            onClick = {
                                overflowMenu = false
                                viewModel.generateDocument(token)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("İyileştir (AI)") },
                            onClick = {
                                overflowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("EN Çevir (AI)") },
                            onClick = {
                                overflowMenu = false
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            AnimatedVisibility(visible = localPreview.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                ExtendedFloatingActionButton(
                    text = { Text("İşlemler") },
                    icon = { Icon(Icons.Outlined.MoreVert, contentDescription = null) },
                    onClick = { showSheet = true },
                    containerColor = AppPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    ) { padding ->

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppScreenGradient)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp)
        ) {
            if (localPreview.isNotBlank()) {
                item {
                    val issues = remember(localPreview) { computeAtsIssues(localPreview) }
                    val pageEstimate = remember(localPreview) { approxPages(localPreview) }
                    val score = remember(issues) { (100 - issues.sumOf { it.penalty }).coerceIn(0, 100) }

                    val headerGradient = Brush.linearGradient(
                        listOf(
                            AppPrimary.copy(alpha = 0.18f),
                            AppAccentLight.copy(alpha = 0.12f)
                        )
                    )

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            Modifier
                                .background(headerGradient)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Kalite Kontrol", style = MaterialTheme.typography.titleMedium)
                                InfoPill(text = String.format("~%.1f syf.", pageEstimate))
                            }
                            LinearProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(50)),
                            color = AppPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("ATS Skoru: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$score / 100", style = MaterialTheme.typography.titleMedium)
                            }
                            if (issues.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    issues.forEach { w -> WarningChip(w.message) }
                                }
                            } else {
                                Text("Hiç uyarı yok. Harika görünüyor! 🎉", color = AppPrimary)
                            }
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Henüz önizleme yok", style = MaterialTheme.typography.titleMedium)

                            Text(
                                text = buildAnnotatedString {
                                    val accent = AppLinkColor
                                    append("Bölümleri doldurun ya da ")
                                    withStyle(
                                        SpanStyle(
                                            color = accent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append("‘CV Oluştur (ATS)’")
                                    }
                                    append(" ile taslak üretin. Sonra burada ")
                                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                                        append("önizleyip dışa aktarabilirsiniz")
                                    }
                                    append(".")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { navController.navigate(Routes.section("personal")) },
                                    colors = appPrimaryButtonColors(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Bölümlere Git")
                                }
                                TextButton(
                                    onClick = { viewModel.generateDocument(token) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                                ) {
                                    Text("CV Oluştur (ATS)")
                                }
                            }
                        }
                    }
                }
            }

            item {
                PreviewControlsPro(
                    mode = mode,
                    onMode = { mode = it },
                    fontScale = fontScale,
                    onFont = { fontScale = it }
                )
            }

            item {
                SectionsShortcutsGrid(
                    onClick = { key -> navController.navigate("cv/section/$key") }
                )
            }

            item {
                MissingCheckModern(
                    onFix = { key -> navController.navigate("cv/section/$key") },
                    vm = viewModel
                )
            }

            item {
                Button(
                    onClick = {
                        viewModel.updateField("ats_mode", "true")
                        viewModel.updateField(
                            "sections_order",
                            "Contact, Summary, Skills, Experience, Education, Projects, Certificates, Languages, References"
                        )
                        viewModel.updateField(
                            "forbidden_elements",
                            "tables, images, icons, graphics, two-columns"
                        )
                        viewModel.updateField(
                            "format_rules",
                            "single-column; plain text; clear headers; bullet points; measurable impact; action verbs"
                        )
                        viewModel.updateField("contact_inline", "true")
                        viewModel.updateField("max_length", "2 pages")
                        viewModel.generateDocument(token)
                    },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = appPrimaryButtonColors()
                ) { Text("✨ CV Oluştur (ATS)") }
            }

            if (viewModel.isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AppPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            if (localPreview.isNotBlank()) {
                item {
                    VersionBar(
                        versions = versions,
                        onSelect = { idx -> if (idx in versions.indices) localPreview = versions[idx] }
                    )
                }
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Önizleme", style = MaterialTheme.typography.titleMedium)
                            val formatted = remember(localPreview, mode) { formatForMode(localPreview, mode) }
                            val base = MaterialTheme.typography.bodyMedium.fontSize
                            val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = base * fontScale,
                                lineHeight = base * fontScale * 1.35f
                            )
                            SelectionContainer {
                                Text(
                                    text = formatted,
                                    fontFamily = if (mode == "ATS") FontFamily.Monospace else FontFamily.Default,
                                    softWrap = true,
                                    style = textStyle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.fieldValues.any { it.value.isNotBlank() }) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Girilen Alanlar", style = MaterialTheme.typography.titleMedium)
                            viewModel.fieldValues.forEach { (k, v) ->
                                if (v.isNotBlank()) {
                                    Text("• ${k.replace('_', ' ').replaceFirstChar { it.uppercase() }}: $v")
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.errorMessage.isNotBlank()) {
                item {
                    Text(
                        "❌ Hata: ${viewModel.errorMessage}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (showSheet && localPreview.isNotBlank()) {
            ActionsSheetModern(
                preview = localPreview,
                onCopy = {
                    clipboard.setText(AnnotatedString(localPreview))
                },
                onSaveAts = {
                    scope.launch {
                        try {
                            val fileName = "cv_${System.currentTimeMillis()}.pdf"
                            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                            val file = File(dir, fileName)

                            withContext(Dispatchers.IO) {
                                saveAsPdfAts(localPreview, file)
                            }
                            viewModel.uploadDocument(token, file, "CV", "Oluşturulan CV (ATS)")

                            snackbar.showSnackbar("Kaydedildi: ${file.absolutePath}")
                        } catch (e: Exception) {
                            snackbar.showSnackbar("Kaydetme hatası: ${e.localizedMessage}")
                        }
                    }
                },
                onShareMasked = {
                    val masked = maskPII(localPreview)
                    clipboard.setText(AnnotatedString(masked))
                },
                onClearAndHome = {
                    viewModel.clearDraftLocal()
                    showSheet = false
                    navController.navigate(Routes.Home)
                },
                onDismiss = { showSheet = false }
            )
        }
    }
}


@Composable
private fun PreviewControlsPro(
    mode: String,
    onMode: (String) -> Unit,
    fontScale: Float,
    onFont: (Float) -> Unit
) {
    val headerGradient = Brush.linearGradient(
        listOf(
            AppPrimary.copy(alpha = 0.12f),
            AppAccentLight.copy(alpha = 0.10f)
        )
    )

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            Modifier
                .background(headerGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Görünüm", style = MaterialTheme.typography.titleMedium)
                InfoPill(
                    text = when (mode) {
                        "ATS" -> "ATS Modu"
                        "Recruiter" -> "İK Modu"
                        else -> "Yazdırma"
                    }
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ATS", "Recruiter", "Print").forEach { m ->
                    val selected = mode == m
                    FilterChip(
                        selected = selected,
                        onClick = { onMode(m) },
                        label = { Text(m) },
                        leadingIcon = {
                            if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = appFilterChipColors()
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yazı Boyutu")
                Slider(
                    value = fontScale,
                    onValueChange = onFont,
                    valueRange = 0.9f..1.2f,
                    steps = 2,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AppPrimary,
                        activeTrackColor = AppPrimary,
                        inactiveTrackColor = AppAccentLight.copy(alpha = 0.35f),
                        activeTickColor = AppPrimary.copy(alpha = 0.6f),
                        inactiveTickColor = AppAccentLight.copy(alpha = 0.5f)
                    )
                )
                InfoPill(text = String.format("%.0f%%", fontScale * 100))
            }
        }
    }
}




@Composable
private fun SectionsShortcutsGrid(onClick: (String) -> Unit) {
    val items = listOf(
        "personal" to "Contact",
        "objective" to "Summary",
        "skills" to "Skills",
        "experience" to "Experience",
        "education" to "Education",
        "projects" to "Projects",
        "courses" to "Certificates",
        "languages" to "Languages",
        "references" to "References"
    )
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bölüm Kısayolları", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { (key, title) ->
                    AssistChip(
                        onClick = { onClick(key) },
                        label = { Text(title) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null) },
                        colors = appAssistChipColors()
                    )
                }
            }
        }
    }
}


@Composable
private fun MissingCheckModern(onFix: (String) -> Unit, vm: DocumentViewModel) {
    val sections = listOf(
        CvSection("personal","Kişisel Bilgiler","", Icons.Outlined.Person, optional = false),
        CvSection("objective","Kariyer Hedefi","", Icons.Outlined.Flag, optional = false),
        CvSection("experience","İş Deneyimleri","", Icons.Outlined.Badge, optional = false),
        CvSection("education","Eğitim","", Icons.Outlined.School, optional = false),
        CvSection("skills","Yetenekler","", Icons.Outlined.Star, optional = false),
        CvSection("languages","Diller","", Icons.Outlined.Language, optional = true),
        CvSection("courses","Kurslar & Sertifikalar","", Icons.Outlined.CardMembership, optional = true),
        CvSection("projects","Projeler","", Icons.Outlined.Code, optional = true),
        CvSection("references","Referanslar","", Icons.Outlined.People, optional = true),
    )

    val statuses = sections.associateWith { s ->
        evaluateStatusAlignedWithSectionScreen(s.key, vm, s.optional)
    }

    val problems = sections.filter { s ->
        val st = statuses[s]!!
        (!s.optional && !st.requiredComplete) || (s.optional && st.hasData && !st.optionalValid)
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Eksik Kontrolü", style = MaterialTheme.typography.titleMedium)
            if (problems.isEmpty()) {
                Text("Zorunlu alanlarda eksik yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    problems.forEach { s ->
                        val st = statuses[s]!!
                        val label = when {
                            !s.optional && !st.requiredComplete -> s.title
                            s.optional && st.hasData && !st.optionalValid -> "${s.title} (ops.)"
                            else -> s.title
                        }
                        AssistChip(
                            onClick = { onFix(s.key) },
                            label = { Text(label) },
                            leadingIcon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun ActionsSheetModern(
    preview: String,
    onCopy: () -> Unit,
    onSaveAts: () -> Unit,
    onShareMasked: () -> Unit,
    onClearAndHome: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ListItem(
                headlineContent = { Text("Kopyala") },
                supportingContent = { Text("Metni panoya kopyala") },
                leadingContent = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth()
                    .clickable { onCopy(); onDismiss() }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            ListItem(
                headlineContent = { Text("PDF (ATS) olarak kaydet") },
                supportingContent = { Text("ATS uyumlu PDF’e dönüştür ve sakla") },
                leadingContent = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth(),
                trailingContent = {
                    TextButton(
                        onClick = { onSaveAts(); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                    ) { Text("Kaydet") }
                }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            ListItem(
                headlineContent = { Text("Gizlilik modunda kopyala") },
                supportingContent = { Text("E-posta/telefon maskelemesi ile paylaş") },
                leadingContent = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth(),
                trailingContent = {
                    TextButton(
                        onClick = { onShareMasked(); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                    ) { Text("Kopyala") }
                }
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            ListItem(
                headlineContent = { Text("Temizle ve Ana Sayfa") },
                leadingContent = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Home, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth(),
                trailingContent = {
                    TextButton(
                        onClick = { onClearAndHome() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                    ) { Text("Git") }
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}



@Composable
private fun VersionBar(versions: List<String>, onSelect: (Int) -> Unit) {
    if (versions.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        versions.forEachIndexed { i, _ ->
            FilterChip(
                selected = i == versions.lastIndex,
                onClick = { onSelect(i) },
                label = { Text(if (i == versions.lastIndex) "Son" else "#${i + 1}") },
                leadingIcon = { if (i == versions.lastIndex) Icon(Icons.Outlined.CheckCircle, null) },
                colors = appFilterChipColors()
            )
        }
    }
}



@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = AppAccentLight.copy(alpha = 0.18f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = AppPrimary
        )
    }
}

@Composable
private fun WarningChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}



private fun formatForMode(text: String, mode: String): String = when (mode) {
    "Recruiter" -> text
        .replace(Regex("^([A-ZÇĞİÖŞÜ ]{3,})\\s*$", RegexOption.MULTILINE)) { m ->
            "## ${m.value.trim().uppercase()}"
        }
        .replace(Regex("(?m)^-\\s+"), "• ")
    "Print" -> text.replace(Regex("\\n{3,}"), "\n\n")
    else -> {
        text.replace(Regex("[\\p{So}\\p{Sk}]"), "")
            .replace(Regex("\\t+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
    }
}

private fun approxPages(text: String): Float {
    val approxCharsPerPage = 3500f
    return (text.length / approxCharsPerPage).coerceAtLeast(0.01f)
}

private data class AtsIssue(val id: String, val message: String, val penalty: Int)

private fun computeAtsIssues(text: String): List<AtsIssue> {
    val out = mutableListOf<AtsIssue>()

    if (Regex("[\\p{So}\\p{Sk}]").containsMatchIn(text)) {
        out += AtsIssue("emoji", "Emoji/simge tespit edildi (ATS için temizleyin).", 20)
    }

    val tableLike = Regex("(?m)^(?:[^\\n]*\\|){3,}[^\\n]*").containsMatchIn(text) ||
            Regex("[│┌┬┐└┴┘]").containsMatchIn(text)
    if (tableLike) out += AtsIssue("table", "Tablo benzeri içerik tespit edildi.", 25)

    if (Regex(" {4,}").containsMatchIn(text)) {
        out += AtsIssue("spaces", "Hizalama için çoklu boşluk kullanımı tespit edildi.", 5)
    }

    if (Regex("!\\[[^]]*]\\([^)]*\\)").containsMatchIn(text)) {
        out += AtsIssue("image", "Görsel/ikon kullanımına dikkat edin (ATS).", 20)
    }

    val experiencePresent = Regex("(?mi)^DENEYİM\\b|^DENEYIM\\b").containsMatchIn(text)
    if (experiencePresent) {
        val bullets = Regex("(?m)^[ \\t]*-\\s+").findAll(text).count()
        val target = 6
        if (bullets < target) {
            out += AtsIssue("bullets", "Madde işareti az — başarıları maddelendirmeniz önerilir.", 10)
        }
    }

    val pages = approxPages(text)
    if (pages > 2.0f) {
        out += AtsIssue("length", "2 sayfayı aşıyor (~%.1f)".format(pages), 10)
    }

    return out
}

private fun maskPII(text: String): String {
    var s = text
    s = s.replace(
        Regex("\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+)\\.(\\w{2,})\\b")
    ) { "${it.groupValues[1]}***@${it.groupValues[2]}.${it.groupValues[3]}" }
    s = s.replace(Regex("\\b\\+?\\d[\\d\\s-]{8,}\\d\\b")) { match ->
        val digits = match.value.filter { it.isDigit() }
        if (digits.length in 9..13) "***${digits.takeLast(4)}" else match.value
    }
    return s
}



private const val ROW_SEP = "<|row|>"
private const val COL_SEP = "||"

private data class CvSection(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val optional: Boolean
)

private data class SectionStatus(
    val requiredComplete: Boolean = false,
    val hasData: Boolean = false,
    val optionalValid: Boolean = true,
    val message: String? = null
)

private fun parseRows(raw: String?): List<List<String>> {
    val src = raw.orEmpty()
    if (src.isBlank()) return emptyList()
    val rows = if (src.contains(ROW_SEP)) src.split(ROW_SEP) else src.split("\n")
    return rows.map { it.trim() }.filter { it.isNotBlank() }.map { r -> r.split(COL_SEP).map { it.trim() } }
}

private fun evaluateStatusAlignedWithSectionScreen(
    key: String,
    vm: DocumentViewModel,
    optional: Boolean
): SectionStatus {
    fun nonBlank(v: String?) = !v.isNullOrBlank()

    return when (key) {
        "personal" -> {
            val hasIsim  = nonBlank(vm.fieldValues["isim"])
            val hasMail  = nonBlank(vm.fieldValues["email"])
            val num      = vm.fieldValues["phone_num"].orEmpty()
            val phoneOk  = num.length == 10 && num.firstOrNull() != '0'
            val ok       = hasIsim && hasMail && phoneOk
            val msg = when {
                ok -> null
                !hasIsim || !hasMail -> "İsim ve E-posta zorunlu."
                true -> "Telefon 10 hane olmalı ve 0 ile başlamamalı."
                else -> "Eksikler var."
            }
            SectionStatus(requiredComplete = ok, hasData = (hasIsim || hasMail || num.isNotBlank()), message = msg)
        }

        "objective" -> {
            val hasPos = nonBlank(vm.fieldValues["pozisyon"])
            val hasObj = nonBlank(vm.fieldValues["objective"])
            val ok = hasPos && hasObj
            SectionStatus(requiredComplete = ok, hasData = hasPos || hasObj, message = if (ok) null else "Hedef pozisyon ve metin zorunlu.")
        }

        "experience" -> {
            val r = parseRows(vm.fieldValues["experience_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(2).isNullOrBlank() ||
                        row.getOrNull(3).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank()
            }
            val ok = hasData && bad < 0
            SectionStatus(requiredComplete = ok, hasData = hasData, message = if (ok) null else "En az bir deneyim ve başlangıç (Ay/Yıl) zorunlu.")
        }

        "education" -> {
            val r = parseRows(vm.fieldValues["education_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(2).isNullOrBlank() ||
                        row.getOrNull(3).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank() ||
                        ((row.getOrNull(5).isNullOrBlank()) xor (row.getOrNull(6).isNullOrBlank()))
            }
            val ok = hasData && bad < 0
            val msg = if (ok) null else "Program, Kurum, Konum, Başlangıç (Ay/Yıl) zorunlu. Bitiş için Ay/Yıl ikilisini birlikte seçin veya boş bırakın."
            SectionStatus(requiredComplete = ok, hasData = hasData, message = msg)
        }

        "skills" -> {
            val raw = vm.fieldValues["skills_list"].orEmpty()
            val list = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            val ok = list.isNotEmpty()
            SectionStatus(requiredComplete = ok, hasData = list.isNotEmpty(), message = if (ok) null else "En az bir yetenek girin.")
        }

        "languages" -> {
            val r = parseRows(vm.fieldValues["languages_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            val valid = bad < 0
            SectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Dil ve Seviye zorunlu." else null)
        }

        "courses" -> {
            val r = parseRows(vm.fieldValues["courses_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() || row.getOrNull(2).isNullOrBlank()
            }
            val valid = bad < 0
            SectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Ad, Kurum ve Açıklama zorunlu." else null)
        }

        "projects" -> {
            val r = parseRows(vm.fieldValues["projects_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            val valid = bad < 0
            SectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Proje Adı ve Açıklama zorunlu." else null)
        }

        "references" -> {
            val r = parseRows(vm.fieldValues["references_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank() ||
                        row.getOrNull(5).isNullOrBlank()
            }
            val valid = bad < 0
            SectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "İsim, İlişki, E-posta ve Telefon zorunlu." else null)
        }

        else -> SectionStatus()
    }
}



object Routes {
    const val Home = "home"
    private const val SectionBase = "cv/section"
    fun section(name: String) = "$SectionBase/$name"
}
