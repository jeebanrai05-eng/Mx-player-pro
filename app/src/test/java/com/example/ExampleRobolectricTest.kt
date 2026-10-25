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
}
