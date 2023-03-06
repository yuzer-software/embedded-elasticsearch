plugins {
    `java-library`
    `maven-publish`
}

apply {
    plugin("groovy")
}

tasks {
    withType<JavaCompile> {
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

//    withType<KotlinCompile> {
//        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
//    }
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":test-base"))

    testImplementation("org.codehaus.groovy:groovy-all:2.4.15")
    testImplementation("org.elasticsearch:elasticsearch:7.17.0")
    testImplementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.0")
    testImplementation("org.elasticsearch.client:elasticsearch-rest-client:7.17.0")
    testImplementation("org.locationtech.spatial4j:spatial4j:0.6")
    testImplementation("org.apache.logging.log4j:log4j-api:2.6.2")
    testImplementation("org.apache.logging.log4j:log4j-core:2.6.2")
}
