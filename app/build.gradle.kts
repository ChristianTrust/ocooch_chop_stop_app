import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Helper function to read/initialize the build number
fun getOrInitBuildNumber(): Int {
    val buildNumberFile = project.rootProject.file("build_number.properties")
    val properties = Properties()
    if (buildNumberFile.exists()) {
        buildNumberFile.inputStream().use { properties.load(it) }
    } else {
        properties.setProperty("buildNumber", "1")
        buildNumberFile.outputStream().use { properties.store(it, "Auto-generated build number tracking") }
    }
    return properties.getProperty("buildNumber", "1").toIntOrNull() ?: 1
}

// Helper function to increment the build number for the next run
fun incrementBuildNumber() {
    val buildNumberFile = project.rootProject.file("build_number.properties")
    val properties = Properties()
    if (buildNumberFile.exists()) {
        buildNumberFile.inputStream().use { properties.load(it) }
    }
    val current = properties.getProperty("buildNumber", "1").toIntOrNull() ?: 1
    val next = current + 1
    properties.setProperty("buildNumber", next.toString())
    buildNumberFile.outputStream().use { properties.store(it, "Auto-generated build number tracking") }
    println("Build number incremented to $next for the next build.")
}

android {
    namespace = "com.christian.ocoochchopstopmk2"
    compileSdk = 35

    val currentBuildNumber = getOrInitBuildNumber()

    signingConfigs {
        create("release") {
            val localProperties = Properties()
            val localPropertiesFile = project.rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { localProperties.load(it) }
            }

            val keystoreFile = project.rootProject.file("release.keystore")
            // If release.keystore does not exist in the root folder, generate one automatically
            if (!keystoreFile.exists()) {
                println("No release.keystore found. Generating one automatically...")
                try {
                    val process = ProcessBuilder(
                        "keytool", "-genkeypair",
                        "-v",
                        "-keystore", keystoreFile.absolutePath,
                        "-alias", "chopstop",
                        "-keyalg", "RSA",
                        "-keysize", "2048",
                        "-validity", "100000",
                        "-storepass", "chopstop123",
                        "-keypass", "chopstop123",
                        "-dname", "CN=Ocooch, OU=ChopStop, O=Ocooch, L=Local, S=Local, C=US"
                    ).start()
                    process.waitFor()
                    println("release.keystore generated successfully!")
                } catch (e: Exception) {
                    println("Failed to auto-generate release.keystore: ${e.message}")
                }
            }

            // Load signing details (use custom values if set in local.properties, else use the auto-generated ones)
            val customStoreFile = localProperties.getProperty("signing.store.file")
            if (customStoreFile != null) {
                storeFile = file(customStoreFile)
                storePassword = localProperties.getProperty("signing.store.password")
                keyAlias = localProperties.getProperty("signing.key.alias")
                keyPassword = localProperties.getProperty("signing.key.password")
            } else if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "chopstop123"
                keyAlias = "chopstop"
                keyPassword = "chopstop123"
            } else {
                // Absolute fallback to debug configuration so build doesn't crash if generation failed
                storeFile = project.rootProject.file("debug.keystore")
            }
        }
    }

    defaultConfig {
        applicationId = "com.christian.ocoochchopstopmk2"
        minSdk = 30
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = currentBuildNumber
        versionName = "1.0.$currentBuildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.compose.ui:ui-graphics:1.11.2")
    implementation("androidx.compose.ui:ui-text:1.11.2")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation:1.8.3")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.github.mik3y:usb-serial-for-android:3.9.0")
    implementation("androidx.compose.foundation:foundation-layout:1.9.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks.register("uploadApkToServer") {
    group = "publishing"
    description = "Builds the release APK and copies it to the self-hosted server over SCP, auto-incrementing build number and pruning old builds"

    dependsOn("assembleRelease")

    doLast {
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                properties.load(stream)
            }
        }

        val host = properties.getProperty("server.ssh.host") ?: ""
        val user = properties.getProperty("server.ssh.user") ?: ""
        val remotePath = properties.getProperty("server.ssh.downloads_path") ?: ""
        val destFilenameBase = properties.getProperty("server.ssh.filename") ?: "chop-stop"
        val maxBuildsStr = properties.getProperty("server.ssh.max_builds") ?: "5"
        val maxBuilds = maxBuildsStr.toIntOrNull() ?: 5

        if (host.isEmpty() || user.isEmpty() || remotePath.isEmpty()) {
            throw GradleException("SSH credentials/path are not properly set in local.properties")
        }

        val filenamePrefix = destFilenameBase.removeSuffix(".apk")
        val buildNumber = getOrInitBuildNumber()
        val destFilename = "$filenamePrefix-$buildNumber.apk"

        val apkFile = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile

        if (apkFile.exists()) {
            val safeRemotePath = if (remotePath.endsWith("/")) remotePath else "$remotePath/"
            val fullRemoteDestination = "$safeRemotePath$destFilename"

            println("Copying ${apkFile.name} to $user@$host:$fullRemoteDestination...")

            // SCP the APK to your server
            val scpProcess = ProcessBuilder(
                "scp",
                apkFile.absolutePath,
                "$user@$host:$fullRemoteDestination"
            ).inheritIO().start()

            val scpExitCode = scpProcess.waitFor()
            if (scpExitCode == 0) {
                println("APK successfully copied via SCP as '$destFilename'!")

                // Clean up old builds on the server (keeping the newest 'maxBuilds' matches)
                println("Cleaning up old builds on the server (keeping latest $maxBuilds)...")
                val sshCleanupCommand = """
                    cd '$remotePath' && ls -1 "$filenamePrefix"-*.apk 2>/dev/null | sort -V -r | tail -n +${maxBuilds + 1} | xargs rm -f
                """.trimIndent()

                val sshProcess = ProcessBuilder(
                    "ssh",
                    "$user@$host",
                    sshCleanupCommand
                ).inheritIO().start()

                val sshExitCode = sshProcess.waitFor()
                if (sshExitCode == 0) {
                    println("Successfully cleaned up old builds on the server.")
                } else {
                    println("Warning: SSH server cleanup returned non-zero exit code ($sshExitCode).")
                }

                // Increment the build counter for the next run
                incrementBuildNumber()
            } else {
                throw GradleException("SCP transfer failed with exit code $scpExitCode")
            }
        } else {
            throw GradleException("Release APK file not found at ${apkFile.absolutePath}")
        }
    }
}