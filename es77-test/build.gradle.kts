plugins {
    `java-library`
}

apply {
    plugin("groovy")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    withType<GroovyCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":test-base"))

    testImplementation("org.apache.groovy:groovy-all:4.0.21")

    testImplementation("org.elasticsearch:elasticsearch:7.7.0")
    testImplementation("org.elasticsearch.client:elasticsearch-rest-client:7.7.0")
    testImplementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.7.0")
    testImplementation("org.apache.logging.log4j:log4j-api:2.17.1")
    testImplementation("org.apache.logging.log4j:log4j-core:2.17.1")
}
