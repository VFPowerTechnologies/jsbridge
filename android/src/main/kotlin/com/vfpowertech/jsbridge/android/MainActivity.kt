package com.vfpowertech.jsbridge.android

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vfpowertech.jsbridge.core.dispatcher.Dispatcher
import com.vfpowertech.jsbridge.core.services.js.testing.TestRunner
import org.slf4j.LoggerFactory

class MainActivity : Activity() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webview = findViewById(R.id.webview) as WebView
        webview.settings.javaScriptEnabled = true

        webview.setWebViewClient(object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                log.info("Page load starting")
            }

            override fun onPageFinished(view: WebView, url: String) {
                log.info("Page load complete")
                webview.evaluateJavascript("document", {
                    log.info("value: $it")
                })
            }
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                log.error("$failingUrl failed: $description ($errorCode)")
            }
        })

        val jsLog = LoggerFactory.getLogger("Javascript")
        webview.setWebChromeClient(object: WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "[${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}] ${consoleMessage.message()}"
                when (consoleMessage.messageLevel()) {
                    ConsoleMessage.MessageLevel.DEBUG -> jsLog.debug(msg)
                    ConsoleMessage.MessageLevel.ERROR -> jsLog.error(msg)
                    ConsoleMessage.MessageLevel.LOG -> jsLog.info(msg)
                    ConsoleMessage.MessageLevel.TIP -> jsLog.info(msg)
                    ConsoleMessage.MessageLevel.WARNING -> jsLog.warn(msg)
                }
                return true;
            }
        })

        val engineInterface = AndroidWebEngineInterface(webview)
        val dispatcher = Dispatcher(engineInterface)
        val testService = com.vfpowertech.jsbridge.core.services.TestService()
        dispatcher.registerService("TestService", com.vfpowertech.jsbridge.core.services.jstojava.TestServiceToJavaProxy(testService, dispatcher))

        val jsTestService = com.vfpowertech.jsbridge.core.services.js.javatojs.JSTestServiceToJSProxy(dispatcher)
        findViewById(R.id.runTestsBtn).setOnClickListener {
            val runner = TestRunner(engineInterface, jsTestService)
            Thread(runner).start()
        }

        webview.loadUrl("file:///android_asset/index.html")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
