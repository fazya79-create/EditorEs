# EditorEs

Modern Android code editor app built with Kotlin and Jetpack Compose.

## Stack
- Kotlin 2.0.21
- Jetpack Compose (Material 3, Compose Navigation)
- Planned code editor engine: sora-editor (io.github.rosemoe, Maven Central, BOM 0.24.4)

## Build
APKs are built by GitHub Actions on every push to main. Download release and debug
APKs from the Actions tab artifacts.

Local build:

./gradlew assembleRelease

## Details
- Application id: com.editor.es
- Min SDK 26, Target SDK 35, Compile SDK 35
- Release builds are minified and resource-shrunk with R8
