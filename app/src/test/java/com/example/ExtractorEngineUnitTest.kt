package com.example

import com.example.data.extractor.DoodExtractor
import com.example.data.extractor.MixDropExtractor
import com.example.data.extractor.QualityInspector
import com.example.data.extractor.Unpacker
import com.example.data.network.HttpClientHelper
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolutionBadge
import com.example.domain.model.ServerHost
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorEngineUnitTest {

    private val httpHelper = HttpClientHelper(OkHttpClient())
    private val qualityInspector = QualityInspector(httpHelper)
    private val doodExtractor = DoodExtractor(httpHelper, qualityInspector)
    private val mixDropExtractor = MixDropExtractor(httpHelper, qualityInspector)

    @Test
    fun unpacker_decodesPackedMdCoreScript() {
        val packed = """
            eval(function(p,a,c,k,e,d){e=function(c){return c.toString(36)};if(!''.replace(/^/,String)){while(c--){d[c.toString(a)]=k[c]||c.toString(a)}k=[function(e){return d[e]}];e=function(){return'\\w+'};c=1};while(c--){if(k[c]){p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c])}}return p}('0.1="//2.3.4/5/6.7?8=9";0.a="//b.3.4/c/d+e.f";',16,16,'Core|wurl|s-delivery|mixdrop|co|v|ep01_360p|mp4|s|ab12cd|remotesub|sub|ar|ep01|sub|srt'.split('|'),0,{}))
        """.trimIndent()

        val unpacked = Unpacker.unpack(packed)
        assertTrue(unpacked.contains("""Core.wurl="//s-delivery.mixdrop.co/v/ep01_360p.mp4?s=ab12cd""""))
        assertTrue(unpacked.contains("""Core.remotesub="//sub.mixdrop.co/ar/ep01+sub.srt""""))
    }

    @Test
    fun mixDropExtractor_extractsWurlAndDecodesSubtitle() {
        val html = """
            <html><head><title>Anime Ep 05 - 360p</title></head>
            <body><script>
            // MDCore
            eval(function(p,a,c,k,e,d){e=function(c){return c.toString(36)};if(!''.replace(/^/,String)){while(c--){d[c.toString(a)]=k[c]||c.toString(a)}k=[function(e){return d[e]}];e=function(){return'\\w+'};c=1};while(c--){if(k[c]){p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c])}}return p}('0.1="//2.3.4/5/6.7?8=9";0.a="//b.3.4/c/d+e.f";',16,16,'Core|wurl|s-delivery|mixdrop|co|v|ep05|mp4|s|tok123|remotesub|sub|ar|ep05|sub|srt'.split('|'),0,{}))
            </script></body></html>
        """.trimIndent()

        val parsed = mixDropExtractor.parseEmbedHtml(html, "https://mixdrop.co/e/ep05")
        assertNotNull(parsed)
        assertEquals("https://s-delivery.mixdrop.co/v/ep05.mp4?s=tok123", parsed!!.videoUrl)
        assertEquals("https://sub.mixdrop.co/ar/ep05 sub.srt", parsed.subtitleUrl)
    }

    @Test
    fun doodExtractor_extractsPassMd5TokenAndTitleResolution() {
        val html = """
            <html><head><title>Watch Episode 12 - 360p - DoodStream</title></head>
            <body><script>
            ${'$'}.get('/pass_md5/12345-xyz/myToken99', function(data){});
            </script></body></html>
        """.trimIndent()

        val parsed = doodExtractor.parseEmbedHtml(html, "https://ds2play.com/e/test")
        assertNotNull(parsed)
        assertEquals("360p", parsed!!.titleResolution)
        assertEquals("/pass_md5/12345-xyz/myToken99", parsed.md5Path)
        assertEquals("myToken99", parsed.token)
        assertEquals(10, doodExtractor.createHashTable(10).length)
    }

    @Test
    fun qualityInspector_parsesM3u8AndPrioritizes360pWithSmartFallback() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1920x1080
            1080p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=700000,RESOLUTION=640x360
            360p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480
            480p/index.m3u8
        """.trimIndent()

        val variants = qualityInspector.parseM3u8Content(
            playlistUrl = "https://cdn.example.com/master.m3u8",
            playlistContent = m3u8,
            sourcePageUrl = "https://cdn.example.com/master.m3u8",
            host = ServerHost.DIRECT_HLS,
            title = "Test Stream",
            headers = emptyMap()
        )

        assertEquals(3, variants.size)
        assertEquals(ResolutionBadge.P360, variants.first().quality.resolution)
        assertTrue(variants.first().is360pPreferred)
        assertEquals("https://cdn.example.com/360p/index.m3u8", variants.first().directUrl)

        val picked = qualityInspector.selectBestByPolicy(variants, QualityPreferencePolicy.TARGET_360P_SMART_FALLBACK)
        assertEquals(ResolutionBadge.P360, picked?.quality?.resolution)

        val without360 = variants.filterNot { it.quality.resolution == ResolutionBadge.P360 }
        val fallbackPicked = qualityInspector.selectBestByPolicy(without360, QualityPreferencePolicy.TARGET_360P_SMART_FALLBACK)
        assertEquals(ResolutionBadge.P480, fallbackPicked?.quality?.resolution)

        val strictNull = qualityInspector.selectBestByPolicy(without360, QualityPreferencePolicy.STRICT_360P_ONLY)
        assertNull(strictNull)
    }
}
