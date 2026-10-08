package com.example

import com.example.data.model.SearchEngine
import com.example.web.WebUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testWebUtils_resolveInput() {
        // Direct URLs
        assertEquals("https://example.com", WebUtils.resolveInput("https://example.com", SearchEngine.DUCKDUCKGO))
        assertEquals("http://test.org", WebUtils.resolveInput("http://test.org", SearchEngine.DUCKDUCKGO))
        assertEquals("nova://newtab", WebUtils.resolveInput("nova://newtab", SearchEngine.DUCKDUCKGO))

        // Domains
        assertEquals("https://wikipedia.org", WebUtils.resolveInput("wikipedia.org", SearchEngine.DUCKDUCKGO))
        assertEquals("https://news.ycombinator.com", WebUtils.resolveInput("news.ycombinator.com", SearchEngine.DUCKDUCKGO))

        // Search queries
        val search = WebUtils.resolveInput("best programming tutorials", SearchEngine.DUCKDUCKGO)
        assertTrue(search.contains("duckduckgo.com"))
        assertTrue(search.contains("best+programming+tutorials") || search.contains("best%20programming%20tutorials"))
    }

    @Test
    fun testWebUtils_isSecure() {
        assertTrue(WebUtils.isSecure("https://google.com"))
        assertTrue(!WebUtils.isSecure("http://insecure.org"))
        assertTrue(!WebUtils.isSecure("nova://newtab"))
    }
}
