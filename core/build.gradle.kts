import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")

    `java-library`
    `maven-publish`
}

apply {
    plugin("java")
    plugin("groovy")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.21")
    implementation("commons-io:commons-io:2.5")
    implementation("org.rauschig:jarchivelib:1.0.0")
    implementation("org.apache.commons:commons-lang3:3.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.6.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.6.2")

    testImplementation("org.codehaus.groovy:groovy:2.4.6")
    testImplementation("org.spockframework:spock-core:1.0-groovy-2.4")
    testImplementation("ch.qos.logback:logback-classic:1.1.7")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "Artifactory"
            url = uri("https://yuzer.jfrog.io/artifactory/yuzer-mvn/")
            credentials {
                username = "yuzer-push"
                password = project.findProperty("gpr.key") as String? ?: System.getenv("ARTIFACTORY_PWD")
            }
        }
    }
}
