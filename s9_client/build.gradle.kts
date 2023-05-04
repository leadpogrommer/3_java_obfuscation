buildscript{
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.1.1") {
            exclude("com.android.tools.build")
        }
    }
}

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("plugin.serialization") version "1.8.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar { enabled = false }
artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
//    archiveFileName.set(rootProject.name + ".jar")
    archiveFileName.set("client_full.jar")
    val dependencyPackage = "${rootProject.group}.dependencies.${rootProject.name.toLowerCase()}"
    // your relocations here
//    exclude("ScopeJVMKt.class")
//    exclude("DebugProbesKt.bin")
    exclude("META-INF/**")
}

tasks.register<proguard.gradle.ProGuardTask>("proguard") {
    // here is where you configure your Proguard stuff, you can include libraries directly
    // through here, or in the configuration file, I usually just use the configuration file
    // to do everything (it can be any name and extension you want, just using .pro here cause
    // that's what Android uses)
    configuration("proguard-rules.pro")
    outputs.upToDateWhen { false }
    dependsOn("clean")
    dependsOn("shadowJar")

}


tasks.withType<JavaCompile> {
    sourceCompatibility = "16"
    targetCompatibility = "16"
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "16" }

kotlin {
    jvmToolchain(8)
}
application {
    mainClass.set("ru.leadpogrommer.sb.client.MainKt")
}