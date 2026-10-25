package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ultra Video Player", appName)
  }

  @Test
  fun `videoItem formattedDuration works as expected`() {
    val video = VideoItem(
        id = 1L,
        title = "Test",
        displayName = "test.mp4",
        contentUriString = "content://media/1",
        duration = 3665000L, // 1h 1m 5s
        size = 104857600L, // 100 MB
        width = 1920,
        height = 1080,
        dateAdded = 1000L,
        folderName = "Movies",
        path = "/storage/Movies/test.mp4",
        mimeType = "video/mp4"
    )

    assertEquals("1:01:05", video.formattedDuration)
    assertEquals("100.0 MB", video.formattedSize)
    assertEquals("1080p", video.resolutionLabel)
  }

  @Test
  fun `privateVaultManager pin configuration and verification works`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vaultManager = com.example.data.repository.PrivateVaultManager(context)

    // Set PIN
    val setSuccess = vaultManager.setPin("2580")
    org.junit.Assert.assertTrue(setSuccess)
    org.junit.Assert.assertTrue(vaultManager.isPinConfigured())

    // Verify correct PIN
    org.junit.Assert.assertTrue(vaultManager.verifyPin("2580"))

    // Verify incorrect PIN
    org.junit.Assert.assertFalse(vaultManager.verifyPin("0000"))
    org.junit.Assert.assertFalse(vaultManager.verifyPin("1234"))
  }

  @Test
  fun `privateVaultManager locking and permanently deleting video works`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vaultManager = com.example.data.repository.PrivateVaultManager(context)

    val sampleVideo = VideoItem(
        id = 999L,
        title = "Private Clip",
        displayName = "secret_clip.mp4",
        contentUriString = "file:///dummy/secret.mp4",
        duration = 15000L,
        size = 2048L,
        width = 1280,
        height = 720,
        dateAdded = System.currentTimeMillis() / 1000,
        folderName = "Camera",
        path = "",
        mimeType = "video/mp4"
    )

    val lockResult = vaultManager.lockVideo(sampleVideo)
    org.junit.Assert.assertTrue(lockResult.isSuccess)

    val vaultList = vaultManager.getVaultVideos()
    org.junit.Assert.assertTrue(vaultList.any { it.title == "Private Clip" })

    val lockedItem = vaultList.first { it.title == "Private Clip" }
    val deleted = vaultManager.deletePermanently(lockedItem)
    org.junit.Assert.assertTrue(deleted)
  }
}

