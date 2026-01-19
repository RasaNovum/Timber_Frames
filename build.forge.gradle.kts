import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.minecraftforge.gradle") version ("6.0.46")
    id("org.spongepowered.mixin") version "0.7.+"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
base.archivesName = property("mod.id") as String

jarJar.enable()

minecraft {
    mappings("parchment", "2023.09.03-1.20.1")

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            args("--mod", property("mod.id") as String, "--all", "--output", file("src/generated/resources/"), "--existing", file("src/main/resources/"))
            mods {
                create(property("mod.id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

mixin {
    add(sourceSets.main.get(), "timber_frames.refmap.json")
    config("timber_frames.mixins.json")
}

repositories {
    mavenLocal()
    maven("https://raw.githubusercontent.com/Rasa-Novum/Mixson/maven/")
    maven("https://raw.githubusercontent.com/Rasa-Novum/Rosetta_Library/maven/")
    maven("https://raw.githubusercontent.com/xameryn/Mixson/maven/")
    maven("https://maven.su5ed.dev/releases")
    maven("https://repo.sleeping.town/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.parchmentmc.org")
    maven("https://modmaven.k-4u.nl/")
    maven("https://jm.gserv.me/repository/maven-public/")
    maven("https://cursemaven.com")
    maven("https://maven.sinytra.org/releases")
    mavenCentral()
}


dependencies {
val mixsonDependency = "com.rasanovum.mixson:mixson-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.mixson")}"
val mixsonRosettaDependency = "com.rasanovum.mixson:mixson-rosetta-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.mixson")}"
    val rosettaDependency = "com.rasanovum.rosetta:rosetta-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.rosetta")}"

    minecraft("net.minecraftforge:forge:${property("deps.minecraft")}-${property("deps.forge_version")}")

    implementation(fg.deobf(rosettaDependency))
    compileOnly(mixsonDependency)
    runtimeOnly(mixsonDependency)
    compileOnly(mixsonRosettaDependency)
    runtimeOnly(mixsonRosettaDependency)
    jarJar("com.rasanovum.mixson:mixson-${property("deps.minecraft")}-${property("deps.loader")}:[${property("deps.mixson")}]")
    jarJar("com.rasanovum.mixson:mixson-rosetta-${property("deps.minecraft")}-${property("deps.loader")}:[${property("deps.mixson")}]") {
        exclude(group = "com.rasanovum.rosetta")
    }
    jarJar("com.rasanovum.rosetta:rosetta-${property("deps.minecraft")}-${property("deps.loader")}:[${property("deps.rosetta")}]")
    implementation("com.google.code.gson:gson:2.10.1")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

configurations.named("jarJar") {
    resolutionStrategy.force("com.rasanovum.rosetta:rosetta-${property("deps.minecraft")}-${property("deps.loader")}:${property("deps.rosetta")}")
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
        "forge" to project.property("deps.forge_version"),
        "rosetta" to project.property("deps.rosetta"),
        "mixson" to project.property("deps.mixson"),
    )

    inputs.properties(props)

    filesMatching("META-INF/mods.toml") {
        expand(props)
    }

    exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml")
}

stonecutter {
    val loaderClientField = "@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)"
    val stringReplacements = mapOf(
        "@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)" to loaderClientField,
        "@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)" to loaderClientField
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
    val javaVersion = 17
    options.release.set(javaVersion)
}

java {
    withSourcesJar()
    val javaVersion = 17
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

publishMods {
    file = tasks.named<Jar>("jarJar").get().archiveFile
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided"
    type = STABLE
    modLoaders.add("forge")
    
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = property("publish.modrinth") as String
        minecraftVersions.add(property("deps.minecraft") as String)

    }
    
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = property("publish.curseforge") as String
        minecraftVersions.add(property("deps.minecraft") as String)

        clientRequired = true
        serverRequired = true
    }
}

tasks.named("publishModrinth") {
    dependsOn("reobfJarJar")
}

tasks.named("publishCurseforge") {
    dependsOn("reobfJarJar")
}

tasks.named("publishMods") {
    dependsOn("reobfJarJar")
}
