import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "7.0.2"
    id("net.ltgt.errorprone") version "4.1.0"
    id("com.github.spotbugs") version "6.0.26"
}

group = "dev.tlmtech"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        allErrorsAsWarnings.set(false)
        allDisabledChecksAsWarnings.set(false)
        // Promote warnings to errors so CI fails on issues
        option("--enable-all-checks")
        // NullAway configuration
        option("NullAway:AnnotatedPackages", "dev.tlmtech.po4j")
    }
    options.compilerArgs.add("-Werror")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    errorprone("com.uber.nullaway:nullaway:0.12.3")

    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.javadoc {
    options {
        (this as StandardJavadocDocletOptions).apply {
            addBooleanOption("Xdoclint:none", true)
            addStringOption("Xmaxwarns", "1")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("po4j")
                description.set("A Java library for parsing and writing GNU gettext PO files")
                url.set("https://github.com/ThilinaTLM/po4j")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("ThilinaTLM")
                        name.set("Thilina Lakshan")
                        url.set("https://github.com/ThilinaTLM")
                    }
                }

                scm {
                    url.set("https://github.com/ThilinaTLM/po4j")
                    connection.set("scm:git:git://github.com/ThilinaTLM/po4j.git")
                    developerConnection.set("scm:git:ssh://git@github.com/ThilinaTLM/po4j.git")
                }
            }
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.50.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.5.0")
    }

    format("misc") {
        target("*.md", "*.yml", ".github/**/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

spotbugs {
    toolVersion.set("4.8.6")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { required.set(true) }
    reports.create("xml") { required.set(false) }
}
