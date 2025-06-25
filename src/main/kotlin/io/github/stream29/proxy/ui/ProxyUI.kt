package io.github.stream29.proxy.ui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.stream29.proxy.config
import io.github.stream29.proxy.lmStudioServer
import io.github.stream29.proxy.ollamaServer
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.OutputStream
import java.io.PrintStream
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.system.exitProcess

class ProxyUI : JFrame("Proxy As Local Model") {
    companion object {
        init {
            // Set up FlatLaf look and feel with custom window decorations
            try {
                // Enable custom window decorations - use macOS specific property
                if (System.getProperty("os.name").lowercase().contains("mac")) {
                    System.setProperty("flatlaf.macos.useWindowDecorations", "true")
                    System.setProperty("apple.awt.application.appearance", "system")
                } else {
                    System.setProperty("flatlaf.useWindowDecorations", "true")
                }

                // Set additional properties for better macOS integration
                UIManager.put("JRootPane.isWindowDecoration", true)
                JFrame.setDefaultLookAndFeelDecorated(true)

                // Customize titlebar appearance
                UIManager.put("TitlePane.background", Color(30, 33, 40))
                UIManager.put("TitlePane.foreground", Color.WHITE)
                UIManager.put("TitlePane.inactiveBackground", Color(43, 45, 49))
                UIManager.put("TitlePane.inactiveForeground", Color(180, 180, 180))
                UIManager.put("TitlePane.closeHoverBackground", Color(232, 17, 35))
                UIManager.put("TitlePane.closePressedBackground", Color(241, 112, 122))
                UIManager.put("TitlePane.buttonHoverBackground", Color(52, 57, 66))
                UIManager.put("TitlePane.buttonPressedBackground", Color(66, 70, 77))

                UIManager.setLookAndFeel(FlatDarkLaf())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Status indicators
    private val lmStudioStatusLabel = JLabel("LM Studio: Initializing...").apply {
        font = font.deriveFont(Font.BOLD)
    }
    private val ollamaStatusLabel = JLabel("Ollama: Initializing...").apply {
        font = font.deriveFont(Font.BOLD)
    }

    // Log area
    private val logTextArea = JTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        background = Color(40, 44, 52)
        foreground = Color(220, 223, 228)
        caretColor = Color.WHITE
    }
    private val logScrollPane = JScrollPane(logTextArea).apply {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }

    // Control buttons
    private val startStopButton = JButton("Stop Service").apply {
        background = Color(61, 90, 254)
        foreground = Color.WHITE
        font = font.deriveFont(Font.BOLD)
        isFocusPainted = false
        border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
    }

    private var isRunning = true
    private var job: Job? = null

    // Custom OutputStream that redirects to the JTextArea
    private inner class TextAreaOutputStream : OutputStream() {
        override fun write(b: Int) {
            SwingUtilities.invokeLater {
                logTextArea.append(b.toChar().toString())
                logTextArea.caretPosition = logTextArea.document.length
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            val str = String(b, off, len)
            SwingUtilities.invokeLater {
                logTextArea.append(str)
                logTextArea.caretPosition = logTextArea.document.length
            }
        }
    }

    // Store original System.out and System.err
    private val originalOut = System.out
    private val originalErr = System.err

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(800, 600)
        minimumSize = Dimension(600, 400)
        layout = BorderLayout()

        // Center the window on the screen
        setLocationRelativeTo(null)

        // Create a modern header panel
        val headerPanel = JPanel(BorderLayout(20, 0)).apply {
            background = Color(30, 33, 40)
            border = BorderFactory.createEmptyBorder(15, 20, 15, 20)
            preferredSize = Dimension(800, 80)  // Increased height
            minimumSize = Dimension(600, 80)    // Set minimum size
            isVisible = true
        }

        // App title
        val titleLabel = JLabel("Proxy As Local Model").apply {
            font = Font("Segoe UI", Font.BOLD, 18)
            foreground = Color.WHITE
        }
        headerPanel.add(titleLabel, BorderLayout.WEST)

        // Status panel - using GridLayout for more reliable layout
        val statusPanel = JPanel(GridLayout(2, 1, 0, 8)).apply {
            background = Color(30, 33, 40)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            preferredSize = Dimension(300, 70)  // Increased width by 100px and height by 10px
            minimumSize = Dimension(280, 60)    // Increased minimum size as well
            isVisible = true
        }

        // Create status indicators with modern styling
        val lmStudioPanel = createStatusIndicator(lmStudioStatusLabel)
        val ollamaPanel = createStatusIndicator(ollamaStatusLabel)

        // Add panels to the grid layout
        statusPanel.add(lmStudioPanel)
        statusPanel.add(ollamaPanel)

        // Ensure both panels are visible
        lmStudioPanel.isVisible = true
        ollamaPanel.isVisible = true

        headerPanel.add(statusPanel, BorderLayout.EAST)

        // Main content panel
        val contentPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 20, 20, 20)
            background = Color(43, 45, 49)
        }

        // Log panel with title
        val logPanel = JPanel(BorderLayout()).apply {
            background = Color(43, 45, 49)
            border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
        }

        val logTitlePanel = JPanel(BorderLayout()).apply {
            background = Color(43, 45, 49)
            border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        }

        val logTitle = JLabel("Logs").apply {
            font = Font("Segoe UI", Font.BOLD, 14)
            foreground = Color.WHITE
        }
        logTitlePanel.add(logTitle, BorderLayout.WEST)

        logPanel.add(logTitlePanel, BorderLayout.NORTH)
        logPanel.add(logScrollPane, BorderLayout.CENTER)

        contentPanel.add(logPanel, BorderLayout.CENTER)

        // Control panel
        val controlPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = Color(43, 45, 49)
            border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        }

        // Style the button
        startStopButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                startStopButton.background = Color(41, 70, 234)
            }

            override fun mouseExited(e: MouseEvent) {
                startStopButton.background = Color(61, 90, 254)
            }
        })

        controlPanel.add(startStopButton)
        contentPanel.add(controlPanel, BorderLayout.SOUTH)

        // Add components to frame
        add(headerPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)

        // Set up event handlers
        startStopButton.addActionListener {
            if (isRunning) {
                stopService()
            } else {
                startService()
            }
        }

        // Redirect System.out and System.err to the JTextArea
        redirectSystemStreams()

        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            if (isRunning) {
                job?.cancel()
            }
            // Restore original streams when the application exits
            restoreSystemStreams()
        })
    }

    private fun redirectSystemStreams() {
        System.setOut(PrintStream(TextAreaOutputStream(), true))
        System.setErr(PrintStream(TextAreaOutputStream(), true))
    }

    private fun restoreSystemStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    fun startService() {
        // Update button without animation
        isRunning = true
        startStopButton.text = "Stop Service"
        startStopButton.background = Color(61, 90, 254)

        job = MainScope().launch {
            try {
                // Initialize config
                logMessage("Initializing configuration...")
                config

                // Start status monitoring
                launch {
                    while (isActive) {
                        updateServerStatus()
                        delay(2000)
                    }
                }

                // Keep the service running
                logMessage("✅ Service started successfully")
                while (isActive) {
                    delay(10000)
                }
            } catch (t: Throwable) {
                logMessage("❌ ERROR: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    /**
     * Stops the running service and updates the UI to reflect the stopped state.
     *
     * This method performs the following actions:
     * - Marks the service as not running by setting the corresponding flag.
     * - Updates the text and background color of the start/stop button to indicate the service can be started.
     * - Cancels the background job responsible for service operation or status monitoring, if active.
     * - Logs a message to the UI indicating the service has been stopped.
     *
     * This does not perform any animations but ensures the UI state immediately reflects that the service is no longer running.
     */
    fun stopService() {
        // Update button without animation
        isRunning = false
        startStopButton.text = "Start Service"
        startStopButton.background = Color(76, 175, 80)

        job?.cancel()
        logMessage("🛑 Service stopped")
    }

    private fun updateServerStatus() {
        SwingUtilities.invokeLater {
            // LM Studio status
            val lmStudioStatus = if (lmStudioServer != null) {
                lmStudioStatusLabel.foreground = Color.GREEN.darker()
                "LM Studio: Running on port ${config.lmStudio.port}"
            } else {
                lmStudioStatusLabel.foreground = if (config.lmStudio.enabled) Color.RED else Color.GRAY
                "LM Studio: ${if (config.lmStudio.enabled) "Failed" else "Disabled"}"
            }
            lmStudioStatusLabel.text = lmStudioStatus
            lmStudioStatusLabel.isVisible = true

            // Update status without animation
            lmStudioStatusLabel.repaint()

            // Ollama status
            val ollamaStatus = if (ollamaServer != null) {
                ollamaStatusLabel.foreground = Color.GREEN.darker()
                "Ollama: Running on port ${config.ollama.port}"
            } else {
                ollamaStatusLabel.foreground = if (config.ollama.enabled) Color.RED else Color.GRAY
                "Ollama: ${if (config.ollama.enabled) "Failed" else "Disabled"}"
            }
            ollamaStatusLabel.text = ollamaStatus
            ollamaStatusLabel.isVisible = true

            // Update status without animation
            ollamaStatusLabel.repaint()

            // Update button state based on running status
            if (isRunning) {
                startStopButton.text = "Stop Service"
                startStopButton.background = Color(61, 90, 254)
            } else {
                startStopButton.text = "Start Service"
                startStopButton.background = Color(76, 175, 80)
            }

            // Force revalidation and repaint of the entire UI
            SwingUtilities.invokeLater {
                // Revalidate and repaint the entire UI
                revalidate()
                repaint()
            }
        }
    }

    fun logMessage(message: String) {
        SwingUtilities.invokeLater {
            logTextArea.append("${message}\n")
            logTextArea.caretPosition = logTextArea.document.length
        }
    }

    /**
     * Creates a styled status indicator panel with the given label
     */
    private fun createStatusIndicator(label: JLabel): JPanel {
        // Ensure label is properly configured
        label.apply {
            isVisible = true
            preferredSize = Dimension(250, 25) // Increased width by 100px and height by 5px
            minimumSize = Dimension(250, 25)   // Increased minimum size as well
            horizontalAlignment = SwingConstants.LEFT
        }

        // Create a custom round dot panel
        class StatusDot : JPanel() {
            init {
                preferredSize = Dimension(10, 10)
                minimumSize = Dimension(10, 10)
                maximumSize = Dimension(10, 10)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = background
                g2d.fillOval(0, 0, width, height)
            }
        }

        // Create the panel with a more reliable layout
        val panel = JPanel(BorderLayout(5, 0)).apply {
            background = Color(30, 33, 40)
            border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
            isVisible = true

            // Create status dot
            val statusDot = StatusDot().apply {
                background = Color.GRAY
                isVisible = true
            }

            // Add components with specific constraints
            add(statusDot, BorderLayout.WEST)
            add(label, BorderLayout.CENTER)

            // Update the status dot color based on the label's foreground color
            label.addPropertyChangeListener("foreground") { 
                statusDot.background = when {
                    label.foreground == Color.GREEN.darker() -> Color.GREEN
                    label.foreground == Color.RED -> Color.RED
                    else -> Color.GRAY
                }
                statusDot.repaint()
            }

            // Initial update of status dot color
            statusDot.background = when {
                label.foreground == Color.GREEN.darker() -> Color.GREEN
                label.foreground == Color.RED -> Color.RED
                else -> Color.GRAY
            }
        }

        return panel
    }
}
