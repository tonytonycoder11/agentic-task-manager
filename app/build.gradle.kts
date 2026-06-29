import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 ships built-in Kotlin, but it is incompatible with KSP (which we need for Room and
    // Hilt). We opt out via android.builtInKotlin=false (gradle.properties) and apply the standard
    // kotlin-android plugin explicitly, pinning Kotlin to the version the Compose plugin expects.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.github.tonytonycoder11.agentictaskmanager"
    // Compile against API 37 because the current AndroidX libraries require it; the app still
    // runs on Android 16 (API 36) thanks to minSdk/targetSdk 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.tonytonycoder11.agentictaskmanager"
        // AppFunctions only exists on Android 16+ (API 36). We pin minSdk there from the start
        // so the whole project targets the same surface the agent layer will need in Phase 2.
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-phase1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    lint {
        // This is a showcase build; don't fail the whole assemble on a lint warning.
        abortOnError = false
    }
}

ksp {
    // Required by the AppFunctions compiler to aggregate the function metadata into the app module.
    arg("appfunctions:aggregateAppFunctions", "true")
}

// Kotlin JVM target is configured on the top-level kotlin extension (not inside android {}),
// kept in sync with compileOptions above.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // The pure-Kotlin domain: entities, graph logic and use cases.
    implementation(project(":domain"))

    // Coroutines (Android dispatchers).
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX core / lifecycle / activity.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (BOM-managed versions).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room persistence (the dependency graph lives here).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt dependency injection.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Hilt-generated Dagger code references errorprone's @CanIgnoreReturnValue; provide it.
    implementation(libs.error.prone.annotations)

    // AppFunctions: expose use cases as agent-callable tools. The compiler runs via KSP and
    // generates the service + the <ClassName>Ids metadata; no AndroidManifest entry is needed.
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    ksp(libs.androidx.appfunctions.compiler)

    // Tests.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
