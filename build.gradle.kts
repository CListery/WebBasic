//apply(from = "publishToMaven.gradle")
//apply(from = "publishToProject.gradle")

plugins {
    id("com.clistery.gradle")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
    }
}
