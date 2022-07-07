package ray.mintcat.shengyiwu

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.selectMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "ray.mintcat.shengyiwu",
        name = "插件示例",
        version = "0.1.0"
    ) {
        author("1523165110")
        info(
            """
            这是一个测试插件, 
            在这里描述插件的功能和用法等.
        """.trimIndent()
        )
        // author 和 info 可以删除.
    }
) {
    val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
            if (message.contentEquals("查圣遗物")) {
                subject.sendMessage("请发送圣遗物图片[背包内截图]")
                val image: ByteArray = selectMessages {
                    has<Image> {
                        withContext(Dispatchers.IO) {
                            URL(it.queryUrl()).readBytes()
                        }
                    }
                    has<PlainText> {
                        withContext(Dispatchers.IO) {
                            URL(it.content).readBytes()
                        }
                    }
                    defaultReply { "请发送图片或图片链接" }
                    timeout(30_000) { subject.sendMessage("请发送你要进行 OCR 的图片或图片链接"); null }
                } ?: return@subscribeAlways
                val json = buildJsonObject {
                    put("image", Base64.getEncoder().encodeToString(image))
                }.toString().toRequestBody()
                val data = httpPost("https://api.genshin.pub/api/v1/app/ocr", json) ?: return@subscribeAlways
                val toRoot = PluginMain.json.decodeFromString(Datas.serializer(), data).apply {
                    hitokoto = "数据来源可莉特调 枫糖Bot整理"
                }
                val png = httpPostPNG(
                    "https://api.genshin.pub/api/v1/relic/pic",
                    PluginMain.json.encodeToString(
                        Root.serializer(), Root(
                            toRoot.hitokoto, toRoot.level,
                            PMainItem(toRoot.main_item.name, toRoot.main_item.type, toRoot.main_item.value),
                            toRoot.name, toRoot.star, mutableListOf<PSubItem>().apply {
                                toRoot.sub_item.forEach {
                                    add(PSubItem(it.name, it.type, it.value))
                                }
                            }
                        )
                    ).toRequestBody()
                ) ?: return@subscribeAlways
                subject.sendImage(png.toExternalResource())

            }
        }
    }

    fun httpPost(url: String, body: RequestBody): String? {
        return try {
            val client = OkHttpClient().newBuilder().apply {
                sslSocketFactory(getSocketFactory(getX509TrustManager())!!, getX509TrustManager())
                hostnameVerifier(getHostnameVerifier())
            }.build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                )
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Connection", "close")
                .build()
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            "请求异常: ${e.message} "
        }
    }

    fun httpPostPNG(url: String, body: RequestBody): InputStream? {
        return try {
            val client = OkHttpClient().newBuilder().apply {
                sslSocketFactory(getSocketFactory(getX509TrustManager())!!, getX509TrustManager())
                hostnameVerifier(getHostnameVerifier())
            }.build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.66 Safari/537.36 Edg/103.0.1264.44"
                )
                .addHeader("Access-Control-Request-Headers", "content-type")
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Connection", "close")
                .build()
            val response = client.newCall(request).execute()
            response.body?.byteStream()
        } catch (e: Exception) {
            "请求异常: ${e.message} "
            null
        }
    }
    //Content-Type


    fun getSocketFactory(manager: TrustManager): SSLSocketFactory? {
        var socketFactory: SSLSocketFactory? = null
        try {
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf<TrustManager>(manager), SecureRandom())
            socketFactory = sslContext.getSocketFactory()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
        return socketFactory
    }

    fun getX509TrustManager(): X509TrustManager {
        return object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }
        }
    }

    fun getHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { s, sslSession -> true }
    }


}
