plugins {
    kotlin("jvm") version "1.4.21"
}

group = "gg.botlabs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.projectreactor:reactor-core:3.4.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-reactor:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")

}
