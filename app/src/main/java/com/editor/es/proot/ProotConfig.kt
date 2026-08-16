package com.editor.es.proot

import android.content.Context
import android.os.Process
import java.io.File

object ProotConfig {

    const val RootfsName = "ubuntu"
    const val FakeKernelVersion = "6.2.1-PRoot-Distro"
    const val InstallMarker = ".installed"

    private const val TarballUrlAarch64 =
        "https://github.com/termux/proot-distro/releases/download/v4.0.2/ubuntu-aarch64-pd-v4.0.2.tar.xz"
    private const val TarballSha256Aarch64 =
        "257e71bbbb8f336491f63a1d1927a83584d8b4ff8a7f4fb15392674473b838d2"
    private const val TarballUrlArm =
        "https://github.com/termux/proot-distro/releases/download/v4.0.2/ubuntu-arm-pd-v4.0.2.tar.xz"
    private const val TarballSha256Arm =
        "aa72f2a1bbb9d55e9b6b239d539183990e8ba6b2fcd038f5cb5680e6326b17b6"

    fun rootfsDir(context: Context): File = File(context.filesDir, RootfsName)

    fun isInstalled(context: Context): Boolean {
        val rootfs = rootfsDir(context)
        return File(rootfs, InstallMarker).exists() && File(rootfs, "etc").isDirectory
    }

    fun prootBinary(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath

    fun loaderBinary(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libloader.so").absolutePath

    fun isAvailable(context: Context): Boolean = File(prootBinary(context)).exists()

    private fun isAarch64(context: Context): Boolean =
        context.applicationInfo.nativeLibraryDir.endsWith("arm64")

    fun tarballUrl(context: Context): String =
        if (isAarch64(context)) TarballUrlAarch64 else TarballUrlArm

    fun tarballSha256(context: Context): String =
        if (isAarch64(context)) TarballSha256Aarch64 else TarballSha256Arm

    fun tarballFile(context: Context): File = File(context.cacheDir, "ubuntu-rootfs.tar.xz")

    fun tmpDir(context: Context): File = File(context.cacheDir, "proot-tmp").apply { mkdirs() }

    fun prootArgs(context: Context): Array<String> {
        val rootfs = rootfsDir(context).absolutePath
        return arrayOf(
            "proot",
            "-L",
            "--kernel-release=$FakeKernelVersion",
            "--link2symlink",
            "--kill-on-exit",
            "--rootfs=$rootfs",
            "--root-id",
            "--cwd=/root",
            "--bind=/dev",
            "--bind=/dev/urandom:/dev/random",
            "--bind=/proc",
            "--bind=/proc/self/fd:/dev/fd",
            "--bind=/proc/self/fd/0:/dev/stdin",
            "--bind=/proc/self/fd/1:/dev/stdout",
            "--bind=/proc/self/fd/2:/dev/stderr",
            "--bind=/sys",
            "--bind=$rootfs/proc/.loadavg:/proc/loadavg",
            "--bind=$rootfs/proc/.stat:/proc/stat",
            "--bind=$rootfs/proc/.uptime:/proc/uptime",
            "--bind=$rootfs/proc/.version:/proc/version",
            "--bind=$rootfs/proc/.vmstat:/proc/vmstat",
            "--bind=$rootfs/proc/.sysctl_entry_cap_last_cap:/proc/sys/kernel/cap_last_cap",
            "/usr/bin/env",
            "-i",
            "HOME=/root",
            "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "TMPDIR=/tmp",
            "/bin/sh"
        )
    }

    fun prootEnv(context: Context): Array<String> = arrayOf(
        "TERM=xterm-256color",
        "PROOT_LOADER=${loaderBinary(context)}",
        "PROOT_TMP_DIR=${tmpDir(context).absolutePath}"
    )

    fun registerAndroidUser(context: Context) {
        val rootfs = rootfsDir(context)
        val uid = Process.myUid()
        val gid = Process.myGid()
        val passwd = File(rootfs, "etc/passwd")
        val shadow = File(rootfs, "etc/shadow")
        val userName = "aid_a$uid"
        runCatching {
            if (passwd.exists() && !passwd.readText().contains(userName)) {
                passwd.appendText("$userName:x:$uid:$gid:EditorEs:/:/sbin/nologin\n")
            }
            if (shadow.exists() && !shadow.readText().contains(userName)) {
                shadow.appendText("$userName:*:18446:0:99999:7:::\n")
            }
        }
    }
}
