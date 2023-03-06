import org.jetbrains.kotlin.ir.backend.js.compile

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
    implementation(project(":core"))

    api("org.codehaus.groovy:groovy-all:2.4.15")
    api("org.spockframework:spock-core:1.0-groovy-2.4")
    api("ch.qos.logback:logback-classic:1.1.7")
    api("org.skyscreamer:jsonassert:1.3.0")
    api("org.apache.httpcomponents:httpclient:4.5.2")
}