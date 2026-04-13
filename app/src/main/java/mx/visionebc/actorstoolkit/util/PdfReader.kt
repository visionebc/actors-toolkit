package mx.visionebc.actorstoolkit.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfReader {

    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context)
            initialized = true
        }
    }

    fun extractText(context: Context, uri: Uri): String {
        init(context)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file")

        return inputStream.use { stream ->
            PDDocument.load(stream).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    }
}
