plugins {
    kotlin("jvm") version "1.4.21"
}

group = "gg.botlabs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api("io.projectreactor:reactor-core:3.4.1")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.1")
    api("com.github.kittinunf.fuel:fuel:2.3.1")
    api("com.github.kittinunf.fuel:fuel-reactor:2.3.1")
    api("org.json:json:20201115")
}
