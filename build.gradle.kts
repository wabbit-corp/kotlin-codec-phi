import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

group   = "one.wabbit"
version = "1.0.0"

plugins {
    kotlin("jvm") version "2.0.20"

    kotlin("plugin.serialization") version "2.0.20"

    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "one.wabbit"
            artifactId = "kotlin-codec-phi"
            version = "1.0.0"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.github.wabbit-corp:kotlin-java-escape:1.0.1")
    testImplementation("com.github.wabbit-corp:kotlin-random:1.0.0")
    testImplementation("com.github.wabbit-corp:kotlin-random-gen:1.1.0")

    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")

    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.13")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
}

java {
    targetCompatibility = JavaVersion.toVersion(21)
    sourceCompatibility = JavaVersion.toVersion(21)
}

tasks {
    withType<Test> {
        jvmArgs("-ea")

    }
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
    }
    withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    jar {
        setProperty("zip64", true)

    }
}