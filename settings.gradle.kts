val snapshotVersion : String? = System.getenv("COMPOSE_SNAPSHOT_ID")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        if(snapshotVersion != null) {
            println("Overriding default snapshot version: https://androidx.dev/snapshots/builds/$snapshotVersion/artifacts/repository/")
            maven { url = uri("https://androidx.dev/snapshots/builds/$snapshotVersion/artifacts/repository/") }
        } else {
            maven { url = uri("https://androidx.dev/snapshots/builds/11621196/artifacts/repository/") }
        }
    }
}
rootProject.name = "snippets"
include(
        ":shared",
        ":bluetoothle",
        ":compose:recomposehighlighter",
        ":kotlin",
        ":compose:snippets",
        ":camera"
)
