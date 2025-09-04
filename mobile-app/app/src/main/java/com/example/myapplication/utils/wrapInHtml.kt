package com.example.myapplication.utils

import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale


private fun normalizeForATS(raw: String): String {
    var s = raw
        .replace(Regex("[•·●◦▪■□◆◇▶►➤✔︎✓✦❖❏❑]"), "-")
        .replace('—', '-')
        .replace('–', '-')
        .replace('“', '"').replace('”', '"')
        .replace('‘', '\'').replace('’', '\'')
        .replace(Regex("\\t+"), " ")
        .replace("\r\n", "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .replace('\u00A0', ' ')

    s = Normalizer.normalize(s, Normalizer.Form.NFC)

    s = s.lines().joinToString("\n") { line ->
        line.trimEnd().replace(Regex(" {2,}"), " ")
    }

    return s.trim()
}

private fun sanitizeForAts(raw: String): String {
    var s = normalizeForATS(raw)

    s = s.replace(Regex("[│┌┬┐└┴┘╔╦╗╚╩╝║]"), "-")

    s = s.lines().joinToString("\n") { line ->
        var ln = line
        ln = ln.replace(Regex("^\\s*[*+]+\\s+"), "- ")
            .replace(Regex("^\\s*[•·●◦▪➤✔\uFE0E✓]+\\s+"), "- ")
            .replace(Regex("^\\s*–\\s+"), "- ")
            .replace(Regex("^\\s*—\\s+"), "- ")
        ln = ln.replace(Regex(" {4,}"), " ")
        ln
    }

    s = s.replace("|", " · ")
        .replace(Regex("(\\s*·\\s*){2,}"), " · ")

    return s.trim()
}


fun saveAsPdfAts(contentRaw: String, file: File) {
    val content = sanitizeForAts(contentRaw)

    val document = PdfDocument()

    val pageWidth = 595
    val pageHeight = 842

    val margin = 42f
    val bodySize = 12.5f
    val headerSize = 15.0f
    val lineSpacing = 18.0f
    val bulletIndent = 14f
    val usableWidth = pageWidth - 2 * margin

    val headerPaint = Paint().apply {
        textSize = headerSize
        isAntiAlias = true
        isFakeBoldText = true
        isSubpixelText = true
        color = Color.BLACK
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        @Suppress("DEPRECATION")
        (Locale("tr", "TR").also { this.textLocale = it })
    }
    val textPaint = Paint().apply {
        textSize = bodySize
        isAntiAlias = true
        isSubpixelText = true
        color = Color.BLACK
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        @Suppress("DEPRECATION")
        (Locale("tr", "TR").also { this.textLocale = it })
    }

    var pageNumber = 1
    fun newPage(): Triple<PdfDocument.Page, Canvas, Float> {
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        val page = document.startPage(info)
        return Triple(page, page.canvas, margin)
    }

    fun hasSpace(currentY: Float, needed: Float = lineSpacing): Boolean =
        currentY + needed <= pageHeight - margin

    fun drawWrappedLine(
        canvas: Canvas,
        paint: Paint,
        text: String,
        startX: Float,
        startY: Float,
        width: Float
    ): Float {
        if (text.isBlank()) return startY + lineSpacing

        val words = text.split(" ")
        var y = startY
        var current = ""
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= width) {
                current = candidate
            } else {
                if (!hasSpace(y)) return y
                canvas.drawText(current, startX, y, paint)
                y += lineSpacing
                current = w
            }
        }
        if (current.isNotEmpty()) {
            if (!hasSpace(y)) return y
            canvas.drawText(current, startX, y, paint)
            y += lineSpacing
        }
        return y
    }

    fun isSectionHeader(line: String): Boolean {
        val l = line.trim()
        if (l.isEmpty()) return false
        val known = setOf(
            "ÖZET", "TEKNİK YETKİNLİKLER", "YETKİNLİKLER",
            "DENEYİM", "İŞ DENEYİMİ", "PROJELER",
            "EĞİTİM", "DİLLER", "SERTİFİKALAR", "SERTİFİKALAR/KURSLAR",
            "KURSLAR", "REFERANSLAR", "İLETİŞİM", "AD SOYAD"
        )
        return (l == l.uppercase() && l.length in 3..56) || known.contains(l.uppercase())
    }

    var (page, canvas, yStart) = newPage()
    var y = yStart
    var lineIndex = 0
    val lines = content.lines()

    if (lines.isNotEmpty() && lines[0].isNotBlank()) {
        if (!hasSpace(y)) {
            document.finishPage(page)
            val t = newPage()
            page = t.first; canvas = t.second; y = t.third
        }
        y = drawWrappedLine(canvas, headerPaint, lines[0].trim(), margin, y, usableWidth)
        lineIndex = 1
        y += 4f
    }

    if (lineIndex < lines.size && lines[lineIndex].isNotBlank()) {
        if (!hasSpace(y)) {
            document.finishPage(page)
            val t = newPage()
            page = t.first; canvas = t.second; y = t.third
        }
        y = drawWrappedLine(canvas, textPaint, lines[lineIndex].trim(), margin, y, usableWidth)
        lineIndex++
        y += 6f
    }

    while (lineIndex < lines.size) {
        val line = lines[lineIndex].trimEnd()
        lineIndex++

        if (!hasSpace(y)) {
            document.finishPage(page)
            val t = newPage()
            page = t.first; canvas = t.second; y = t.third
        }

        if (line.isBlank()) {
            y += lineSpacing
            continue
        }

        if (isSectionHeader(line)) {
            canvas.drawText(line, margin, y, headerPaint)
            y += lineSpacing * 0.85f
            continue
        }

        val isBullet = line.startsWith("- ")
        if (isBullet) {
            val text = line.removePrefix("- ").trimStart()
            y = drawWrappedLine(canvas, textPaint, "- $text", margin + bulletIndent, y, usableWidth - bulletIndent)
        } else {
            y = drawWrappedLine(canvas, textPaint, line, margin, y, usableWidth)
        }
    }

    document.finishPage(page)
    document.writeTo(FileOutputStream(file))
    document.close()
}
