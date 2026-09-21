package com.notivault.app.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class DataExporterTest {

    @Test
    fun testCsvEscapingQuotesAndCommas() {
        // Test escapeCsv private method using reflection
        val method: Method = DataExporter::class.java.getDeclaredMethod("escapeCsv", String::class.java)
        method.isAccessible = true

        val simpleText = "Hello, world!"
        val escaped1 = method.invoke(DataExporter, simpleText) as String
        assertEquals("\"Hello, world!\"", escaped1)

        val quotedText = "He said \"hello\"."
        val escaped2 = method.invoke(DataExporter, quotedText) as String
        assertEquals("\"He said \"\"hello\"\".\"", escaped2)

        val newlineText = "Line 1\nLine 2\rLine 3"
        val escaped3 = method.invoke(DataExporter, newlineText) as String
        assertEquals("\"Line 1 Line 2 Line 3\"", escaped3)

        val crlfText = "Line 1\r\nLine 2"
        val escaped4 = method.invoke(DataExporter, crlfText) as String
        assertEquals("\"Line 1 Line 2\"", escaped4)
    }
}
