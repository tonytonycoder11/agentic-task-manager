import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :domain is a PURE Kotlin/JVM module — there is intentionally NO Android plugin here.
// That guarantees, at compile time, that none of the graph/use-case logic can import
// Android. This is the project's most valuable and most heavily unit-tested code.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        // Target Java 11 bytecode to stay aligned with the rest of the project
        // (and with the appfunctions alpha, which targets Java 11 from alpha07).
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // coroutines-core is a plain JVM library (NOT Android) — it is the only third-party
    // dependency the domain needs, for Flow-based observation exposed through the repository.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
