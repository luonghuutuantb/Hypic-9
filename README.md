# Project H V32.6

Project H is an independent Android photo editor project. V32.6 is a clean Gradle/build configuration pass based on V32.

## Build configuration

- Android Gradle Plugin 8.6.1
- Kotlin 2.0.21
- Gradle 8.10 in GitHub Actions
- JDK 17
- compileSdk 35
- minSdk 26
- targetSdk 35
- Java and Kotlin JVM target 17

## GitHub Actions

The repository includes `.github/workflows/android.yml`.
It installs JDK 17 and Gradle 8.10, then runs `:app:assembleDebug` and uploads `app-debug.apk` as the `ProjectH-V32.6-debug` artifact.

This project intentionally does not require a Gradle Wrapper for the GitHub workflow because the workflow installs the pinned Gradle version directly.

## Important

This is an independent implementation. It does not contain Hypic source code, proprietary assets, branding, or proprietary models.
