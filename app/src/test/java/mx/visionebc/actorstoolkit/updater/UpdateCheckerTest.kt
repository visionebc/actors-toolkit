package mx.visionebc.actorstoolkit.updater

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `UpdateInfo data class holds correct values`() {
        val info = UpdateInfo(
            versionName = "1.0.0",
            versionCode = 5,
            downloadUrl = "https://example.com/app.apk",
            apkSizeBytes = 1024 * 1024 * 25L,
            appName = "Test App"
        )
        assertEquals("1.0.0", info.versionName)
        assertEquals(5, info.versionCode)
        assertEquals("https://example.com/app.apk", info.downloadUrl)
        assertEquals(25 * 1024 * 1024L, info.apkSizeBytes)
        assertEquals("Test App", info.appName)
    }

    @Test
    fun `UpdateInfo equality works`() {
        val info1 = UpdateInfo("1.0", 1, "url", 100, "App")
        val info2 = UpdateInfo("1.0", 1, "url", 100, "App")
        assertEquals(info1, info2)
    }

    @Test
    fun `UpdateInfo inequality on different version`() {
        val info1 = UpdateInfo("1.0", 1, "url", 100, "App")
        val info2 = UpdateInfo("2.0", 2, "url", 100, "App")
        assertNotEquals(info1, info2)
    }
}
