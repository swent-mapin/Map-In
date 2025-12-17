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
    compileSdk = 35

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

    // Release keystore properties
    val releaseStoreFile = findSharedKeystoreProperty("RELEASE_STORE_FILE")
    val releaseStorePassword = findSharedKeystoreProperty("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = findSharedKeystoreProperty("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = findSharedKeystoreProperty("RELEASE_KEY_PASSWORD")

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

        create("release") {
            val missingProperty = listOf(
                "RELEASE_STORE_FILE" to releaseStoreFile,
                "RELEASE_STORE_PASSWORD" to releaseStorePassword,
                "RELEASE_KEY_ALIAS" to releaseKeyAlias,
                "RELEASE_KEY_PASSWORD" to releaseKeyPassword
            ).firstOrNull { it.second.isNullOrBlank() }

            check(missingProperty == null) {
                "Missing release keystore property: ${missingProperty?.first}. Add it to android/gradle.properties or your local gradle.properties."
            }

            storeFile = rootProject.file(releaseStoreFile!!)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }


    defaultConfig {
        applicationId = "com.swent.mapin"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Injection de la clé Maps dans le manifeste
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    implementation(libs.suncalc)

    // ------------- Protobuf (fix for Firebase conflict) ------------------
    implementation(libs.protobuf.javalite)
    androidTestImplementation(libs.protobuf.javalite)

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
    implementation(libs.coil.compose)

    // Material Icons Extended - for additional icons
    implementation("androidx.compose.material:material-icons-extended")

    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ------------- AndroidX ------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.navigation.compose)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ------------- Firebase ------------------
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging") // For push notifications
    implementation("com.google.firebase:firebase-functions") // For Cloud Functions
    implementation("com.firebase:geofire-android-common:3.2.0")

    // ------------- Google Maps ------------------
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.maps.utils)
    implementation(libs.play.services.location)

    // ------------- Networking ------------------
    implementation(libs.okhttp)
    implementation(libs.gson)

    // ------------- Google Identity & Auth ------------------
    implementation(libs.google.id)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.browser)

    // ------------- Coroutines ------------------
    implementation(libs.coroutines.play.services)

    // ------------- Room (local cache for saved events) ------------------
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // ------------- DataStore (for settings persistence) ------------------
    implementation(libs.androidx.datastore.preferences)

    // ------------- Biometric Authentication ------------------
    implementation(libs.androidx.biometric)

    // ------------- Media ------------------
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3.standalone)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // ------------- Unit Tests (JVM) ------------------
    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.androidx.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.kotlin)
    testImplementation(libs.test.mockito.inline)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.json)
    testImplementation(libs.mockk)
    testImplementation(libs.test.mockwebserver)

    // ------------- Instrumented Tests (androidTest) ------------------
    androidTestImplementation(libs.androidtest.junit)
    androidTestImplementation(libs.androidtest.espresso.core)
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