plugins {
    id("org.openrewrite.build.root") version ("latest.release")
    id("org.openrewrite.build.java-base") version ("latest.release")
    id("org.owasp.dependencycheck") version ("latest.release")
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    analyzers.assemblyEnabled = false
    analyzers.nodeAuditEnabled = false
    analyzers.nodeEnabled = false
    failBuildOnCVSS = System.getenv("FAIL_BUILD_ON_CVSS")?.toFloatOrNull() ?: 9.0F
    format = System.getenv("DEPENDENCY_CHECK_FORMAT") ?: "HTML"
    nvd.apiKey = System.getenv("NVD_API_KEY")
    suppressionFile = "suppressions.xml"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."


//    repositories {
//
//        maven {
//            name = "c_dev_m2"
//            url = uri("file:///C:/dev/.m2")
//        }
//        maven {
//            url = uri("file:///C:/aa06010/.m2/repository")
//        }
//    }


}

subprojects {
    plugins.withType<JavaLibraryPlugin> {
        apply(plugin = "maven-publish")

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>(
                    "_toMyLocalMavenRepo_"
                ) {
                    from(components["java"])

                    version=project.version.toString() // ensure it remains 1.0-SNAPSHOT

                    // optionally: explicitly name artifct file to avoid timestamping
                    artifactId=project.name
                    println("Publishing version: ${project.version.toString()} -> $artifactId.$version")
                }
            }
            repositories {
                maven {
                    name = "_DevM2"
                    url = uri("file:///C:/dev/.m2")

                }
            }
        }
    }
}

gradle.projectsEvaluated {
    allprojects.forEach { project ->
        println("Repositories for project ${project.name}")
        project.repositories.forEach { repo ->
            when (repo) {
                is org.gradle.api.artifacts.repositories.MavenArtifactRepository -> {
                    println(" - ${repo.name}: ${repo.url}")
                }

            }

        }
    }
}
