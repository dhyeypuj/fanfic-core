plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("kapt") version "1.9.23"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.17.2")

    // Room (Android-compatible)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
