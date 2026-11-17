package com.ngyennhi.expensemanager.presentation.export

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvDocumentWriterTest {
    @Test fun completeBytesAreWrittenWithoutCleanup() {
        val output = ByteArrayOutputStream()
        var cleanups = 0

        val result = writeCsvDocument(byteArrayOf(1, 2, 3), { output }, { cleanups++; true })

        assertTrue(result.success)
        assertFalse(result.partialFileMayRemain)
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertTrue(cleanups == 0)
    }

    @Test fun failedWriteReportsSafeFailureWhenCleanupSucceeds() {
        val result = writeCsvDocument(byteArrayOf(1, 2, 3), { FailingOutputStream() }, { true })

        assertFalse(result.success)
        assertFalse(result.partialFileMayRemain)
    }

    @Test fun failedWriteWarnsWhenProviderCannotRemoveOrTruncatePartialFile() {
        val result = writeCsvDocument(byteArrayOf(1), { FailingOutputStream() }, { false })

        assertFalse(result.success)
        assertTrue(result.partialFileMayRemain)
    }

    @Test fun missingOutputStreamAlsoRunsCleanup() {
        var cleaned = false
        val result = writeCsvDocument(byteArrayOf(1), { null }, { cleaned = true; true })

        assertTrue(cleaned)
        assertFalse(result.success)
        assertFalse(result.partialFileMayRemain)
    }

    private class FailingOutputStream : OutputStream() {
        override fun write(b: Int) = throw IOException("provider failed")
        override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("provider failed")
    }
}
