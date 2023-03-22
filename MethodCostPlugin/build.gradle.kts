plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm")
//    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "com.hoop"
version = "1.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools.build:gradle:4.0.0")
    implementation("com.android.tools.build:gradle-api:7.4.2")
    implementation("org.ow2.asm:asm-util:9.2")
}

gradlePlugin {
    plugins {
        create("MethodCostPlugin") {
            id = "method_cost_plugin"
            implementationClass = "com.example.methodcostplugin.MethodCostPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("${rootProject.projectDir.path}/repos")
        }
    }
    publications {
        create<MavenPublication>("MethodCostPlugin") {
            groupId = "com.hoop"
            artifactId = "method_cost_plugin"
            version = "1.0"

            from(components["java"])
        }
    }
}