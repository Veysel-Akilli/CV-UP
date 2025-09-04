package com.example.myapplication.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.DocumentRepository
import com.example.myapplication.data.local.CvDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.*


data class HomeUiState(
    val isLoading: Boolean = false,
    val documents: List<Document> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val draftStore: CvDraftStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    var generatedContent by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var lastSavedAt by mutableStateOf<Long?>(null)
        private set


    val fieldValues = mutableStateMapOf<String, String>()

    fun updateField(key: String, value: String) {
        fieldValues[key] = value
    }

    init {
        viewModelScope.launch {
            try {
                val (map, ts) = draftStore.load()
                if (map.isNotEmpty()) {
                    fieldValues.clear()
                    fieldValues.putAll(map)
                    lastSavedAt = ts
                }
            } catch (_: Exception) {  }
        }
    }


    fun saveDraftLocal() {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = ""
                draftStore.save(fieldValues.toMap())
                lastSavedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Lokal kaydetme hatası"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearDraftLocal() {
        viewModelScope.launch {
            try { draftStore.clear() } catch (_: Exception) { }
            lastSavedAt = null
        }
    }

    fun generateDocument(token: String) {
        viewModelScope.launch {
            isLoading = true; errorMessage = ""
            try {
                val input = fieldValues.toMap() + mapOf(
                    "template" to "ats_v1",
                    "rules" to listOf(
                        "Başlıklar sabit", "YYYY-AA tarih", "tek sütun",
                        "madde işareti '-'", "tablo/ikon yok", "450-600 kelime"
                    )
                )
                val request = DocumentGenerateRequest("cv", input)
                val response = repository.generateDocument(token, request)
                generatedContent = response.generated_content
                clearDraftLocal()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Bir hata oluştu"
            } finally { isLoading = false }
        }
    }



    fun uploadDocument(token: String, file: File, title: String, description: String) {
        viewModelScope.launch {
            try { repository.uploadDocument(token, file, title, description) }
            catch (e: Exception) { errorMessage = "Yükleme hatası: ${e.localizedMessage}" }
        }
    }

    fun fetchDocuments(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val docs = repository.getUserDocuments(token)
                _uiState.update { it.copy(isLoading = false, documents = docs) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Bilinmeyen hata") }
            }
        }
    }

    fun generateObjectiveAI(token: String, profession: String, goals: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val input = mapOf(
                    "profession" to profession,
                    "goals" to goals,
                    "instruction" to "CV için kısa (2-3 cümle), ATS uyumlu, birinci tekil kullanmadan net bir Objective yaz."
                )
                val req = DocumentGenerateRequest(document_type = "objective", input_data = input)
                val res = repository.generateDocument(token, req)
                val text = res.generated_content.ifBlank { "Objective üretilemedi." }
                fieldValues["objective"] = text
                generatedContent = ""
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Objective üretimi başarısız."
            } finally {
                isLoading = false
            }
        }
    }


    suspend fun generateExperienceBulletsAI(
        token: String,
        title: String,
        company: String,
        location: String,
        startYear: String,
        stackHints: String,
        current: List<String>
    ): List<String> = withContext(Dispatchers.IO) {
        val cleanedCurrent = current.map { it.trim() }.filter { it.isNotEmpty() }

        try {
            val req = ExperienceBulletsRequest(
                title = title,
                company = company,
                location = location,
                startYear = startYear.ifBlank { null },
                stackHints = stackHints.ifBlank { null },
                current = cleanedCurrent
            )

            val apiResp: ExperienceBulletsResponse = repository.generateExperienceBullets(token, req)

            val fromApi = when {
                apiResp.bullets != null -> apiResp.bullets
                !apiResp.text.isNullOrBlank() ->
                    apiResp.text.split("\n")
                        .map { it.trim().removePrefix("-").trim() }
                        .filter { it.isNotBlank() }
                else -> emptyList()
            }

            if (fromApi.isNotEmpty()) return@withContext fromApi
        } catch (_: Exception) {
        }

        val role = title.ifBlank { "Role" }
        val comp = company.ifBlank { "Company" }
        val stk  = stackHints
            .split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(5)
            .joinToString(", ")

        val baseline = listOf(
            "Led end-to-end ${role.lowercase()} initiatives at $comp; delivered on time and within scope.",
            "Implemented clean architecture (MVVM) and asynchronous flows with Coroutines; improved responsiveness.",
            "Integrated REST services with Retrofit; reduced network-related crashes and timeouts.",
            "Optimized Room queries and caching; improved load time and reduced I/O footprint.",
            "Collaborated with cross-functional teams; translated requirements into shippable increments."
        )

        val techPlus = if (stk.isNotBlank())
            listOf("Utilized $stk in daily development and code reviews.") else emptyList()

        val uniq = (baseline + techPlus)
            .map { it.trim().removePrefix("-").trim() }
            .distinct()

        val merged = (cleanedCurrent + uniq).distinct().take(7)
        merged.ifEmpty { listOf("Drove measurable impact through ownership of key deliverables.") }
    }



    fun clearGenerated() {
        generatedContent = ""
        errorMessage = ""
    }
}
