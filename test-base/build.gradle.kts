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
}

dependencies {
    implementation(project(":core"))

    api("org.apache.groovy:groovy-all:4.0.21")
    api("org.spockframework:spock-core:2.3-groovy-4.0")
    api("ch.qos.logback:logback-classic:1.5.6")
    api("org.skyscreamer:jsonassert:1.5.1")
    api("org.apache.httpcomponents.client5:httpclient5:5.3.1")
}
