@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.myapplication.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapplication.viewmodel.DocumentViewModel
import com.example.myapplication.utils.downloadAndOpenDocument
import com.example.myapplication.utils.formatDate
import com.example.myapplication.utils.deleteDocumentById
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos


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

@Preview
@Composable
fun AppPrimaryButtonColorsPreview() {
    Button(
        onClick = {},
        colors = appPrimaryButtonColors()
    ) {
        Text("Primary Button")
    }
}

@Composable
private fun appOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = AppLinkColor)

@Preview
@Composable
fun AppOutlinedButtonColorsPreview() {
    OutlinedButton(onClick = {}, colors = appOutlinedButtonColors()) { Text("Outlined Button") }
}

private val AppOutlinedButtonBorder = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(listOf(AppLinkColor, AppAccentLight))
)


@Composable
fun HomeScreen(
    navController: NavController,
    token: String,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    LaunchedEffect(token) { viewModel.fetchDocuments(token) }
    val uiState by viewModel.uiState.collectAsState()

    var showConfirmDelete by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteTitle by remember { mutableStateOf("") }

    val gradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Silinsin mi?") },
            text = { Text("“$pendingDeleteTitle” adlı CV kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingDeleteId ?: return@TextButton
                        showConfirmDelete = false
                        deleteDocumentById(
                            token = token,
                            documentId = id,
                            context = ctx
                        ) { viewModel.fetchDocuments(token) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppLinkColor)
                ) { Text("Vazgeç") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Belgelerim", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.fetchDocuments(token) }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Yenile",
                            tint = AppLinkColor
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("generate/cv") },
                containerColor = AppPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Yeni CV")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            HeaderHome()

            when {
                uiState.isLoading -> LoadingList()
                uiState.error != null -> ErrorBox(
                    message = uiState.error ?: "Bir hata oluştu",
                    onRetry = { viewModel.fetchDocuments(token) }
                )
                uiState.documents.isEmpty() -> EmptyBox(
                    onCreate = { navController.navigate("generate/cv") }
                )
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.documents) { doc ->
                            DocumentCard(
                                title = doc.title,
                                createdAt = formatDate(doc.createdAt),
                                onOpen = {
                                    downloadAndOpenDocument(
                                        token = token,
                                        documentId = doc.id, fileType = doc.fileType,
                                        context = ctx
                                    )
                                },
                                onLongPress = {
                                    pendingDeleteId = doc.id
                                    pendingDeleteTitle = doc.title
                                    showConfirmDelete = true
                                }
                            )
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        navController = NavController(LocalContext.current),
        token = "dummy_token"
    )
}



@Composable
private fun HeaderHome() {
    Spacer(Modifier.height(8.dp))
}

@Preview
@Composable
fun HeaderHomePreview() {
    HeaderHome()
}


@Composable
private fun DocumentCard(
    title: String,
    createdAt: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val rippleIndication = ripple(
        color = AppLinkColor.copy(alpha = 0.2f),
        bounded = true
    )

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress,
                interactionSource = interaction,
                indication = rippleIndication
            )
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
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppPrimary.copy(alpha = 0.12f),
                                AppAccentLight.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = AppPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = AppLinkColor
            )
        }
    }
}



@Preview
@Composable
fun DocumentCardPreview() {
    DocumentCard(
        title = "Sample CV",
        createdAt = "2023-10-27",
        onOpen = {},
        onLongPress = {}
    )
}



@Composable
private fun LoadingList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {}
        }
    }
}

@Preview
@Composable
fun LoadingListPreview() {
    LoadingList()
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("❌ $message", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetry,
                colors = appOutlinedButtonColors(),
                border = AppOutlinedButtonBorder,
                shape = RoundedCornerShape(14.dp)
            ) { Text("Tekrar Dene") }
        }
    }
}

@Preview
@Composable
fun ErrorBoxPreview() {
    ErrorBox(message = "Failed to load documents.", onRetry = {})
}

@Composable
private fun EmptyBox(onCreate: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Henüz yüklenmiş bir CV bulunamadı.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(14.dp),
                colors = appPrimaryButtonColors()
            ) { Text("➕ İlk CV’ni Oluştur") }
        }
    }
}

@Preview
@Composable
fun EmptyBoxPreview() {
    EmptyBox(onCreate = {})
}
