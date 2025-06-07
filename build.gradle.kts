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
