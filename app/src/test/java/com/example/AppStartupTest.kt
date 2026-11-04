package com.example

import com.example.data.api.ApiClient
import com.example.data.api.VideoLinks
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppStartupTest {

    @Test
    fun testApiClientInitialization() {
        assertNotNull(ApiClient.apiService)
    }

    @Test
    fun testVideoLinks() {
        VideoLinks.videoId("https://app.cashpay-all.com/onchat/videos/123")
    }
}
