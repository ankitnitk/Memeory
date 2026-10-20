plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.memeory.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Room writes a description of the database layout here. We keep these files
// in git so future database changes can be upgraded safely without losing memes.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val room = "2.6.1"
    api("androidx.room:room-runtime:$room")
    api("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    api("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Reads the words printed on memes. This is the "bundled" version: the
    // text-reading model is packed inside the app, so it works offline from
    // the first launch with nothing to download. Adds roughly 4 MB.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    testImplementation("junit:junit:4.13.2")
}
