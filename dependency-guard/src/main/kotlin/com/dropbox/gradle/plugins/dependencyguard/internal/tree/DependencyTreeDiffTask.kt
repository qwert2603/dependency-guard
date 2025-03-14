package com.dropbox.gradle.plugins.dependencyguard.internal.tree

import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPlugin
import com.dropbox.gradle.plugins.dependencyguard.internal.ConfigurationValidators
import com.dropbox.gradle.plugins.dependencyguard.internal.getResolvedComponentResult
import com.dropbox.gradle.plugins.dependencyguard.internal.projectConfigurations
import com.dropbox.gradle.plugins.dependencyguard.internal.utils.DependencyGuardTreeDiffer
import com.dropbox.gradle.plugins.dependencyguard.internal.utils.OutputFileUtils
import com.dropbox.gradle.plugins.dependencyguard.internal.utils.Tasks.declareCompatibilities
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class DependencyTreeDiffTask : DefaultTask() {

    init {
        group = DependencyGuardPlugin.DEPENDENCY_GUARD_TASK_GROUP
    }

    @get:Input
    abstract val resolvedComponentResult: Property<Provider<ResolvedComponentResult>>

    @get:Input
    abstract val configurationName: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val shouldBaseline: Property<Boolean>

    @get:OutputFile
    abstract val buildDirOutputFile: Property<File>

    @get:OutputFile
    abstract val projectDirOutputFile: Property<File>

    fun setParams(
        project: Project,
        configurationName: String,
        shouldBaseline: Boolean,
    ) {
        ConfigurationValidators.validateConfigurationsAreAvailable(
            project,
            listOf(configurationName)
        )
        val projectDependenciesDir = OutputFileUtils.projectDirDependenciesDir(project)
        val projectDirOutputFile: File = DependencyGuardTreeDiffer.projectDirOutputFile(projectDependenciesDir, configurationName)
        val buildDirOutputFile: File = DependencyGuardTreeDiffer.buildDirOutputFile(project.layout.buildDirectory.get(), configurationName)
        val projectPath = project.path
        val resolvedComponentResult = project.projectConfigurations.getResolvedComponentResult(configurationName)

        this.resolvedComponentResult.set(resolvedComponentResult)
        this.projectDirOutputFile.set(projectDirOutputFile)
        this.buildDirOutputFile.set(buildDirOutputFile)
        this.configurationName.set(configurationName)
        this.projectPath.set(projectPath)
        this.shouldBaseline.set(shouldBaseline)

        declareCompatibilities()
    }

    @TaskAction
    fun generate() {
        // USES INTERNAL API
        val asciiRenderer = AsciiDependencyReportRenderer2()

        val resolvedComponentResult: Provider<ResolvedComponentResult> = resolvedComponentResult.get()
        asciiRenderer.setOutputFile(buildDirOutputFile.get())
        asciiRenderer.prepareVisit()
        asciiRenderer.render(resolvedComponentResult.get())

        DependencyGuardTreeDiffer(
            projectPath = projectPath.get(),
            configurationName = configurationName.get(),
            shouldBaseline = shouldBaseline.get(),
            projectDirOutputFile = projectDirOutputFile.get(),
            buildDirOutputFile = buildDirOutputFile.get(),
        ).performDiff()
    }
}
