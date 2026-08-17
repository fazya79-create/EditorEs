package com.editor.es.data

import java.io.File

enum class ProjectLanguage(val label: String) {
    Kotlin("Kotlin"),
    Java("Java")
}

data class AndroidProjectRequest(
    val folderName: String,
    val appName: String,
    val packageName: String,
    val language: ProjectLanguage,
    val compileSdk: Int,
    val minSdk: Int,
    val withNative: Boolean
)

object AndroidProjectCreator {

    const val AgpVersion = "8.13.0"
    const val GradleVersion = "8.14.3"
    const val KotlinVersion = "2.0.21"
    const val NdkVersion = "27.0.12077973"

    private val folderPattern = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{0,39}$")
    private val segmentPattern = Regex("^[a-z][a-z0-9_]*$")

    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
        "null", "in", "is", "object", "fun", "val", "var", "when", "typealias"
    )

    fun derivePackage(appName: String): String {
        val slug = appName.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { "example" }
        val safe = if (slug.first().isDigit() || slug in javaKeywords) "app$slug" else slug
        return "com.$safe.app"
    }

    fun validatePackage(packageName: String): String? {
        val segments = packageName.split('.')
        if (segments.size < 2) return "Package name needs at least two segments"
        for (segment in segments) {
            if (!segmentPattern.matches(segment)) {
                return "Invalid package segment: $segment"
            }
            if (segment in javaKeywords) return "Reserved word in package: $segment"
        }
        return null
    }

    fun create(request: AndroidProjectRequest): Result<String> = runCatching {
        val folder = request.folderName.trim()
        require(folderPattern.matches(folder)) {
            "Folder name may only contain letters, numbers, - and _"
        }
        require(request.appName.trim().isNotEmpty()) { "App name cannot be empty" }
        validatePackage(request.packageName)?.let { throw IllegalArgumentException(it) }
        require(request.minSdk <= request.compileSdk) {
            "Minimum SDK cannot be higher than the target SDK"
        }

        val root = File(ProjectCreator.baseDir(), folder)
        require(!root.exists()) { "A folder named $folder already exists" }
        require(root.mkdirs()) { "Unable to create the project folder" }

        writeRootFiles(root, request)
        writeAppModule(root, request)
        root.absolutePath
    }

    private fun writeRootFiles(root: File, request: AndroidProjectRequest) {
        File(root, "settings.gradle.kts").writeText(settingsGradle(request))
        File(root, "build.gradle.kts").writeText(rootBuildGradle())
        File(root, "gradle.properties").writeText(gradleProperties())
        File(root, ".gitignore").writeText(gitignore())
        File(root, "gradle/wrapper").mkdirs()
        File(root, "gradle/wrapper/gradle-wrapper.properties")
            .writeText(wrapperProperties())
    }

    private fun writeAppModule(root: File, request: AndroidProjectRequest) {
        val app = File(root, "app").apply { mkdirs() }
        File(app, "build.gradle.kts").writeText(appBuildGradle(request))
        File(app, "proguard-rules.pro").writeText("-dontwarn java.lang.invoke.**\n")

        val main = File(app, "src/main").apply { mkdirs() }
        File(main, "AndroidManifest.xml").writeText(manifest(request))

        val pkgPath = request.packageName.replace('.', '/')
        val sourceRoot = File(main, "java/$pkgPath")
        sourceRoot.mkdirs()
        when (request.language) {
            ProjectLanguage.Kotlin ->
                File(sourceRoot, "MainActivity.kt").writeText(mainActivityKotlin(request))
            ProjectLanguage.Java ->
                File(sourceRoot, "MainActivity.java").writeText(mainActivityJava(request))
        }

        File(main, "res/layout").mkdirs()
        File(main, "res/layout/activity_main.xml").writeText(activityLayout())
        File(main, "res/values").mkdirs()
        File(main, "res/values/strings.xml").writeText(stringsXml(request))
        File(main, "res/values/themes.xml").writeText(themesXml())

        if (request.withNative) {
            val cpp = File(main, "cpp").apply { mkdirs() }
            File(cpp, "CMakeLists.txt").writeText(nativeCmake())
            File(cpp, "native-lib.cpp").writeText(nativeSource(request))
        }
    }

    private fun settingsGradle(request: AndroidProjectRequest) = buildString {
        appendLine("pluginManagement {")
        appendLine("    repositories {")
        appendLine("        google()")
        appendLine("        mavenCentral()")
        appendLine("        gradlePluginPortal()")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("dependencyResolutionManagement {")
        appendLine("    repositories {")
        appendLine("        google()")
        appendLine("        mavenCentral()")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("rootProject.name = \"${request.appName.trim()}\"")
        appendLine()
        appendLine("include(\":app\")")
    }

    private fun rootBuildGradle() = buildString {
        appendLine("plugins {")
        appendLine("    id(\"com.android.application\") version \"$AgpVersion\" apply false")
        appendLine("    id(\"org.jetbrains.kotlin.android\") version \"$KotlinVersion\" apply false")
        appendLine("}")
    }

    private fun gradleProperties() = buildString {
        appendLine("org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m")
        appendLine("org.gradle.daemon=false")
        appendLine("org.gradle.parallel=false")
        appendLine("org.gradle.caching=true")
        appendLine("android.useAndroidX=true")
        appendLine("android.nonTransitiveRClass=true")
        appendLine("kotlin.code.style=official")
    }

    private fun wrapperProperties() = buildString {
        appendLine("distributionBase=GRADLE_USER_HOME")
        appendLine("distributionPath=wrapper/dists")
        appendLine(
            "distributionUrl=https\\://services.gradle.org/distributions/" +
                "gradle-$GradleVersion-bin.zip"
        )
        appendLine("zipStoreBase=GRADLE_USER_HOME")
        appendLine("zipStorePath=wrapper/dists")
    }

    private fun gitignore() = buildString {
        appendLine("*.iml")
        appendLine(".gradle")
        appendLine("local.properties")
        appendLine(".idea")
        appendLine("build")
        appendLine("captures")
        appendLine(".cxx")
    }

    private fun appBuildGradle(request: AndroidProjectRequest) = buildString {
        appendLine("plugins {")
        appendLine("    id(\"com.android.application\")")
        if (request.language == ProjectLanguage.Kotlin) {
            appendLine("    id(\"org.jetbrains.kotlin.android\")")
        }
        appendLine("}")
        appendLine()
        appendLine("android {")
        appendLine("    namespace = \"${request.packageName}\"")
        appendLine("    compileSdk = ${request.compileSdk}")
        if (request.withNative) {
            appendLine("    ndkVersion = \"$NdkVersion\"")
        }
        appendLine()
        appendLine("    defaultConfig {")
        appendLine("        applicationId = \"${request.packageName}\"")
        appendLine("        minSdk = ${request.minSdk}")
        appendLine("        targetSdk = ${request.compileSdk}")
        appendLine("        versionCode = 1")
        appendLine("        versionName = \"1.0\"")
        if (request.withNative) {
            appendLine("        ndk {")
            appendLine("            abiFilters += listOf(\"arm64-v8a\")")
            appendLine("        }")
        }
        appendLine("    }")
        appendLine()
        appendLine("    signingConfigs {")
        appendLine("        create(\"editores\") {")
        appendLine("            val storePath = findProperty(\"EDITORES_STORE_FILE\") as String?")
        appendLine("            if (storePath != null) {")
        appendLine("                storeFile = file(storePath)")
        appendLine("                storePassword = findProperty(\"EDITORES_STORE_PASSWORD\") as String?")
        appendLine("                keyAlias = findProperty(\"EDITORES_KEY_ALIAS\") as String?")
        appendLine("                keyPassword = findProperty(\"EDITORES_KEY_PASSWORD\") as String?")
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
        appendLine()
        appendLine("    buildTypes {")
        appendLine("        release {")
        appendLine("            isMinifyEnabled = false")
        appendLine("            proguardFiles(")
        appendLine("                getDefaultProguardFile(\"proguard-android-optimize.txt\"),")
        appendLine("                \"proguard-rules.pro\"")
        appendLine("            )")
        appendLine("            if (findProperty(\"EDITORES_STORE_FILE\") != null) {")
        appendLine("                signingConfig = signingConfigs.getByName(\"editores\")")
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
        appendLine()
        appendLine("    compileOptions {")
        appendLine("        sourceCompatibility = JavaVersion.VERSION_17")
        appendLine("        targetCompatibility = JavaVersion.VERSION_17")
        appendLine("    }")
        if (request.language == ProjectLanguage.Kotlin) {
            appendLine()
            appendLine("    kotlinOptions {")
            appendLine("        jvmTarget = \"17\"")
            appendLine("    }")
        }
        if (request.withNative) {
            appendLine()
            appendLine("    externalNativeBuild {")
            appendLine("        cmake {")
            appendLine("            path = file(\"src/main/cpp/CMakeLists.txt\")")
            appendLine("            version = \"3.22.1\"")
            appendLine("        }")
            appendLine("    }")
        }
        appendLine("}")
        appendLine()
        appendLine("dependencies {")
        appendLine("    implementation(\"androidx.appcompat:appcompat:1.7.0\")")
        appendLine("    implementation(\"com.google.android.material:material:1.12.0\")")
        appendLine("}")
    }

    private fun manifest(request: AndroidProjectRequest) = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">")
        appendLine()
        appendLine("    <application")
        appendLine("        android:allowBackup=\"true\"")
        appendLine("        android:label=\"@string/app_name\"")
        appendLine("        android:supportsRtl=\"true\"")
        appendLine("        android:theme=\"@style/Theme.App\">")
        appendLine()
        appendLine("        <activity")
        appendLine("            android:name=\".MainActivity\"")
        appendLine("            android:exported=\"true\">")
        appendLine("            <intent-filter>")
        appendLine("                <action android:name=\"android.intent.action.MAIN\" />")
        appendLine("                <category android:name=\"android.intent.category.LAUNCHER\" />")
        appendLine("            </intent-filter>")
        appendLine("        </activity>")
        appendLine()
        appendLine("    </application>")
        appendLine()
        appendLine("</manifest>")
    }

    private fun mainActivityKotlin(request: AndroidProjectRequest) = buildString {
        appendLine("package ${request.packageName}")
        appendLine()
        appendLine("import android.os.Bundle")
        appendLine("import android.widget.TextView")
        appendLine("import androidx.appcompat.app.AppCompatActivity")
        appendLine()
        appendLine("class MainActivity : AppCompatActivity() {")
        appendLine()
        if (request.withNative) {
            appendLine("    companion object {")
            appendLine("        init {")
            appendLine("            System.loadLibrary(\"native-lib\")")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            appendLine("    private external fun nativeGreeting(): String")
            appendLine()
        }
        appendLine("    override fun onCreate(savedInstanceState: Bundle?) {")
        appendLine("        super.onCreate(savedInstanceState)")
        appendLine("        setContentView(R.layout.activity_main)")
        if (request.withNative) {
            appendLine("        findViewById<TextView>(R.id.message).text = nativeGreeting()")
        } else {
            appendLine("        findViewById<TextView>(R.id.message).text = \"Hello from EditorEs\"")
        }
        appendLine("    }")
        appendLine("}")
    }

    private fun mainActivityJava(request: AndroidProjectRequest) = buildString {
        appendLine("package ${request.packageName};")
        appendLine()
        appendLine("import android.os.Bundle;")
        appendLine("import android.widget.TextView;")
        appendLine("import androidx.appcompat.app.AppCompatActivity;")
        appendLine()
        appendLine("public class MainActivity extends AppCompatActivity {")
        appendLine()
        if (request.withNative) {
            appendLine("    static {")
            appendLine("        System.loadLibrary(\"native-lib\");")
            appendLine("    }")
            appendLine()
            appendLine("    public native String nativeGreeting();")
            appendLine()
        }
        appendLine("    @Override")
        appendLine("    protected void onCreate(Bundle savedInstanceState) {")
        appendLine("        super.onCreate(savedInstanceState);")
        appendLine("        setContentView(R.layout.activity_main);")
        appendLine("        TextView message = findViewById(R.id.message);")
        if (request.withNative) {
            appendLine("        message.setText(nativeGreeting());")
        } else {
            appendLine("        message.setText(\"Hello from EditorEs\");")
        }
        appendLine("    }")
        appendLine("}")
    }

    private fun activityLayout() = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"")
        appendLine("    android:layout_width=\"match_parent\"")
        appendLine("    android:layout_height=\"match_parent\">")
        appendLine()
        appendLine("    <TextView")
        appendLine("        android:id=\"@+id/message\"")
        appendLine("        android:layout_width=\"wrap_content\"")
        appendLine("        android:layout_height=\"wrap_content\"")
        appendLine("        android:layout_gravity=\"center\"")
        appendLine("        android:textSize=\"18sp\" />")
        appendLine()
        appendLine("</FrameLayout>")
    }

    private fun stringsXml(request: AndroidProjectRequest) = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<resources>")
        appendLine("    <string name=\"app_name\">${request.appName.trim()}</string>")
        appendLine("</resources>")
    }

    private fun themesXml() = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<resources>")
        appendLine("    <style name=\"Theme.App\" parent=\"Theme.MaterialComponents.DayNight.NoActionBar\" />")
        appendLine("</resources>")
    }

    private fun nativeCmake() = buildString {
        appendLine("cmake_minimum_required(VERSION 3.22.1)")
        appendLine()
        appendLine("project(\"native-lib\")")
        appendLine()
        appendLine("add_library(native-lib SHARED native-lib.cpp)")
        appendLine()
        appendLine("find_library(log-lib log)")
        appendLine()
        appendLine("target_link_libraries(native-lib \${log-lib})")
    }

    private fun nativeSource(request: AndroidProjectRequest): String {
        val jniName = request.packageName.replace("_", "_1").replace('.', '_')
        val suffix = "nativeGreeting"
        return buildString {
            appendLine("#include <jni.h>")
            appendLine("#include <string>")
            appendLine()
            appendLine("extern \"C\" JNIEXPORT jstring JNICALL")
            appendLine("Java_${jniName}_MainActivity_$suffix(JNIEnv *env, jobject) {")
            appendLine("    std::string greeting = \"Hello from C++\";")
            appendLine("    return env->NewStringUTF(greeting.c_str());")
            appendLine("}")
        }
    }
}
