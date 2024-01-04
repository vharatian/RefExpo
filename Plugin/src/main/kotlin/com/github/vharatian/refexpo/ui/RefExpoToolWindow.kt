import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.ui.InspectionRunner
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
    private val ignoreInterFile = JCheckBox("Ignore intra file referencing")
    private val ignoreInterClass = JCheckBox("Ignore intra class referencing")
    private val ignoreInterMethod = JCheckBox("Ignore intra method referencing")
    private val addPackageName = JCheckBox("Include package name in classes")
    private val ignoringFilesRegex = JTextField("")
    private val ignoringClassesRegex = JTextField("")
    private val ignoringMethodsRegex = JTextField("")
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
        addTextField("Output file path:", filePathField)

        // Configure and add file path field
        addTextField("Ignoring files regex:", ignoringFilesRegex)
        addTextField("Ignoring classes regex:", ignoringClassesRegex)
        addTextField("Ignoring methods regex:", ignoringMethodsRegex)

        // Configure and add the checkbox
        addComponent(prepareLefAlignedComponent(ignoreInterFile))
        addComponent(prepareLefAlignedComponent(ignoreInterClass), false)
        addComponent(prepareLefAlignedComponent(ignoreInterMethod), false)
        addComponent(prepareLefAlignedComponent(addPackageName), false)

        // Configure and add run button
        runButton.addActionListener { runInspection() }
        runButton.alignmentX = JPanel.CENTER_ALIGNMENT
        addComponent(runButton)

        setStatusMessage("Ready")
    }

    private fun addTextField(message: String, textField: JTextField) {
        addComponent(prepareLefAlignedComponent(JLabel(message)))
        textField.alignmentX = JPanel.CENTER_ALIGNMENT
        textField.maximumSize = Dimension(Integer.MAX_VALUE - 100, filePathField.preferredSize.height)
        addComponent(textField, false)
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
            ignoringFilesRegex.text,
            ignoringClassesRegex.text,
            ignoringMethodsRegex.text,
            ignoreInterFile.isSelected,
            ignoreInterClass.isSelected,
            ignoreInterMethod.isSelected,
            addPackageName.isSelected
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
