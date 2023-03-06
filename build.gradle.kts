plugins {
    kotlin("jvm") version "1.8.10"
}

allprojects {
    group = "com.yuzer"
    version = "7.7.1"

//    apply plugin: 'java'
//    apply plugin: 'groovy'

    repositories {
        mavenCentral()
    }

//    test {
//        testLogging {
//            events "passed", "skipped", "failed"
//            exceptionFormat = 'full'
//        }
//    }
}
