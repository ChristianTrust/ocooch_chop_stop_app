import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.christian.ocoochchopstopmk2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.christian.ocoochchopstopmk2"
        minSdk = 30
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.compose.foundation:foundation-layout:1.9.4") // Added for USB serial communication
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.3.0")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
//    androidTestImplementation(platform("androidx.compose:compose-bom:2025.07.00"))
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")
//    testImplementation(kotlin("test"))
}

tasks.register("uploadApkToServer") {
    group = "publishing"
    description = "Builds the debug APK and copies it to the self-hosted server over SCP"

    dependsOn("assembleDebug")

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
        val remotePath = properties.getProperty("server.ssh.path") ?: ""
        val destFilename = properties.getProperty("server.ssh.filename") ?: "app-debug.apk"

        if (host.isEmpty() || user.isEmpty() || remotePath.isEmpty()) {
            throw GradleException("SSH credentials/path are not properly set in local.properties")
        }

        val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile

        if (apkFile.exists()) {
            val safeRemotePath = if (remotePath.endsWith("/")) remotePath else "$remotePath/"
            val fullRemoteDestination = "$safeRemotePath$destFilename"

            println("Copying ${apkFile.name} to $user@$host:$fullRemoteDestination...")

            val process = ProcessBuilder(
                "scp",
                apkFile.absolutePath,
                "$user@$host:$fullRemoteDestination"
            ).inheritIO().start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("APK successfully copied via SCP as '$destFilename'!")
            } else {
                throw GradleException("SCP transfer failed with exit code $exitCode")
            }
        } else {
            throw GradleException("APK file not found at ${apkFile.absolutePath}")
        }
    }
}