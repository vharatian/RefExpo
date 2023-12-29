import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.toolWindow.InspectionRunner
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

class RefExpoToolWindow(private val project: Project) {
    private val runButton = JButton("Run Inspection")
    private val statusLabel = JLabel()
    private val mainPanel = JPanel()
    private val filePathField = JTextField("references.csv")
    private val ignoreInterFile = JCheckBox("Ignore inter file referencing")
    private val ignoreInterClass = JCheckBox("Ignore inter class referencing")
    private val ignoreInterMethod = JCheckBox("Ignore inter method referencing")
    private val runInspectionAction = InspectionRunner(project)
    private val verticalPadding = 20

    init {
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        // Configure components and add them to the main panel with padding
        statusLabel.alignmentX = JPanel.CENTER_ALIGNMENT
        val currentFontSize = statusLabel.font.size
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD, currentFontSize * 1.3f) // Set font to bold
        statusLabel.foreground = JBColor.WHITE // Set a distinct color
        addComponent(statusLabel)

        //Configure and add file path field
        addComponent(prepareLefAlignedComponent(JLabel("Export File Path:")))
        filePathField.alignmentX = JPanel.CENTER_ALIGNMENT
        filePathField.maximumSize = Dimension(Integer.MAX_VALUE - 10, filePathField.preferredSize.height)
        addComponent(filePathField, false)

        // Configure and add the checkbox
        addComponent(prepareLefAlignedComponent(ignoreInterFile))
        addComponent(prepareLefAlignedComponent(ignoreInterClass), false)
        addComponent(prepareLefAlignedComponent(ignoreInterMethod), false)

        // Configure and add run button
        runButton.addActionListener { runInspection() }
        runButton.alignmentX = JPanel.CENTER_ALIGNMENT
        addComponent(runButton)

        setStatusMessage("Ready")
    }

    private fun prepareLefAlignedComponent(component: JComponent): JPanel {
        val wrapperPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        wrapperPanel.add(component)
        wrapperPanel.maximumSize = Dimension(Integer.MAX_VALUE, wrapperPanel.getPreferredSize().height)
        return wrapperPanel
    }

    private fun addComponent(component: JComponent, padding: Boolean = true) {
        if (padding)
            mainPanel.add(Box.createVerticalStrut(verticalPadding))

        mainPanel.add(component)
    }

    private fun runInspection() {
        val filePath = filePathField.text ?: "references.csv"

        if (filePath.isEmpty()) {
            setStatusMessage("Please enter a valid file path")
            return
        }

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

        val config = RefExpoExecutionConfig(
            filePath,
            ignoreInterFile.isSelected,
            ignoreInterClass.isSelected,
            ignoreInterMethod.isSelected
        )

        runInspectionAction.run(config, ::onFinished)
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

    fun onFinished(success: Boolean) {
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
