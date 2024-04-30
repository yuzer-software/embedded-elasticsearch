plugins {
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
}

dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.15.2"))

    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("commons-io:commons-io:2.16.1")
    implementation("org.rauschig:jarchivelib:1.2.0") {
        constraints {
            implementation("org.apache.commons:commons-compress") {
                version { require("[1.26.0,)") }
                because("version 1.21 pulled from jarchivelib has vulnerabilities.")
            }
        }
    }
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    testImplementation("org.apache.groovy:groovy:4.0.21")
    testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "embedded-elasticsearch"
            pom {
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "ElasticMavenRepository"
            url = uri("https://maven.pkg.github.com/yuzer-software/embedded-elasticsearch")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("YUZER_MAVEN_REPOSITORY_USERNAME") ?: "OWNER"
                password = project.findProperty("gpr.key") as String? ?: System.getenv("YUZER_MAVEN_REPOSITORY_PASSWORD")
            }
        }
    }
}
