package com.example.custombrowser

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var startBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        startBtn = findViewById(R.id.startBtn)

        setupWebView()

        startBtn.setOnClickListener {
            val targetUrl = urlInput.text.toString()
            if (targetUrl.isNotEmpty()) {
                webView.loadUrl(targetUrl)
            }
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                val cookies = CookieManager.getInstance().getCookie(url)
                
                url?.let { currentUrl ->
                    val intent = Intent(this@MainActivity, ExtractorService::class.java).apply {
                        putExtra("TARGET_URL", currentUrl)
                        putExtra("COOKIES", cookies ?: "")
                    }
                    startForegroundService(intent)
                }
            }
        }
    }
}
