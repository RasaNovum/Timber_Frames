import net.fabricmc.loom.task.RemapJarTask
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

// TODO: Update loom to match Runeweaver
abstract class RuneweaverLoomCompatibilityTransform : TransformAction<TransformParameters.None> {
    @get:org.gradle.api.artifacts.transform.InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: org.gradle.api.artifacts.transform.TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-loom-compatible.jar")

        JarFile(input).use { inputJar ->
            val manifest = (inputJar.manifest ?: Manifest()).apply {
                mainAttributes.remove(Attributes.Name("Fabric-Loom-Version"))
            }

            JarOutputStream(output.outputStream(), manifest).use { outputJar ->
                inputJar.entries().asSequence()
                    .filterNot { entry ->
                        entry.name == JarFile.MANIFEST_NAME ||
                            entry.name.startsWith("META-INF/") &&
                            (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || entry.name.endsWith(".DSA"))
                    }
                    .forEach { entry ->
                        outputJar.putNextEntry(JarEntry(entry.name).apply { setTime(entry.time) })
                        if (!entry.isDirectory) {
                            inputJar.getInputStream(entry).use { it.copyTo(outputJar) }
                        }
                        outputJar.closeEntry()
                    }
            }
        }
    }
}

plugins {
    id("fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

loom {
    mixin {
        defaultRefmapName = "${property("mod.id")}.refmap.json"
        useLegacyMixinAp = true
    }
    accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.accesswidener")
}

repositories {
    mavenLocal()
    maven("https://raw.githubusercontent.com/Rasa-Novum/runeweaver/maven/")
    maven("https://raw.githubusercontent.com/Rasa-Novum/Rosetta_Library/maven/")
    maven("https://repo.sleeping.town/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.parchmentmc.org")
    maven("https://modmaven.k-4u.nl/")
    maven("https://jm.gserv.me/repository/maven-public/")
    maven("https://cursemaven.com")
    mavenCentral()
}

val runeweaverDependency = "com.rasanovum.runeweaver:runeweaver-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.runeweaver")}"
val runeweaverRosettaDependency = "com.rasanovum.runeweaver:runeweaver-rosetta-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.runeweaver")}"
val runeweaverCompatible = configurations.create("runeweaverCompatible") {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "runeweaver-loom-compatible"
        )
    }
}

dependencies {
    registerTransform(RuneweaverLoomCompatibilityTransform::class) {
        from.attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            ArtifactTypeDefinition.JAR_TYPE
        )
        to.attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "runeweaver-loom-compatible"
        )
    }

    val rosettaDependency = "com.rasanovum.rosetta:rosetta-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.rosetta")}"

    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation(rosettaDependency)
    add(runeweaverCompatible.name, runeweaverDependency)
    add(runeweaverCompatible.name, runeweaverRosettaDependency)
    include(rosettaDependency)
    include(runeweaverDependency)
    include(runeweaverRosettaDependency)
    modImplementation("maven.modrinth:sodium:${property("deps.sodium")}")
    modImplementation("maven.modrinth:iris:${property("deps.iris")}")
    if (property("deps.minecraft") == "1.20.1") {
        modImplementation("maven.modrinth:indium:1.0.36+mc1.20.1")
    }

    implementation("org.anarres:jcpp:1.4.14")
    implementation("io.github.douira:glsl-transformer:2.0.1")

    annotationProcessor("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
    modImplementation("com.google.code.gson:gson:2.10.1")
    modImplementation("org.anarres:jcpp:1.4.14")
}

dependencies {
    modCompileOnly(files(runeweaverCompatible))
    modRuntimeOnly(files(runeweaverCompatible))
}

if (property("deps.minecraft") == "1.20.1") {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.lwjgl:lwjgl:3.3.1",
            "org.lwjgl:lwjgl-glfw:3.3.1",
            "org.lwjgl:lwjgl-jemalloc:3.3.1",
            "org.lwjgl:lwjgl-openal:3.3.1",
            "org.lwjgl:lwjgl-opengl:3.3.1",
            "org.lwjgl:lwjgl-stb:3.3.1",
            "org.lwjgl:lwjgl-tinyfd:3.3.1"
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val props = mapOf(
        "version" to project.version,
        "mc" to project.property("deps.minecraft"),

        "modName" to project.property("mod.name"),
        "modId" to project.property("mod.id"),
        "modDescription" to project.property("mod.description"),
        "authors" to project.property("mod.authors"),
        "contributors" to project.property("mod.contributors"),
        "license" to project.property("mod.license"),
        "homepage" to project.property("mod.homepage"),
        "issues" to project.property("mod.issues"),
        "sources" to project.property("mod.sources"),

        "fl" to project.property("deps.fabric_loader"),
        "fapi" to project.property("deps.fabric_api"),

        "rosetta" to project.property("deps.rosetta"),
        "runeweaver" to project.property("deps.runeweaver"),
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") { expand(props) }

    exclude("**/neoforge.mods.toml", "**/mods.toml")
}

stonecutter {
    val loaderClientField = "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)"
    val stringReplacements = mapOf(
        "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)" to loaderClientField,
        "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)" to loaderClientField
    )

    stringReplacements.forEach { (from, to) ->
        replacements.string {
            direction = true
            replace(from, to)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    options.release.set(javaVersion)
}

sourceSets.main {
    java.exclude("**/client/render/forge/**")
}


java {
    withSourcesJar()
    val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

publishMods {
    file = tasks.named<RemapJarTask>("remapJar").get().archiveFile
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided"
    type = STABLE
    modLoaders.add("fabric")
    
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = property("publish.modrinth") as String
        minecraftVersions.add(property("deps.minecraft") as String)

        requires { slug = "fabric-api" }
    }
    
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = property("publish.curseforge") as String
        minecraftVersions.add(property("deps.minecraft") as String)

        clientRequired = true
        serverRequired = true
        
        requires { slug = "fabric-api" }
    }
}
