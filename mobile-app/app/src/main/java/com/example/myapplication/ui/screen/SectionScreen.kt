@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.myapplication.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import com.example.myapplication.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch


private val AppLinkColor = Color(0xFF64B5F6)
private val AppAccentLight = Color(0xFF90CAF9)
private val AppPrimary = Color(0xFF1565C0)

@Composable
private fun appPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = AppPrimary,
    contentColor = Color.White,
    disabledContainerColor = AppPrimary.copy(alpha = 0.4f),
    disabledContentColor = Color.White.copy(alpha = 0.7f)
)

@Composable
private fun appOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = AppLinkColor)

@Composable
private fun appTonalButtonColors(): ButtonColors =
    ButtonDefaults.filledTonalButtonColors(
        containerColor = AppPrimary.copy(alpha = 0.12f),
        contentColor = AppPrimary
    )

private val AppOutlinedButtonBorder = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(listOf(AppLinkColor, AppAccentLight))
)

@Composable
fun SectionScreen(
    section: String,
    token: String,
    navController: NavController,
    viewModel: DocumentViewModel
) {
    val steps = remember {
        listOf(
            "personal"   to ("Kişisel Bilgiler" to Icons.Outlined.Person),
            "objective"  to ("Kariyer Hedefi" to Icons.Outlined.Flag),
            "experience" to ("İş Deneyimleri" to Icons.Outlined.Badge),
            "education"  to ("Eğitim" to Icons.Outlined.School),
            "skills"     to ("Yetenekler" to Icons.Outlined.Star),
            "languages"  to ("Diller (Opsiyonel)" to Icons.Outlined.Language),
            "courses"    to ("Kurslar & Sertifikalar (Opsiyonel)" to Icons.Outlined.CardMembership),
            "projects"   to ("Projeler (Opsiyonel)" to Icons.Outlined.Code),
            "references" to ("Referanslar" to Icons.Outlined.People),
        )
    }
    val idx = steps.indexOfFirst { it.first == section }.coerceAtLeast(0)
    val (title, icon) = steps[idx].second

    var localError by remember { mutableStateOf<String?>(null) }
    val saveEnabled by remember(section) { derivedStateOf { calcSaveEnabled(section, viewModel) } }

    fun onSave() {
        val fn = viewModel.fieldValues["first_name"].orEmpty().trim()
        val ln = viewModel.fieldValues["last_name"].orEmpty().trim()
        if (fn.isNotEmpty() || ln.isNotEmpty()) {
            viewModel.updateField("isim", listOf(fn, ln).filter { it.isNotBlank() }.joinToString(" "))
        }

        val (ok, msg) = validateSection(section, viewModel)
        if (!ok) { localError = msg ?: "Eksik/yanlış alanlar var."; return }
        normalizeAndSave(viewModel)
        viewModel.saveDraftLocal()
        navController.popBackStack()
    }

    val screenGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AppLinkColor)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        StepperPill(current = idx + 1, total = steps.size)
                    }
                }
            )
        },
        bottomBar = {
            BottomBarSlim(
                error = localError,
                saveEnabled = saveEnabled,
                onBack = { navController.popBackStack() },
                onSave = { onSave() }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(screenGradient)
                .padding(padding)
        ) {
            FancyHeader(icon = icon, title = title)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (section) {
                    "personal"   -> PersonalCard(viewModel)
                    "objective"  -> ObjectiveCard(viewModel, token)
                    "experience" -> ExperienceCard(viewModel, token)
                    "education"  -> EducationCard(viewModel)
                    "skills"     -> SkillsCard(viewModel)
                    "languages"  -> LanguagesCard(viewModel)
                    "courses"    -> CoursesCard(viewModel)
                    "projects"   -> ProjectsCard(viewModel)
                    "references" -> ReferencesCard(viewModel)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}


@Composable
private fun FancyHeader(icon: ImageVector, title: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            AppPrimary.copy(alpha = 0.18f),
            AppAccentLight.copy(alpha = 0.12f)
        )
    )
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Bilgileri modern kartlarda düzenle. Kaydetmeden önce kısa kontrol yapılır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepperPill(current: Int, total: Int) {
    val ratio = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(shape = RoundedCornerShape(50), tonalElevation = 3.dp) {
        Box(Modifier.width(200.dp).height(12.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(AppPrimary, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun BottomBarSlim(
    error: String?,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = !error.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                if (!error.isNullOrBlank()) {
                    Text("❌ $error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = appOutlinedButtonColors(),
                    border = AppOutlinedButtonBorder
                ) { Text("Geri") }
                Button(
                    onClick = onSave,
                    enabled = saveEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = appPrimaryButtonColors()
                ) { Text("Kaydet") }
            }
        }
    }
}



@Composable
private fun PersonalCard(vm: DocumentViewModel) {
    val firstInit = vm.fieldValues["first_name"] ?: ""
    val lastInit  = vm.fieldValues["last_name"] ?: ""
    val emailInit = vm.fieldValues["email"] ?: ""
    val addrInit  = vm.fieldValues["address"] ?: ""
    val numInit   = vm.fieldValues["phone_num"] ?: ""

    LaunchedEffect(Unit) { vm.updateField("phone_cc", "+90") }

    var first by remember { mutableStateOf(firstInit) }
    var last  by remember { mutableStateOf(lastInit) }
    var email by remember { mutableStateOf(emailInit) }
    var addr  by remember { mutableStateOf(addrInit) }
    var num   by remember { mutableStateOf(numInit) }

    fun save() {
        vm.updateField("first_name", first)
        vm.updateField("last_name",  last)
        vm.updateField("email",      email)
        vm.updateField("address",    addr)
        vm.updateField("phone_cc",   "+90")
        vm.updateField("phone_num",  num)
    }

    FormCard("Kişisel Bilgiler") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = first, onValueChange = { first = it; save() },
                label = { Text("Ad") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = last, onValueChange = { last = it; save() },
                label = { Text("Soyad") }, singleLine = true, modifier = Modifier.weight(1f)
            )
        }
        LabeledField("E-posta", email, { email = it; save() }, "ornek@mail.com", keyboardType = KeyboardType.Email)
        LabeledField("Adres (Opsiyonel)", addr, { addr = it; save() }, "İl/İlçe, Sokak, No", minLines = 2)

        Text("Telefon", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = "🇹🇷  +90",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Ülke Kodu") },
                singleLine = true,
                modifier = Modifier.width(130.dp)
            )
            OutlinedTextField(
                value = num,
                onValueChange = { newText ->
                    val digits = newText.filter { it.isDigit() }
                    num = digits.take(10)
                    vm.updateField("phone_num", num)
                },
                label = { Text("Numara (10 hane)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                supportingText = {
                    val ok = num.length == 10 && (num.firstOrNull() ?: '1') != '0'
                    Text(if (ok) "✓ Geçerli" else "10 hane olmalı ve 0 ile başlamamalı")
                }
            )
        }
    }
}


@Composable
private fun ObjectiveCard(vm: DocumentViewModel, token: String) {
    var pozisyon by remember { mutableStateOf(vm.fieldValues["pozisyon"] ?: "") }
    var katkilar by remember { mutableStateOf(vm.fieldValues["objective_goals"] ?: "") }

    FormCard("Kariyer Hedefi") {
        LabeledField("Hedef Pozisyon", pozisyon, { txt -> pozisyon = txt }, "Örn: Android Developer (Kotlin)")
        LabeledField(
            "Katabileceklerin / Güçlü Yanların (Opsiyonel)",
            katkilar,
            { txt -> katkilar = txt },
            "Örn: Performans optimizasyonu, MVVM mimarisi, test kültürü…",
            minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    vm.updateField("pozisyon", pozisyon.trim())
                    vm.updateField("objective_goals", katkilar.trim())
                    vm.generateObjectiveAI(token, pozisyon.trim(), katkilar.trim())
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = appPrimaryButtonColors()
            ) { Text("✨ AI ile Yaz") }
        }
        LabeledField(
            "Hedef Metni",
            vm.fieldValues["objective"] ?: "",
            { txt -> vm.updateField("objective", txt) },
            "2–3 cümlede rol + katacağın değer + teknoloji/etki.",
            minLines = 4,
            charLimit = 600
        )
    }
}



@Composable
private fun ExperienceCard(vm: DocumentViewModel, token: String) {
    val listState = rememberComplex(vm.fieldValues["experience_list"] ?: "")
    val monthOptions = listOf("01","02","03","04","05","06","07","08","09","10","11","12")
    val yearOptions = (2005..2035).map { it.toString() }
    val scope = rememberCoroutineScope()

    FormCard("İş Deneyimleri") {
        listState.value.forEachIndexed { idx, row ->
            var title    by remember(row) { mutableStateOf(row.getOrNull(0) ?: "") }
            var company  by remember(row) { mutableStateOf(row.getOrNull(1) ?: "") }
            var location by remember(row) { mutableStateOf(row.getOrNull(2) ?: "") }
            var startM   by remember(row) { mutableStateOf(row.getOrNull(3) ?: "") }
            var startY   by remember(row) { mutableStateOf(row.getOrNull(4) ?: "") }
            var endM     by remember(row) { mutableStateOf(row.getOrNull(5) ?: "") }
            var endY     by remember(row) { mutableStateOf(row.getOrNull(6) ?: "") }

            val descList = remember(row) {
                mutableStateListOf<String>().apply {
                    val initial = row.getOrNull(7)?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    addAll(initial)
                }
            }

            fun save() {
                val joinedDesc = descList.joinToString("\n")
                val next = listState.value.toMutableList()
                next[idx] = listOf(title, company, location, startM, startY, endM, endY, joinedDesc)
                listState.value = next
                vm.updateField("experience_list", serialize(next))
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    LabeledField("Pozisyon / Unvan", title,   { v -> title = v; save() })
                    LabeledField("Şirket / Kurum", company, { v -> company = v; save() })
                    LabeledField("Konum", location, { v -> location = v; save() }, "İl/İlçe, Ülke")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DropdownField("Başlangıç Ay", monthOptions, startM, { sel -> startM = sel; save() }, Modifier.weight(1f))
                        DropdownField("Başlangıç Yıl",  yearOptions,  startY, { sel -> startY = sel; save() }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DropdownField("Bitiş Ay (Ops.)", listOf("Devam") + monthOptions, endM, { sel -> endM = sel; save() }, Modifier.weight(1f))
                        DropdownField("Bitiş Yıl (Ops.)",  listOf("Devam") + yearOptions,  endY, { sel -> endY = sel; save() }, Modifier.weight(1f))
                    }

                    Text("Açıklama — her satır bir madde", style = MaterialTheme.typography.bodyMedium)

                    descList.forEachIndexed { i, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { txt ->
                                    descList[i] = txt
                                    save()
                                },
                                singleLine = false,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                if (i in 0 until descList.size) {
                                    descList.removeAt(i)
                                    save()
                                }
                            }) { Text("Sil") }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { descList.add(""); save() },
                            shape = RoundedCornerShape(14.dp),
                            colors = appTonalButtonColors()
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Yeni Madde")
                        }
                        TextButton(
                            onClick = {
                                if (title.isBlank() && company.isBlank()) return@TextButton
                                scope.launch {
                                    try {
                                        val ideas = vm.generateExperienceBulletsAI(
                                            token = token,
                                            title = title,
                                            company = company,
                                            location = location,
                                            startYear = startY,
                                            stackHints = vm.fieldValues["skills_list"].orEmpty(),
                                            current = descList.toList()
                                        )
                                        if (ideas.isNotEmpty()) {
                                            descList.clear()
                                            descList.addAll(ideas.map { it.trim() }.filter { it.isNotEmpty() })
                                            save()
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                            colors = appOutlinedButtonColors(),
                            border = AppOutlinedButtonBorder
                        ) { Text("✨ AI öner") }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            val next = listState.value.toMutableList()
                            if (idx in next.indices) next.removeAt(idx)
                            listState.value = next
                            vm.updateField("experience_list", serialize(next))
                        }) { Text("İşi Sil") }
                    }
                }
            }
        }

        AddButton(onClick = {
            val next = listState.value + listOf(listOf("", "", "", "", "", "", "", ""))
            listState.value = next
            vm.updateField("experience_list", serialize(next))
        })
    }
}



@Composable
private fun EducationCard(vm: DocumentViewModel) {
    val edu = rememberComplex(vm.fieldValues["education_list"] ?: "")
    var expandedIdx by remember(edu.value.size) { mutableStateOf(if (edu.value.isEmpty()) null else 0) }

    val monthOptions = listOf("01","02","03","04","05","06","07","08","09","10","11","12")
    val yearOptions = (2005..2035).map { it.toString() }

    FormCard("Eğitim") {
        edu.value.forEachIndexed { idx, row ->
            var courseName by remember(row) { mutableStateOf(row.getOrNull(0) ?: "") }
            var institution by remember(row) { mutableStateOf(row.getOrNull(1) ?: "") }
            var location   by remember(row) { mutableStateOf(row.getOrNull(2) ?: "") }
            var startM     by remember(row) { mutableStateOf(row.getOrNull(3) ?: "") }
            var startY     by remember(row) { mutableStateOf(row.getOrNull(4) ?: "") }
            var endM       by remember(row) { mutableStateOf(row.getOrNull(5) ?: "") }
            var endY       by remember(row) { mutableStateOf(row.getOrNull(6) ?: "") }
            val descList = remember(row) {
                mutableStateListOf<String>().apply {
                    val initial = row.getOrNull(7)?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    addAll(initial)
                }
            }

            fun save() {
                val joined = descList.joinToString("\n")
                edu.value = edu.value.toMutableList().also { list ->
                    list[idx] = listOf(courseName, institution, location, startM, startY, endM, endY, joined)
                }
                vm.updateField("education_list", serialize(edu.value))
            }

            val isExpanded = expandedIdx == idx
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(
                        Modifier.fillMaxWidth().clickable { expandedIdx = if (isExpanded) null else idx },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (courseName.isNotBlank()) "$courseName — $institution" else "Yeni eğitim", fontWeight = FontWeight.SemiBold)
                            if (!isExpanded) {
                                val line = listOf(
                                    listOf(startM, startY).filter { it.isNotBlank() }.joinToString("/"),
                                    listOf(endM, endY).filter { it.isNotBlank() }.joinToString("/").ifBlank { "" }
                                ).filter { it.isNotBlank() }.joinToString(" — ") +
                                        if (location.isNotBlank()) " · $location" else ""
                                if (line.isNotBlank())
                                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }

                    AnimatedVisibility(isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LabeledField("Program/Bölüm", courseName, { courseName = it; save() })
                            LabeledField("Eğitim Kurumu", institution, { institution = it; save() })
                            LabeledField("Konum", location, { location = it; save() }, "İl/İlçe, Ülke")

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                DropdownField("Başlangıç Ay", monthOptions, startM, { sel -> startM = sel; save() }, Modifier.weight(1f))
                                DropdownField("Başlangıç Yıl", yearOptions, startY, { sel -> startY = sel; save() }, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                DropdownField("Bitiş Ay (Ops.)", listOf("Devam") + monthOptions, endM, { sel -> endM = sel; save() }, Modifier.weight(1f))
                                DropdownField("Bitiş Yıl (Ops.)", listOf("Devam") + yearOptions, endY, { sel -> endY = sel; save() }, Modifier.weight(1f))
                            }

                            Text("Açıklama (Ops.) — her satır bir madde", style = MaterialTheme.typography.bodyMedium)
                            descList.forEachIndexed { i, item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = item, onValueChange = { txt -> descList[i] = txt; save() }, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = { if (i in 0 until descList.size) { descList.removeAt(i); save() } }) { Text("Sil") }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { descList.add(""); save() }, shape = RoundedCornerShape(14.dp), colors = appTonalButtonColors()) {
                                    Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Yeni Madde")
                                }
                                TextButton(onClick = {
                                    val seed = listOf(
                                        "Strateji, finans ve operasyon odaklı dersleri tamamladı.",
                                        "Analitik ve liderlik becerilerini takım projelerinde geliştirdi."
                                    )
                                    seed.forEach { s -> if (s !in descList) descList.add(s) }
                                    save()
                                }, colors = appOutlinedButtonColors(), border = AppOutlinedButtonBorder) { Text("Örnek Ekle") }
                            }

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    edu.value = edu.value.toMutableList().also { list -> list.removeAt(idx) }
                                    vm.updateField("education_list", serialize(edu.value))
                                    expandedIdx = null
                                }) { Text("Sil") }
                            }
                        }
                    }
                }
            }
        }

        AddButton(onClick = {
            edu.value = edu.value + listOf(listOf("", "", "", "", "", "", "", ""))
            vm.updateField("education_list", serialize(edu.value))
            expandedIdx = edu.value.lastIndex
        })
    }
}



@Composable
private fun SkillsCard(vm: DocumentViewModel) {
    val raw = vm.fieldValues["skills_list"] ?: ""
    var skills by remember(raw) { mutableStateOf(raw.split("\n").filter { it.isNotBlank() }.toMutableList()) }
    val suggestions = listOf("Kotlin","Jetpack Compose","MVVM","Coroutines","Room","Retrofit","CI/CD","Unit Testing","Git","REST")

    FormCard("Yetenekler") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                AssistChip(
                    onClick = {
                        if (suggestion !in skills) {
                            skills.add(suggestion)
                            vm.updateField("skills_list", skills.joinToString("\n"))
                        }
                    },
                    label = { Text(suggestion) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            skills.forEachIndexed { i, tag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { txt ->
                            skills[i] = txt
                            vm.updateField("skills_list", skills.joinToString("\n"))
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        skills.removeAt(i)
                        vm.updateField("skills_list", skills.joinToString("\n"))
                    }) { Text("Sil") }
                }
            }
            FilledTonalButton(
                onClick = {
                    skills.add("")
                    vm.updateField("skills_list", skills.joinToString("\n"))
                },
                shape = RoundedCornerShape(14.dp),
                colors = appTonalButtonColors()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Yetenek Ekle")
            }
        }
    }
}


@Composable
private fun LanguagesCard(vm: DocumentViewModel) {
    val pairs = rememberPairs(vm.fieldValues["languages_list"] ?: "")
    val levels = listOf("A1","A2","B1","B2","C1","C2","Ana Dil")
    FormCard("Diller (Opsiyonel)") {
        pairs.value.forEachIndexed { idx, (lang, lvl) ->
            var l by remember(lang, lvl) { mutableStateOf(lang) }
            var s by remember(lang, lvl) { mutableStateOf(lvl) }
            fun save() {
                pairs.value = pairs.value.toMutableList().also { list -> list[idx] = l to s }
                vm.updateField("languages_list", pairs.value.joinToString("\n") { "${it.first}||${it.second}" })
            }
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Dil", l, { txt -> l = txt; save() })
                    ExposedDropdownMenuBoxSample("Seviye", levels, s) { sel -> s = sel; save() }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            pairs.value = pairs.value.toMutableList().also { list -> list.removeAt(idx) }
                            vm.updateField("languages_list", pairs.value.joinToString("\n") { "${it.first}||${it.second}" })
                        }) { Text("Sil") }
                    }
                }
            }
        }
        AddButton(onClick = {
            pairs.value = pairs.value + ("" to "")
            vm.updateField("languages_list", pairs.value.joinToString("\n") { "${it.first}||${it.second}" })
        })
    }
}



@Composable
private fun CoursesCard(vm: DocumentViewModel) {
    val courses = rememberComplex(vm.fieldValues["courses_list"] ?: "")
    var expandedIdx by remember(courses.value.size) { mutableStateOf(if (courses.value.isEmpty()) null else 0) }

    FormCard("Kurslar & Sertifikalar (Opsiyonel)") {
        courses.value.forEachIndexed { idx, row ->
            var name     by remember(row) { mutableStateOf(row.getOrNull(0) ?: "") }
            var provider by remember(row) { mutableStateOf(row.getOrNull(1) ?: "") }
            var desc     by remember(row) { mutableStateOf(row.getOrNull(2) ?: "") }
            var year     by remember(row) { mutableStateOf(row.getOrNull(3) ?: "") }

            fun save() {
                courses.value = courses.value.toMutableList().also { list -> list[idx] = listOf(name, provider, desc, year) }
                vm.updateField("courses_list", serialize(courses.value))
            }

            val isExpanded = expandedIdx == idx
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { expandedIdx = if (isExpanded) null else idx }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (name.isNotBlank()) "$name — $provider" else "Yeni kurs/sertifika", fontWeight = FontWeight.SemiBold)
                            if (!isExpanded) Text(year.takeIf { it.isNotBlank() } ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LabeledField("Kurs/Sertifika Adı", name, { name = it; save() })
                            LabeledField("Kurum", provider, { provider = it; save() })
                            LabeledField("Açıklama", desc, { desc = it; save() }, minLines = 3, placeholder = "Örn: SEO, içerik pazarlama, sosyal medya analizi…")
                            LabeledField("Yıl (Ops.)", year, { year = it; save() }, keyboardType = KeyboardType.Number)

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    courses.value = courses.value.toMutableList().also { list -> list.removeAt(idx) }
                                    vm.updateField("courses_list", serialize(courses.value))
                                    expandedIdx = null
                                }) { Text("Sil") }
                            }
                        }
                    }
                }
            }
        }
        AddButton(onClick = {
            courses.value = courses.value + listOf(listOf("", "", "", ""))
            vm.updateField("courses_list", serialize(courses.value))
            expandedIdx = courses.value.lastIndex
        })
    }
}



@Composable
private fun ProjectsCard(vm: DocumentViewModel) {
    val parsed = (vm.fieldValues["projects_list"] ?: "")
        .let { raw ->
            if (raw.isBlank()) emptyList()
            else {
                val rows = if (raw.contains(ROW_SEP)) raw.split(ROW_SEP) else raw.split("\n")
                rows.map { it.trim() }.filter { it.isNotBlank() }.map { row ->
                    val cols = row.split(COL_SEP).map { it.trim() }
                    listOf(cols.getOrNull(0) ?: "", cols.getOrNull(1) ?: "", cols.getOrNull(2) ?: "")
                }
            }
        }
    val projects = remember { mutableStateOf(parsed) }

    fun persist() {
        vm.updateField("projects_list", projects.value.joinToString(ROW_SEP) { it.joinToString(COL_SEP) })
    }

    FormCard("Projeler (Opsiyonel)") {
        LabeledField("GitHub (Opsiyonel)", vm.fieldValues["github"] ?: "", { txt -> vm.updateField("github", txt) }, "https://github.com/kullanici")

        projects.value.forEachIndexed { idx, row ->
            var name by remember(row) { mutableStateOf(row.getOrNull(0) ?: "") }
            var desc by remember(row) { mutableStateOf(row.getOrNull(1) ?: "") }
            var stack by remember(row) { mutableStateOf(row.getOrNull(2) ?: "") }

            fun save() {
                projects.value = projects.value.toMutableList().also { list -> list[idx] = listOf(name, desc, stack) }
                persist()
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Proje Adı", name, { txt -> name = txt; save() })
                    LabeledField("Açıklama", desc, { txt -> desc = txt; save() }, minLines = 3)
                    LabeledField("Teknolojiler (virgülle)", stack, { txt -> stack = txt; save() }, "Kotlin, Compose, Retrofit")
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            projects.value = projects.value.toMutableList().also { list -> list.removeAt(idx) }
                            persist()
                        }) { Text("Sil") }
                    }
                }
            }
        }
        AddButton(onClick = {
            projects.value = projects.value + listOf(listOf("", "", ""))
            persist()
        })
    }
}



@Composable
private fun ReferencesCard(vm: DocumentViewModel) {
    val refs = rememberComplex(vm.fieldValues["references_list"] ?: "")
    var expandedIdx by remember(refs.value.size) { mutableStateOf(if (refs.value.isEmpty()) null else 0) }

    FormCard("Referanslar") {
        refs.value.forEachIndexed { idx, row ->
            var name        by remember(row) { mutableStateOf(row.getOrNull(0) ?: "") }
            var relation    by remember(row) { mutableStateOf(row.getOrNull(1) ?: "") }
            var position    by remember(row) { mutableStateOf(row.getOrNull(2) ?: "") }
            var companyOrLo by remember(row) { mutableStateOf(row.getOrNull(3) ?: "") }
            var email       by remember(row) { mutableStateOf(row.getOrNull(4) ?: "") }
            var phone       by remember(row) { mutableStateOf(row.getOrNull(5) ?: "") }

            fun save() {
                refs.value = refs.value.toMutableList().also { it[idx] = listOf(name, relation, position, companyOrLo, email, phone) }
                vm.updateField("references_list", serialize(refs.value))
            }

            val isExpanded = expandedIdx == idx
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(Modifier.fillMaxWidth().clickable { expandedIdx = if (isExpanded) null else idx }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name.ifBlank { "Yeni referans" }, fontWeight = FontWeight.SemiBold)
                            if (!isExpanded) Text(listOf(relation, companyOrLo).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }

                    AnimatedVisibility(isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LabeledField("Ad Soyad", name, { name = it; save() })
                            LabeledField("Sizinle İlişkisi", relation, { relation = it; save() }, "Örn: Eski Yönetici, Mentor")
                            LabeledField("Mevcut Pozisyon (Ops.)", position, { position = it; save() })
                            LabeledField("Şirket veya Konum (Ops.)", companyOrLo, { companyOrLo = it; save() })
                            LabeledField("E-posta", email, { email = it; save() }, keyboardType = KeyboardType.Email)
                            LabeledField("Telefon", phone, { phone = it; save() })
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    refs.value = refs.value.toMutableList().also { list -> list.removeAt(idx) }
                                    vm.updateField("references_list", serialize(refs.value))
                                    expandedIdx = null
                                }) { Text("Sil") }
                            }
                        }
                    }
                }
            }
        }

        AddButton(onClick = {
            val next = refs.value + listOf(listOf("", "", "", "", "", ""))
            refs.value = next
            vm.updateField("references_list", serialize(next))
            expandedIdx = refs.value.lastIndex
        })
    }
}


@Composable
private fun FormCard(
    title: String,
    subtitle: String? = null,
    locked: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (locked) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, enabled = false, label = { Text("Kilitli") })
                }
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = !locked, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 1,
    charLimit: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var text by remember(value) { mutableStateOf(value) }
    val counter = if (charLimit != null) "${text.length}/$charLimit" else null
    val singleLine = minLines <= 1
    val maxLines = if (singleLine) 1 else Int.MAX_VALUE

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val next = if (charLimit != null) newText.take(charLimit) else newText
            text = next
            onValueChange(next)
        },
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        supportingText = { if (counter != null) Text(counter) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val onSelectUpdated by rememberUpdatedState(onSelect)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    onSelectUpdated(opt)
                    expanded = false
                })
            }
        }
    }
}


private const val ROW_SEP = "<|row|>"
private const val COL_SEP = "||"

@Composable
private fun rememberPairs(raw: String): MutableState<List<Pair<String, String>>> {
    val parsed = raw
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            val p = line.split(COL_SEP)
            (p.getOrNull(0)?.trim() ?: "") to (p.getOrNull(1)?.trim() ?: "")
        }
        .toList()
    return remember(raw) { mutableStateOf(parsed) }
}

@Composable
private fun rememberComplex(raw: String): MutableState<List<List<String>>> {
    val rows = if (raw.contains(ROW_SEP)) raw.split(ROW_SEP) else raw.split("\n")
    val parsed = rows
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { row -> row.split(COL_SEP).map { it.trim() } }
    return remember(raw) { mutableStateOf(parsed) }
}

private fun serialize(rows: List<List<String>>): String =
    rows.joinToString(ROW_SEP) { row -> row.joinToString(COL_SEP) }

private fun normalizeAndSave(vm: DocumentViewModel) {
    fun clean(s: String) = s
        .replace(Regex("[\\p{So}\\p{Cn}]"), "")
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .trim()

    val keys = listOf(
        "first_name", "last_name", "address",
        "isim", "email", "linkedin", "website",
        "pozisyon", "objective", "skills_list", "languages_list",
        "courses_list", "references_list", "education_list", "experience_list",
        "projects_list", "github", "phone_cc", "phone_num"
    )

    keys.forEach { key ->
        vm.fieldValues[key]?.let { vm.updateField(key, clean(it)) }
    }
}


private fun validateSection(section: String, vm: DocumentViewModel): Pair<Boolean, String?> {
    fun nonBlank(v: String?) = !v.isNullOrBlank()
    fun rows(raw: String?) = (raw ?: "")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line -> line.split("||").map { it.trim() } }
        .toList()

    return when (section) {
        "personal" -> {
            val fn  = vm.fieldValues["first_name"]
            val ln  = vm.fieldValues["last_name"]
            val mail= vm.fieldValues["email"]
            val num = vm.fieldValues["phone_num"].orEmpty()
            val phoneOk = num.length == 10 && num.firstOrNull() != '0'
            val missing = mutableListOf<String>()
            if (!nonBlank(fn))   missing += "Ad"
            if (!nonBlank(ln))   missing += "Soyad"
            if (!nonBlank(mail)) missing += "E-posta"
            if (!phoneOk)        missing += "Telefon (10 hane ve 0 ile başlamamalı)"
            if (missing.isNotEmpty()) false to "Eksik/Zayıf alanlar: ${missing.joinToString(", ")}" else true to null
        }

        "objective" -> {
            val obj = vm.fieldValues["objective"]
            if (!nonBlank(obj)) false to "Hedef metni zorunlu." else true to null
        }

        "experience" -> {
            val r = rows(vm.fieldValues["experience_list"])
            if (r.isEmpty()) return false to "En az bir iş deneyimi ekleyin."
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(2).isNullOrBlank() ||
                        row.getOrNull(3).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank()
            }
            if (bad >= 0) false to "Deneyim #${bad + 1}: Pozisyon, Şirket, Konum, Başlangıç (Ay/Yıl) zorunlu." else true to null
        }

        "education" -> {
            val r = rows(vm.fieldValues["education_list"])
            if (r.isEmpty()) return false to "En az bir eğitim ekleyin."
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(2).isNullOrBlank() ||
                        row.getOrNull(3).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank() ||
                        ((row.getOrNull(5).isNullOrBlank()) xor (row.getOrNull(6).isNullOrBlank()))
            }
            if (bad >= 0) false to "Eğitim #${bad + 1}: Program, Kurum, Konum, Başlangıç (Ay/Yıl) zorunlu. Bitiş için Ay/Yıl ikilisini birlikte seçin veya boş bırakın."
            else true to null
        }

        "skills" -> {
            val raw = vm.fieldValues["skills_list"] ?: ""
            val list = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (list.isEmpty()) false to "En az bir yetenek girin." else true to null
        }

        "languages" -> {
            val r = rows(vm.fieldValues["languages_list"])
            if (r.isEmpty()) return true to null
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            if (bad >= 0) false to "Dil #${bad + 1}: Dil ve Seviye zorunlu." else true to null
        }

        "courses" -> {
            val r = rows(vm.fieldValues["courses_list"])
            if (r.isEmpty()) return true to null
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(2).isNullOrBlank()
            }
            if (bad >= 0) false to "Kurs #${bad + 1}: Ad, Kurum ve Açıklama zorunlu." else true to null
        }

        "projects" -> {
            val r = rows(vm.fieldValues["projects_list"])
            if (r.isEmpty()) return true to null
            val bad = r.indexOfFirst { row -> row.getOrNull(0).isNullOrBlank() || row.getOrNull(1).isNullOrBlank() }
            if (bad >= 0) false to "Proje #${bad + 1}: Proje Adı ve Açıklama zorunlu." else true to null
        }

        "references" -> {
            val r = rows(vm.fieldValues["references_list"])
            if (r.isEmpty()) return true to null
            val bad = r.indexOfFirst { row ->
                row.getOrNull(0).isNullOrBlank() ||
                        row.getOrNull(1).isNullOrBlank() ||
                        row.getOrNull(4).isNullOrBlank() ||
                        row.getOrNull(5).isNullOrBlank()
            }
            if (bad >= 0) false to "Referans #${bad + 1}: İsim, İlişki, E-posta ve Telefon zorunlu." else true to null
        }

        else -> true to null
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(14.dp), colors = appTonalButtonColors()) {
        Icon(Icons.Outlined.Add, contentDescription = "Yeni Ekle")
        Spacer(Modifier.width(8.dp))
        Text("Yeni Ekle")
    }
}

private fun calcSaveEnabled(section: String, vm: DocumentViewModel): Boolean =
    validateSection(section, vm).first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxSample(
    label: String,
    options: List<String>,
    value: String,
    onValue: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val onValueState by rememberUpdatedState(onValue)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onValueState(option)
                    expanded = false
                })
            }
        }
    }
}
