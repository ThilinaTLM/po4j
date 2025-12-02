plugins {
    java
    `java-library`
    `maven-publish`
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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
