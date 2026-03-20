package com.example.help_stu_agent.ui.treeStructure

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@Composable
fun MathJaxWebView(content: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val textColor = scheme.onSurface.toCssHex()
    val linkColor = scheme.primary.toCssHex()
    val mutedColor = scheme.onSurfaceVariant.toCssHex()

    val htmlData = remember(content, textColor, linkColor, mutedColor) {
        """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <script>
                window.MathJax = {
                    tex: {
                        inlineMath: [['$', '$'], ['\\(', '\\)']],
                        displayMath: [['$$', '$$'], ['\\[', '\\]']],
                        processEscapes: true
                    },
                    options: {
                        renderActions: {
                            addMenu: []
                        }
                    },
                    chtml: {
                        displayAlign: 'left'
                    }
                };
            </script>
            <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
            <style>
                body { font-family: sans-serif; font-size: 16px; line-height: 1.6; color: $textColor; background-color: transparent; margin: 0; padding: 8px; }
                a { color: $linkColor; }
                .muted { color: $mutedColor; }
                mjx-container[display="true"] { margin: 1em 0 !important; }
            </style>
        </head>
        <body>
            <div id="content">${content.replace("\n", "<br>")}</div>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://cdn.jsdelivr.net/",
                htmlData,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun MarkdownMathJaxWebView(
    markdown: String,
    modifier: Modifier = Modifier,
    textColorCss: String,
    linkColorCss: String,
    codeBgCss: String,
    bubbleBgCss: String
) {
    val density = LocalDensity.current
    var heightDp by remember { mutableStateOf(60.dp) } // 调高初始高度

    val parser = remember {
        Parser.builder()
            .extensions(listOf(TablesExtension.create(), TaskListItemsExtension.create()))
            .build()
    }
    val renderer = remember {
        HtmlRenderer.builder().escapeHtml(true).build()
    }

    val html = remember(markdown, textColorCss, linkColorCss, codeBgCss, bubbleBgCss) {
        val preProcessed = markdown
            .replace("\\(", "\\\\(")
            .replace("\\)", "\\\\)")
            .replace("\\[", "\\\\[")
            .replace("\\]", "\\\\]")

        val htmlBody = renderer.render(parser.parse(preProcessed))

        """
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
          <style>
            * { box-sizing: border-box; }
            html, body {
              margin: 0; padding: 0;
              background: transparent;
              width: 100%;
            }
            body {
              padding: 12px 16px;
              color: $textColorCss;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
              line-height: 1.6;
              font-size: 15px;
              word-wrap: break-word;
              min-height: 100%;
            }
            #content {
              width: 100%; 
              display: block;
              overflow: visible;
            }
            .MathJax_Display { margin: 0.8em 0 !important; overflow-x: auto; }
            pre { background: $codeBgCss; padding: 10px; border-radius: 8px; overflow-x: auto; white-space: pre-wrap; }
            p:first-child { margin-top: 0; }
            p:last-child { margin-bottom: 0; }
            mjx-container { outline: none !important; margin: 0.5em 0 !important; }
          </style>

          <script>
            window.MathJax = {
              tex: {
                inlineMath: [['$', '$'], ['\\(', '\\)']],
                displayMath: [['$$', '$$'], ['\\[', '\\]']],
                processEscapes: true
              },
              options: {
                renderActions: {
                  addMenu: []
                }
              },
              startup: {
                pageReady: () => {
                  return MathJax.startup.defaultPageReady().then(() => {
                    requestAnimationFrame(reportHeight);
                  });
                }
              }
            };
          </script>
          <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
        </head>
        <body>
          <div id="content">$htmlBody</div>
          <script>
            function reportHeight() {
              if (window.Android) {
                const body = document.body;
                const html = document.documentElement;
                // 获取最真实的高度，防止底部被截断
                const height = Math.max(body.scrollHeight, body.offsetHeight, html.offsetHeight);
                window.Android.reportHeight(height);
              }
            }
            
            const observer = new ResizeObserver(() => {
              reportHeight();
            });
            observer.observe(document.body);
            
            window.onload = reportHeight;
            setTimeout(reportHeight, 500);
            setTimeout(reportHeight, 2000);
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.height(heightDp).fillMaxWidth(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun reportHeight(height: Float) {
                        post {
                            if (height > 0) {
                                val newDp = with(density) { height.toDp() }
                                if (Math.abs(newDp.value - heightDp.value) > 1f) {
                                    heightDp = newDp
                                }
                            }
                        }
                    }
                }, "Android")
            }
        },
        update = { wv ->
            // 这里建议只有在 html 内容真正变化时才更新，防止闪烁
            // Compose 的 update 块在 recompose 时会执行
            wv.loadDataWithBaseURL("https://cdn.jsdelivr.net/", html, "text/html", "UTF-8", null)
        }
    )
}

fun Color.toCssHex(): String {
    val rgb = this.toArgb() and 0x00FFFFFF
    return String.format("#%06X", rgb)
}
