plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
    `maven-publish`
}

group = "wueffi"
version = "1.3.7.2"

repositories {
    mavenCentral()

    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

val targetJavaVersion = 21

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)

    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks {
    register<Copy>("copyPlugin") {
        dependsOn(jar)

        from("build/libs/MiniGameCore-1.3.7.2.jar")
        into("run/plugins")
    }

    runServer {
        version.set("1.21")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"

        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    processResources {
        val props = mapOf("version" to version)

        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}