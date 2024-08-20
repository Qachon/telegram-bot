package eu.vendeli.ktgram.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources

abstract class KtGramPlugin : Plugin<Project> {
    private val log = Logging.getLogger(KtGramPlugin::class.java)
    private val libVer = loadPropertyFromResources("ktgram.properties", "ktgram.version")

    final override fun apply(project: Project) {
        val pluginExtension = project.extensions.create("ktGram", KtGramExt::class.java)
        val kspPluginPresent = project.plugins.hasPlugin("com.google.devtools.ksp")
        var kspProcessorApplied = false
        val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

        project.configurations.configureEach {
            if (name.startsWith("ksp")) dependencies.whenObjectAdded {
                if (group == "eu.vendeli" && name == "ksp") kspProcessorApplied = true
            }
        }

        project.afterEvaluate {
            val targetVer = pluginExtension.forceVersion.getOrElse(libVer)
            if (pluginExtension.addSnapshotRepo.getOrElse(false)) {
                project.repositories.maven {
                    name = "KtGramSnapRepo"
                    setUrl("https://mvn.vendeli.eu/telegram-bot")
                }
            }

            project.applyDependencies(targetVer, isMultiplatform, kspProcessorApplied)

            project.extensions.configure<KspExtension> {
                pluginExtension.packages.orNull?.takeIf { it.isNotEmpty() }?.joinToString(";")?.let {
                    arg("package", it)
                }
            }

            if (!kspPluginPresent) log.error(
                "For full use of `telegram-bot` KSP Gradle Plugin is needed. " +
                    "Add 'com.google.devtools.ksp' plugin to your build, " +
                    "f.e by adding 'id(\"com.google.devtools.ksp\")' to 'plugins' section of the build.gradle[.kts] file",
            )
        }
    }

    private fun Project.applyDependencies(depVersion: String, isMultiplatform: Boolean, kspProcessorApplied: Boolean) {
        if (isMultiplatform) project.extensions.configure<KotlinMultiplatformExtension> {
            targets.forEach { target ->
                val tName = if (target.targetName == "metadata") "CommonMainMetadata"
                else target.targetName.replaceFirstChar { it.uppercaseChar() }

                project.dependencies.add(
                    "ksp$tName",
                    "eu.vendeli:ksp:$depVersion",
                )
            }
        }

        when {
            kspProcessorApplied -> {}
            isMultiplatform -> {
                project.extensions.configure(KotlinProjectExtension::class.java) {
                    sourceSets["commonMain"].apply {
                        dependencies {
                            implementation("eu.vendeli:telegram-bot:$depVersion")
                        }
                    }
                }

                project.dependencies {
                    add("ksp", "eu.vendeli:ksp:$depVersion")
                }
            }

            else -> {
                project.dependencies.add("implementation", "eu.vendeli:telegram-bot:$depVersion")
                project.dependencies {
                    add("ksp", "eu.vendeli:ksp:$depVersion")
                }
            }
        }
    }
}
