import java.net.URI

plugins {
    kotlin("jvm") version "1.9.23"
    id("maven-publish")
}

group = "gg.botlabs"
version = "2.0"

repositories {
    mavenCentral()
    jcenter()
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {
    api(kotlin("stdlib"))
    api("io.projectreactor:reactor-core:3.6.5")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.1")
    implementation("org.springframework:spring-webflux:6.1.6")
    implementation("org.json:json:20201115")
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.projectreactor:reactor-test:3.4.1")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}



publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}


