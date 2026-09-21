package com.notivault.app.service.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MediaMagicDetectorTest {

    @Test
    fun testDetectJpegMagicBytes() {
        val tempFile = File.createTempFile("test_sample", ".dat").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46))
            deleteOnExit()
        }

        val result = MediaMagicDetector.detect(tempFile)
        assertNotNull(result)
        assertEquals("image/jpeg", result?.mimeType)
        assertEquals("IMAGE", result?.mediaType)
    }

    @Test
    fun testDetectPngMagicBytes() {
        val tempFile = File.createTempFile("test_sample", ".dat").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            deleteOnExit()
        }

        val result = MediaMagicDetector.detect(tempFile)
        assertNotNull(result)
        assertEquals("image/png", result?.mimeType)
        assertEquals("IMAGE", result?.mediaType)
    }

    @Test
    fun testDetectMp4MagicBytes() {
        val tempFile = File.createTempFile("test_sample", ".dat").apply {
            // standard MP4 ftyp header at offset 4
            writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D))
            deleteOnExit()
        }

        val result = MediaMagicDetector.detect(tempFile)
        assertNotNull(result)
        assertEquals("video/mp4", result?.mimeType)
        assertEquals("VIDEO", result?.mediaType)
    }

    @Test
    fun testDetectWebpMagicBytes() {
        val tempFile = File.createTempFile("test_sample", ".dat").apply {
            // RIFF....WEBP
            writeBytes(byteArrayOf(
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
            ))
            deleteOnExit()
        }

        val result = MediaMagicDetector.detect(tempFile)
        assertNotNull(result)
        assertEquals("image/webp", result?.mimeType)
        assertEquals("IMAGE", result?.mediaType)
    }

    @Test
    fun testRejectArbitraryData() {
        val tempFile = File.createTempFile("test_sample", ".dat").apply {
            writeBytes("This is just plain text content".toByteArray())
            deleteOnExit()
        }

        val result = MediaMagicDetector.detect(tempFile)
        assertNull(result)
    }

    @Test
    fun testRootAvailableCheckDoesNotCrash() {
        // Must safely return a boolean (typically false on linux test runner without su) without hanging
        val isRoot = RootViewOnceManager.isRootAvailable(forceCheck = true)
        // Check caching works
        val cached = RootViewOnceManager.isRootAvailable(forceCheck = false)
        assertEquals(isRoot, cached)
    }
}
