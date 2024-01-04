import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RefExpoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val refExpoToolWindow = RefExpoToolWindow(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(refExpoToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
