package com.editor.es.data

import android.os.Environment
import java.io.File

enum class BuildSystem(val label: String) {
    CMake("CMake"),
    Ndk("NDK")
}

enum class StarterFile(val fileName: String) {
    Main("main.cpp"),
    NativeLib("native-lib.cpp")
}

object ProjectCreator {

    private val namePattern = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{0,39}$")

    fun baseDir(): File = File(Environment.getExternalStorageDirectory(), "EditorEs")

    fun create(
        folderName: String,
        buildSystem: BuildSystem,
        libraryName: String,
        starterFile: StarterFile
    ): Result<String> = runCatching {
        val folder = folderName.trim()
        val library = libraryName.trim()
        require(namePattern.matches(folder)) { "Folder name may only contain letters, numbers, - and _" }
        require(namePattern.matches(library)) { "Library name may only contain letters, numbers, - and _" }
        val projectDir = File(baseDir(), folder)
        require(!projectDir.exists()) { "A folder named $folder already exists" }
        require(projectDir.mkdirs()) { "Unable to create the project folder" }
        File(projectDir, starterFile.fileName).writeText(starterTemplate(starterFile))
        when (buildSystem) {
            BuildSystem.CMake -> File(projectDir, "CMakeLists.txt")
                .writeText(cmakeTemplate(library, starterFile.fileName))
            BuildSystem.Ndk -> File(projectDir, "Android.mk")
                .writeText(ndkTemplate(library, starterFile.fileName))
        }
        projectDir.absolutePath
    }

    private fun starterTemplate(starterFile: StarterFile): String = when (starterFile) {
        StarterFile.Main -> mainCppTemplate
        StarterFile.NativeLib -> nativeLibCppTemplate
    }

    private fun cmakeTemplate(library: String, source: String): String = buildString {
        appendLine("cmake_minimum_required(VERSION 3.22.1)")
        appendLine()
        appendLine("project(\"$library\")")
        appendLine()
        appendLine("add_library($library SHARED $source)")
        appendLine()
        appendLine("find_library(log-lib log)")
        appendLine()
        appendLine("target_link_libraries($library \${log-lib})")
    }

    private fun ndkTemplate(library: String, source: String): String = buildString {
        appendLine("LOCAL_PATH := \$(call my-dir)")
        appendLine()
        appendLine("include \$(CLEAR_VARS)")
        appendLine()
        appendLine("LOCAL_MODULE := $library")
        appendLine("LOCAL_SRC_FILES := $source")
        appendLine("LOCAL_LDLIBS := -llog")
        appendLine()
        appendLine("include \$(BUILD_SHARED_LIBRARY)")
    }

    private val mainCppTemplate = buildString {
        appendLine("#include <iostream>")
        appendLine()
        appendLine("int main(int argc, char **argv) {")
        appendLine("    std::cout << \"Hello from EditorEs\" << std::endl;")
        appendLine("    return 0;")
        appendLine("}")
    }

    private val nativeLibCppTemplate = buildString {
        appendLine("#include <jni.h>")
        appendLine("#include <string>")
        appendLine()
        appendLine("extern \"C\" JNIEXPORT jstring JNICALL")
        appendLine("Java_com_editor_es_MainActivity_stringFromJNI(JNIEnv *env, jobject) {")
        appendLine("    std::string message = \"Hello from EditorEs\";")
        appendLine("    return env->NewStringUTF(message.c_str());")
        appendLine("}")
    }
}
