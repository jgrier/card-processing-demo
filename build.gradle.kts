plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("dev.restate:sdk-api-gen:2.6.0")

    implementation("dev.restate:sdk-java-http:2.6.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.restate.demo.App")
}

tasks.shadowJar {
    archiveBaseName.set("card-processing-demo")
    archiveClassifier.set("all")
    archiveVersion.set("")
    mergeServiceFiles()
}
