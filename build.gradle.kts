import java.net.URI

plugins {
    kotlin("jvm") version "1.4.21"
    id("maven")
}

group = "gg.botlabs"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {
    api(kotlin("stdlib"))
    api("io.projectreactor:reactor-core:3.4.1")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.1")
    api("com.github.kittinunf.fuel:fuel:2.3.1")
    api("com.github.kittinunf.fuel:fuel-reactor:2.3.1")
    api("org.json:json:20201115")
    testCompile(platform("org.junit:junit-bom:5.7.0"))
    testCompile("org.junit.jupiter:junit-jupiter")
    testCompile("com.github.KennethWussmann:mock-fuel:1.3.0")
    testCompile("io.projectreactor:reactor-test:3.4.1")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
