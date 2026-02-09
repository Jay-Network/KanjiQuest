plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.jworks.kanjiquest.pipeline.MainKt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // SQLite JDBC for building the database
    implementation(libs.sqldelight.sqlite.driver)

    // Testing
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(17)
}
