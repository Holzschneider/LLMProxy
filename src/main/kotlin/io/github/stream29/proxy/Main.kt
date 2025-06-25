package io.github.stream29.proxy

import com.formdev.flatlaf.FlatDarkLaf
import io.github.stream29.proxy.ui.ProxyUI
import kotlinx.coroutines.runBlocking
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    SwingUtilities.invokeLater {
        val ui = ProxyUI()
        ui.isVisible = true
        ui.startService()
    }
}
