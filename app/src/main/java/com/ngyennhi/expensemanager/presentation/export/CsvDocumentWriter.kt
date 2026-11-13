package com.ngyennhi.expensemanager.presentation.export

import java.io.IOException
import java.io.OutputStream

data class CsvWriteResult(
    val success: Boolean,
    val partialFileMayRemain: Boolean = false,
)

fun writeCsvDocument(
    bytes: ByteArray,
    openDestination: () -> OutputStream?,
    cleanupDestination: () -> Boolean,
): CsvWriteResult = try {
    val output = openDestination() ?: throw IOException("Document provider returned no output stream")
    output.use {
        it.write(bytes)
        it.flush()
    }
    CsvWriteResult(success = true)
} catch (_: Exception) {
    val cleaned = try {
        cleanupDestination()
    } catch (_: Exception) {
        false
    }
    CsvWriteResult(success = false, partialFileMayRemain = !cleaned)
}
