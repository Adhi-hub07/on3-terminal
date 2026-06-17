pluginManagement {
    repositories {
        maven { url = uri("http://127.0.0.1:48081/google/") }
        maven { url = uri("http://127.0.0.1:48081/central/") }
        maven { url = uri("http://127.0.0.1:48081/plugins/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("http://127.0.0.1:48081/google/") }
        maven { url = uri("http://127.0.0.1:48081/central/") }
    }
}

rootProject.name = "on3-terminal"
include(":app")
include(":terminal-core")
