import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("jacoco")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.swent.mapin"
    compileSdk = 34

    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) localProps.load(FileInputStream(localPropsFile))
    val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY") ?: ""

    val sharedKeystoreProps = Properties()
    val sharedKeystorePropsFile = rootProject.file("android/gradle.properties")
    if (sharedKeystorePropsFile.exists()) {
        FileInputStream(sharedKeystorePropsFile).use(sharedKeystoreProps::load)
    }

    fun findSharedKeystoreProperty(name: String): String? {
        val fromProject = project.findProperty(name) as? String
        val fromFile = sharedKeystoreProps.getProperty(name)
        return (fromProject ?: fromFile)?.takeIf { it.isNotBlank() }
    }

    val teamDebugStoreFile = findSharedKeystoreProperty("TEAM_DEBUG_STORE_FILE")
    val teamDebugStorePassword = findSharedKeystoreProperty("TEAM_DEBUG_STORE_PASSWORD")
    val teamDebugKeyAlias = findSharedKeystoreProperty("TEAM_DEBUG_KEY_ALIAS")
    val teamDebugKeyPassword = findSharedKeystoreProperty("TEAM_DEBUG_KEY_PASSWORD")

    signingConfigs {
        getByName("debug") {
            val missingProperty = listOf(
                "TEAM_DEBUG_STORE_FILE" to teamDebugStoreFile,
                "TEAM_DEBUG_STORE_PASSWORD" to teamDebugStorePassword,
                "TEAM_DEBUG_KEY_ALIAS" to teamDebugKeyAlias,
                "TEAM_DEBUG_KEY_PASSWORD" to teamDebugKeyPassword
            ).firstOrNull { it.second.isNullOrBlank() }

            check(missingProperty == null) {
                "Missing shared debug keystore property: ${missingProperty?.first}. Add it to android/gradle.properties or your local gradle.properties."
            }

            storeFile = rootProject.file(teamDebugStoreFile!!)
            storePassword = teamDebugStorePassword
            keyAlias = teamDebugKeyAlias
            keyPassword = teamDebugKeyPassword
        }
    }


    defaultConfig {
        applicationId = "com.swent.mapin"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Injection de la clé Maps dans le manifeste
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    configurations.all {
        resolutionStrategy {
            force("com.google.protobuf:protobuf-javalite:3.21.12")
            force("com.google.protobuf:protobuf-java:3.21.12")
        }
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    // Robolectric: déplace src/test vers testDebug
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")
        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }

    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

sonar {
    properties {
        property("sonar.projectKey", "swent-mapin_Map-In")
        property("sonar.projectName", "Map-In")
        property("sonar.organization", "swent-mapin")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugunitTest/")
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}

fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {

    implementation("org.shredzone.commons:commons-suncalc:3.11")

    // ------------- Protobuf (fix for Firebase conflict) ------------------
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    androidTestImplementation("com.google.protobuf:protobuf-javalite:3.21.12")

    // ------------- Mapbox ------------------
    implementation(libs.android.ndk27)
    implementation(libs.maps.compose.ndk27)


    // ------------- Jetpack Compose ------------------
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    globalTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Material Icons Extended - for additional icons
    implementation("androidx.compose.material:material-icons-extended")

    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ------------- AndroidX ------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ------------- Firebase ------------------
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging") // For push notifications

    // ------------- Google Maps ------------------
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ------------- Networking ------------------
    implementation(libs.okhttp)

    // ------------- Google Identity & Auth ------------------
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("androidx.browser:browser:1.7.0")

    // ------------- Coroutines ------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ------------- Room (local cache for saved events) ------------------
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    kapt("androidx.room:room-compiler:2.8.3")

    // ------------- DataStore (for settings persistence) ------------------
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ------------- Biometric Authentication ------------------
    implementation("androidx.biometric:biometric:1.1.0")

    // ------------- Media ------------------
    implementation("androidx.compose.foundation:foundation:1.7.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // ------------- Unit Tests (JVM) ------------------
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("org.json:json:20250517")
    testImplementation(libs.mockk)

    // ------------- Instrumented Tests (androidTest) ------------------
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kaspresso)
    androidTestImplementation(libs.kaspresso.compose)
    testImplementation(kotlin("test"))
}


tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

    reports {
        xml.required = true
        html.required = true
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })
}