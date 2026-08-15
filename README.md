# EditorEs

Modern Android code editor app built with Kotlin and Jetpack Compose.

## Stack
- Kotlin 2.3.10 with AGP 9.1.0 built-in Kotlin support
- Jetpack Compose (Material 3, Compose Navigation)
- sora-editor 0.24.6 as a git submodule, compiled from source via includeBuild
- TextMate grammars for C, C++, Kotlin, Java, JSON and Markdown highlighting

## Build
APKs are built by GitHub Actions on every push to main. Download release and debug
APKs from the Actions tab artifacts.

Local build:

git submodule update --init --recursive
./gradlew assembleRelease

## Details
- Application id: com.editor.es
- Min SDK 26, Target SDK 36, Compile SDK 36
- Release builds are minified and resource-shrunk with R8
