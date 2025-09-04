@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.myapplication.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.viewmodel.DocumentViewModel

private val AppLinkColor    = Color(0xFF64B5F6)
private val AppAccentLight  = Color(0xFF90CAF9)
private val AppPrimary      = Color(0xFF1565C0)
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
fun GenerateScreen(
    documentType: String,
    token: String,
    navController: NavController,
    viewModel: DocumentViewModel
) {
    LaunchedEffect(documentType) { viewModel.clearGenerated() }

    val sections = remember {
        listOf(
            GenerateSectionMeta("personal",   "Kişisel Bilgiler",              "Ad, e-posta, telefon",          Icons.Outlined.Person,         optional = false),
            GenerateSectionMeta("objective",  "Kariyer Hedefi",                "Kısa hedef (AI ile yazdır)",    Icons.Outlined.Flag,           optional = false),
            GenerateSectionMeta("experience", "İş Deneyimleri",                "Pozisyonlar ve katkılar",       Icons.Outlined.Badge,          optional = false),
            GenerateSectionMeta("education",  "Eğitim",                        "Okul, program, yıllar",         Icons.Outlined.School,         optional = false),
            GenerateSectionMeta("skills",     "Yetenekler",                    "Teknik ve sosyal yetenekler",   Icons.Outlined.Star,           optional = false),
            GenerateSectionMeta("languages",  "Diller (Opsiyonel)",            "Dil ve seviye",                 Icons.Outlined.Language,       optional = true),
            GenerateSectionMeta("courses",    "Kurslar & Sertifikalar (Ops.)", "Kurum ve açıklama",             Icons.Outlined.CardMembership, optional = true),
            GenerateSectionMeta("projects",   "Projeler (Opsiyonel)",          "Açıklama ve teknolojiler",      Icons.Outlined.Code,           optional = true),
            GenerateSectionMeta("references", "Referanslar (Opsiyonel)",       "Kişi ve iletişim bilgileri",    Icons.Outlined.People,         optional = true),
        )
    }

    val completionMap = remember { mutableStateMapOf<String, GenerateSectionStatus>() }
    LaunchedEffect(viewModel.fieldValues) {
        completionMap.clear()
        sections.forEach { s ->
            completionMap[s.key] = evaluateStatusAlignedWithSectionScreen(s.key, viewModel, s.optional)
        }
    }

    val required = sections.filterNot { it.optional }
    val optional = sections.filter { it.optional }

    val totalRequired = required.size
    val completedRequired = required.count { completionMap[it.key]?.requiredComplete == true }
    val progress = if (totalRequired == 0) 0f else completedRequired.toFloat() / totalRequired.toFloat()
    val progressAnim by animateFloatAsState(targetValue = progress, label = "overallProgress")
    val allRequiredDone = completedRequired == totalRequired

    val optionalDone = optional.count { st ->
        val s = completionMap[st.key]
        s?.hasData == true && s.optionalValid
    }

    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri", tint = AppLinkColor)
                    }
                },
                title = { Text("CV Düzenle", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {  }) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Yardım", tint = AppLinkColor)
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                progress = progressAnim,
                allDone = allRequiredDone,
                onCreateClick = {
                    if (allRequiredDone) {
                        viewModel.clearGenerated()
                        navController.navigate("cv/review")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppScreenGradient)
                .padding(padding)
                .verticalScroll(scroll)
        ) {
            HeroHeader(
                completedRatio = progressAnim,
                requiredDone = completedRequired,
                requiredTotal = totalRequired,
                optionalDone = optionalDone,
                optionalTotal = optional.size
            )
            Spacer(Modifier.height(8.dp))

            sections.forEach { s ->
                val status = completionMap[s.key] ?: GenerateSectionStatus()
                SectionTile(
                    section = s,
                    status = status,
                    onClick = { navController.navigate("cv/section/${s.key}") }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}


@Composable
private fun HeroHeader(
    completedRatio: Float,
    requiredDone: Int,
    requiredTotal: Int,
    optionalDone: Int,
    optionalTotal: Int
) {
    val bg = Brush.linearGradient(
        listOf(
            AppPrimary.copy(alpha = 0.14f),
            AppAccentLight.copy(alpha = 0.12f)
        )
    )
    val percentText = String.format("%d%%", (completedRatio.coerceIn(0f, 1f) * 100f).toInt())

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(16.dp)
        ) {
            Text("CV Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Zorunlu bölümleri tamamla; opsiyonelleri istersen atlayabilirsin. Sonra önizleyip PDF oluştur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                progress = { completedRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                color = AppPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Spacer(Modifier.width(10.dp))
                InfoPill(percentText)
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("Zorunlu", "$requiredDone / $requiredTotal")
                MetricPill("Opsiyonel", "$optionalDone / $optionalTotal")
            }
        }
    }
}

@Composable
private fun MetricPill(title: String, value: String) {
    val isRequired = title.equals("Zorunlu", ignoreCase = true)
    val container = if (isRequired) AppPrimary.copy(alpha = 0.14f) else AppAccentLight.copy(alpha = 0.18f)
    val fg = if (isRequired) AppPrimary else AppPrimary

    Surface(shape = RoundedCornerShape(50), color = container) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Icon(if (isRequired) Icons.Outlined.CheckCircle else Icons.Outlined.Star, contentDescription = null, tint = fg)
            Spacer(Modifier.width(6.dp))
            Text("$title:", style = MaterialTheme.typography.labelMedium, color = fg)
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = fg)
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
private fun BottomBar(
    progress: Float,
    allDone: Boolean,
    onCreateClick: () -> Unit
) {
    val clamped = progress.coerceIn(0f, 1f)
    val percentText = "${(clamped * 100).toInt()}%"

    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (allDone) "Hazır! Önizleyip oluşturabilirsin."
                    else "Zorunlu alanlar tamamlanınca devam edebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onCreateClick,
                    enabled = allDone,
                    shape = RoundedCornerShape(12.dp),
                    colors = appPrimaryButtonColors()
                ) { Text("Önizle ve Oluştur") }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                progress = { clamped },
                modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                color = AppPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Spacer(Modifier.width(10.dp))
                Surface(shape = RoundedCornerShape(50), color = AppAccentLight.copy(alpha = 0.18f)) {
                    Text(
                        percentText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPrimary
                    )
                }
            }
        }
    }
}



@Composable
private fun SectionTile(
    section: GenerateSectionMeta,
    status: GenerateSectionStatus,
    onClick: () -> Unit
) {
    val isRequired = !section.optional
    val showCheck = isRequired && status.requiredComplete

    val container = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val iconCapsule = Brush.linearGradient(
        listOf(AppPrimary.copy(alpha = 0.12f), AppAccentLight.copy(alpha = 0.12f))
    )

    val (stateText, bgAlpha) = when {
        isRequired && status.requiredComplete -> "Tamamlandı" to 0.18f
        isRequired && !status.requiredComplete -> "Eksikler var" to 0.18f
        !status.hasData -> "Opsiyonel" to 0.12f
        status.hasData && status.optionalValid -> "Eklendi" to 0.18f
        else -> "Eksik var (ops.)" to 0.18f
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconCapsule),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showCheck) Icons.Outlined.CheckCircle else section.icon,
                    contentDescription = null,
                    tint = AppPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (stateText == "Eksikler var" || stateText.contains("Eksik"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        AppAccentLight.copy(alpha = bgAlpha)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        val iv = when (stateText) {
                            "Tamamlandı" -> Icons.Outlined.CheckCircle
                            "Eksikler var", "Eksik var (ops.)" -> Icons.Outlined.ErrorOutline
                            "Eklendi" -> Icons.Outlined.Done
                            else -> Icons.Outlined.Info
                        }
                        Icon(
                            iv,
                            contentDescription = null,
                            tint = if (stateText.startsWith("Eksik")) MaterialTheme.colorScheme.onErrorContainer else AppPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stateText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (stateText.startsWith("Eksik")) MaterialTheme.colorScheme.onErrorContainer else AppPrimary
                        )
                    }
                }

                val shouldShowMsg =
                    (!section.optional && !status.requiredComplete && !status.message.isNullOrBlank()) ||
                            (section.optional && status.hasData && !status.message.isNullOrBlank())

                AnimatedVisibility(visible = shouldShowMsg, enter = fadeIn(), exit = fadeOut()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(status.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = AppLinkColor)
        }
    }
}


private data class GenerateSectionMeta(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val optional: Boolean
)

private data class GenerateSectionStatus(
    val requiredComplete: Boolean = false,
    val hasData: Boolean = false,
    val optionalValid: Boolean = true,
    val message: String? = null
)


private const val ROW_SEP = "<|row|>"
private const val COL_SEP = "||"

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
): GenerateSectionStatus {
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
                else -> "Telefon 10 hane olmalı ve 0 ile başlamamalı."
            }
            GenerateSectionStatus(requiredComplete = ok, hasData = (hasIsim || hasMail || num.isNotBlank()), message = msg)
        }
        "objective" -> {
            val hasPos = nonBlank(vm.fieldValues["pozisyon"])
            val hasObj = nonBlank(vm.fieldValues["objective"])
            val ok = hasPos && hasObj
            GenerateSectionStatus(requiredComplete = ok, hasData = hasPos || hasObj, message = if (ok) null else "Hedef pozisyon ve metin zorunlu.")
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
            GenerateSectionStatus(requiredComplete = ok, hasData = hasData, message = if (ok) null else "En az bir deneyim ve başlangıç (Ay/Yıl) zorunlu.")
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
            GenerateSectionStatus(requiredComplete = ok, hasData = hasData, message = msg)
        }
        "skills" -> {
            val raw = vm.fieldValues["skills_list"].orEmpty()
            val list = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            val ok = list.isNotEmpty()
            GenerateSectionStatus(requiredComplete = ok, hasData = list.isNotEmpty(), message = if (ok) null else "En az bir yetenek girin.")
        }
        "languages" -> {
            val r = parseRows(vm.fieldValues["languages_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            val valid = bad < 0
            GenerateSectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Dil ve Seviye zorunlu." else null)
        }
        "courses" -> {
            val r = parseRows(vm.fieldValues["courses_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() || row.getOrNull(2).isNullOrBlank()
            }
            val valid = bad < 0
            GenerateSectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Ad, Kurum ve Açıklama zorunlu." else null)
        }
        "projects" -> {
            val r = parseRows(vm.fieldValues["projects_list"])
            val hasData = r.isNotEmpty()
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            val valid = bad < 0
            GenerateSectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "Proje Adı ve Açıklama zorunlu." else null)
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
            GenerateSectionStatus(requiredComplete = false, hasData = hasData, optionalValid = !hasData || valid, message = if (hasData && !valid) "İsim, İlişki, E-posta ve Telefon zorunlu." else null)
        }
        else -> GenerateSectionStatus()
    }
}
