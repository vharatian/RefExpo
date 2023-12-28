import com.github.vharatian.refexpo.toolWindow.InspectionRunner
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import java.awt.*
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*

class RefExpoToolWindow(private val project: Project) {
    private val runButton = JButton("Run Inspection")
    private val statusLabel = JLabel()
    private val mainPanel = JPanel()
    private val filePathLabel = JLabel("Export File Path:")
    private val filePathField = JTextField("references.csv")
    private val runInspectionAction = InspectionRunner(project)
    private val verticalPadding = 20

    init {
        runButton.addActionListener(ActionListener {

            runInspection()
        })

        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.add(Box.createVerticalStrut(verticalPadding))

        // Configure components and add them to the main panel with padding
        statusLabel.alignmentX = JPanel.CENTER_ALIGNMENT
        val currentFontSize = statusLabel.font.size
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD, currentFontSize * 1.3f) // Set font to bold
        statusLabel.foreground = Color.WHITE // Set a distinct color
        mainPanel.add(statusLabel)
        mainPanel.add(Box.createVerticalStrut(verticalPadding))

        filePathLabel.alignmentX = JPanel.CENTER_ALIGNMENT
        mainPanel.add(filePathLabel)

        filePathField.alignmentX = JPanel.CENTER_ALIGNMENT
        filePathField.maximumSize = Dimension(Integer.MAX_VALUE, filePathField.preferredSize.height)
        mainPanel.add(filePathField)
        mainPanel.add(Box.createVerticalStrut(verticalPadding)) // Padding after file path field

        runButton.alignmentX = JPanel.CENTER_ALIGNMENT
        mainPanel.add(runButton)

        setStatusMessage("Ready")
    }

    private fun runInspection() {
        val filePath = filePathField.text ?: "references.csv"

        val csvFile = if (filePath.startsWith("/"))
            File(filePath)
        else
            File(project.basePath, filePath)

        if (DumbService.isDumb(project)) {
            setStatusMessage("Indexing in progress. Please wait...")
            return
        }

        if (isBuildInProgress(project)) {
            setStatusMessage("Build in progress. Please wait...")
            return
        }

        setButtonEnabled(false)
        setStatusMessage("Working...")

        runInspectionAction.run(csvFile, ::onFinished)
    }

    fun getContent(): JPanel = mainPanel

    fun setButtonEnabled(enabled: Boolean) {
        runButton.isEnabled = enabled
    }

    fun setStatusMessage(message: String) {
        statusLabel.text = "Message: $message"
    }

    private fun isBuildInProgress(project: Project): Boolean {
        return CompilerManager.getInstance(project).isCompilationActive
    }

    fun onFinished(success: Boolean){
        SwingUtilities.invokeLater {
            setButtonEnabled(true)
            if (success) {
                setStatusMessage("Task Completed")
            } else {
                setStatusMessage("Cancelled")
            }

        }

    }
}
