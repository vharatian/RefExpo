//import com.intellij.openapi.compiler.CompilerManager
import com.github.vharatian.refexpo.models.RefExpoExecutionConfig
import com.github.vharatian.refexpo.ui.InspectionRunner
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import javax.swing.*


class RefExpoToolWindow(private val project: Project) {
    private val runButton = JButton("Run Inspection")
    private val statusLabel = JLabel()
    private val contentPanel = JPanel()
    private val rootPanel= JPanel(BorderLayout())
    private val filePathField = JTextField("refExpo.csv")
    private val ignoreInterFile = JCheckBox("Ignore intra file referencing")
    private val ignoreInterClass = JCheckBox("Ignore intra class referencing")
    private val ignoreInterMethod = JCheckBox("Ignore intra method referencing")
    private val ignoringFilesRegex = JTextField("")
    private val ignoringClassesRegex = JTextField("")
    private val ignoringMethodsRegex = JTextField("")
    private val runInspectionAction = InspectionRunner(project)
    private val verticalPadding = 20

    init {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // Configure components and add them to the main panel with padding
        statusLabel.alignmentX = JPanel.CENTER_ALIGNMENT
        val currentFontSize = statusLabel.font.size
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD, currentFontSize * 1.3f)
        statusLabel.foreground = Color.WHITE
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

        // Configure and add run button
        runButton.addActionListener { runInspection() }
        runButton.alignmentX = JPanel.CENTER_ALIGNMENT
        addComponent(runButton)

        val scrollPane = JBScrollPane(contentPanel,JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        rootPanel.add(scrollPane, BorderLayout.CENTER)

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
            contentPanel.add(Box.createVerticalStrut(verticalPadding))

        contentPanel.add(component)
    }

    private fun runInspection() {
        val filePath = filePathField.text ?: "refExpo.csv"

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
            ignoreInterMethod.isSelected
        )

        runInspectionAction.run(config, ::onFinished)
    }

    fun getContent(): JPanel = rootPanel

    fun setButtonEnabled(enabled: Boolean) {
        runButton.isEnabled = enabled
    }

    fun setStatusMessage(message: String) {
        statusLabel.text = "Message: $message"
    }

    private fun isBuildInProgress(project: Project): Boolean {
        return false
//        return CompilerManager.getInstance(project).isCompilationActive
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
