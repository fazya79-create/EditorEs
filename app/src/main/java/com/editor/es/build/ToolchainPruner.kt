package com.editor.es.build

object ToolchainPruner {

    private const val LlvmPrefix = "toolchains/llvm/prebuilt/linux-arm64/"

    private val DroppedTools = setOf(
        "clang-check",
        "clang-tidy",
        "clang-format",
        "clang-scan-deps",
        "clangd",
        "clang-repl",
        "clang-refactor",
        "clang-apply-replacements",
        "clang-doc",
        "clang-include-fixer",
        "clang-move",
        "clang-query",
        "clang-reorder-fields",
        "llvm-bolt",
        "perf2bolt",
        "merge-fdata",
        "dsymutil",
        "llvm-dwp",
        "llvm-dwarfdump",
        "llvm-cfi-verify",
        "llvm-ml",
        "llvm-profdata",
        "llvm-cov",
        "llvm-ifs",
        "llvm-modextract",
        "llvm-dis",
        "llvm-rc",
        "llvm-windres",
        "llvm-strings",
        "llvm-lib",
        "llvm-dlltool",
        "llvm-mca",
        "llvm-xray",
        "llvm-reduce",
        "llvm-stress",
        "llvm-exegesis",
        "llvm-pdbutil",
        "llvm-remarkutil",
        "llvm-undname",
        "llvm-cvtres",
        "llvm-diff",
        "llvm-jitlink",
        "llvm-gsymutil",
        "llvm-tli-checker",
        "llvm-debuginfod-find",
        "sancov",
        "sanstats",
        "diagtool",
        "hmaptool",
        "c-index-test",
        "obj2yaml",
        "yaml2obj",
        "verify-uselistorder",
        "lld-link",
        "ld64.lld",
        "wasm-ld",
        "lldb",
        "lldb-server",
        "lldb-argdumper",
        "lldb-dap",
        "lldb-vscode",
        "modularize",
        "pp-trace",
        "find-all-symbols",
        "git-clang-format",
        "run-clang-tidy",
        "scan-build",
        "scan-view",
        "analyze-build",
        "intercept-build",
        "bisect_driver.py"
    )

    private val ForeignTargetPrefixes = listOf(
        "armv7a-linux-android",
        "arm-linux-android",
        "i686-linux-android",
        "x86_64-linux-android",
        "riscv64-linux-android"
    )

    private val ForeignRuntimeTokens = listOf(
        "riscv64",
        "x86_64",
        "i686",
        "i386"
    )

    private val ForeignRuntimeDirs = setOf(
        "riscv64",
        "x86_64",
        "i686",
        "i386",
        "arm"
    )

    fun keep(kind: ToolchainKind, relative: String): Boolean = when (kind) {
        ToolchainKind.CMake -> true
        ToolchainKind.Ndk -> keepNdk(relative)
    }

    private fun keepNdk(relative: String): Boolean {
        if (relative.startsWith("shader-tools/")) return false
        if (relative.startsWith("simpleperf/")) return false
        if (!relative.startsWith(LlvmPrefix)) return true

        val rest = relative.removePrefix(LlvmPrefix)
        if (rest.startsWith("python3/")) return keepPython(rest.removePrefix("python3/"))
        if (rest.startsWith("bin/")) return keepBin(rest.removePrefix("bin/"))
        if (rest.startsWith("sysroot/usr/lib/")) return keepSysrootLib(rest.removePrefix("sysroot/usr/lib/"))
        if (rest.startsWith("lib/clang/")) return keepClangLib(rest)
        return true
    }

    private val DroppedPythonDirs = listOf(
        "test",
        "idlelib",
        "tkinter",
        "lib2to3",
        "distutils",
        "ensurepip",
        "pydoc_data",
        "unittest",
        "site-packages",
        "turtledemo",
        "__pycache__"
    )

    private fun keepPython(rest: String): Boolean {
        if (rest.startsWith("lib/python3.11/")) {
            val tail = rest.removePrefix("lib/python3.11/")
            val head = tail.substringBefore('/')
            if (head in DroppedPythonDirs) return false
            if (tail.contains("/__pycache__/")) return false
            return true
        }
        if (rest.startsWith("lib/") && rest.contains("/config-")) return false
        if (rest.contains("config-3.11")) return false
        if (rest.startsWith("share/")) return false
        if (rest.startsWith("include/")) return false
        return true
    }

    private fun keepBin(name: String): Boolean {
        if (name.isEmpty()) return true
        if (name in DroppedTools) return false
        if (ForeignTargetPrefixes.any { name.startsWith(it) }) return false
        return true
    }

    private fun keepSysrootLib(rest: String): Boolean {
        val head = rest.substringBefore('/')
        if (head.contains("-linux-android") && !head.startsWith("aarch64")) return false
        return true
    }

    private fun keepClangLib(rest: String): Boolean {
        val marker = "/lib/linux/"
        val index = rest.indexOf(marker)
        if (index < 0) return true
        val name = rest.substring(index + marker.length)
        if (name.isEmpty()) return true
        val head = name.substringBefore('/')
        if (head == "aarch64") return true
        if (head in ForeignRuntimeDirs) return false
        if (name.contains("aarch64")) return true
        if (ForeignRuntimeTokens.any { name.contains(it) }) return false
        if (name.contains("-arm-android") || name.contains("armhf")) return false
        return true
    }
}
