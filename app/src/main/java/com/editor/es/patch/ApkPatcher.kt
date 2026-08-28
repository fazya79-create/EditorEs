package com.editor.es.patch

import android.content.Context
import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

sealed class PatchPhase {
    data object Idle : PatchPhase()
    data object Running : PatchPhase()
    data object Done : PatchPhase()
    data object Cancelled : PatchPhase()
    data class Failed(val message: String) : PatchPhase()
}

class PatchCancelledException : Exception("patch cancelled")

data class PatchLibVariant(val abi: String, val file: File)

data class PatchLib(val name: String, val variants: List<PatchLibVariant>)

object ApkPatcher {

    private const val LoaderBase = "Leditorespatch"
    private const val LibraryAlignment = 16384

    fun outputDir(context: Context): File = File(context.cacheDir, "patch").apply { mkdirs() }

    fun scanLibraries(projectDir: File): List<PatchLib> {
        val buildDir = File(projectDir, "build")
        if (!buildDir.isDirectory) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<PatchLibVariant>>()
        val presets = buildDir.listFiles { file -> file.isDirectory }?.sortedBy { it.name } ?: return emptyList()
        for (preset in presets) {
            val parts = preset.name.split("-")
            val abi = if (parts.size > 3) parts.subList(1, parts.size - 1).joinToString("-") else preset.name
            val libs = preset.listFiles { file -> file.isFile && file.name.startsWith("lib") && file.name.endsWith(".so") } ?: continue
            for (lib in libs) {
                val name = lib.name.removePrefix("lib").removeSuffix(".so")
                grouped.getOrPut(name) { mutableListOf() }.add(PatchLibVariant(abi, lib))
            }
        }
        return grouped.map { (name, variants) -> PatchLib(name, variants.sortedBy { it.abi }) }
    }

    suspend fun patch(
        context: Context,
        apkFile: File,
        lib: PatchLib,
        cancelled: AtomicBoolean,
        onLine: (String) -> Unit,
        onPhase: (PatchPhase) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "patch-work")
        workDir.deleteRecursively()
        workDir.mkdirs()
        val outDir = outputDir(context)
        val unsigned = File(outDir, apkFile.nameWithoutExtension + "-patched-unsigned.apk")
        val signed = File(outDir, apkFile.nameWithoutExtension + "-patched.apk")
        try {
            onPhase(PatchPhase.Running)
            if (!apkFile.isFile) throw IllegalArgumentException("apk file not found: " + apkFile.absolutePath)
            onLine("opening " + apkFile.name)
            val manifestBytes = ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml")
                    ?: throw IllegalArgumentException("AndroidManifest.xml missing")
                zip.getInputStream(entry).use { it.readBytes() }
            }
            if (cancelled.get()) throw PatchCancelledException()
            onLine("parsing manifest")
            val launcher = AxmlParser.launcherActivity(manifestBytes)
            val targetType = "L" + launcher.replace(".", "/") + ";"
            onLine("launcher activity: " + launcher)
            val dexNames = ZipFile(apkFile).use { zip ->
                zip.entries().toList().map { it.name }
                    .filter { it == "classes.dex" || (it.startsWith("classes") && it.endsWith(".dex")) }
                    .sorted()
            }
            val opcodes = Opcodes.getDefault()
            var patchedEntryName: String? = null
            var patchedDexFile: File? = null
            for (dexName in dexNames) {
                if (cancelled.get()) throw PatchCancelledException()
                onLine("scanning " + dexName)
                val extracted = File(workDir, dexName.replace("/", "_"))
                ZipFile(apkFile).use { zip ->
                    val entry = zip.getEntry(dexName) ?: return@use
                    zip.getInputStream(entry).use { input ->
                        extracted.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                val dexFile = DexFileFactory.loadDexFile(extracted, opcodes)
                val targetClass = dexFile.classes.firstOrNull { it.type == targetType }
                if (targetClass == null) {
                    onLine("no " + launcher + " in " + dexName + ", skipping")
                    continue
                }
                onLine("target class found in " + dexName)
                val onCreate = targetClass.methods.firstOrNull {
                    it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;") && it.returnType == "V"
                } ?: throw IllegalArgumentException("onCreate(Landroid/os/Bundle;)V not found in " + launcher)
                val implementation = onCreate.implementation
                    ?: throw IllegalArgumentException("onCreate has no implementation in " + launcher)
                val mutable = MutableMethodImplementation(implementation)
                val loaderType = findLoaderType(dexFile)
                val injectorClass = buildInjector(loaderType, lib.name)
                mutable.addInstruction(
                    0,
                    BuilderInstruction35c(
                        Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                        ImmutableMethodReference(loaderType, "load", emptyList(), "V")
                    )
                )
                val directMethods = targetClass.directMethods.map { method ->
                    if (method === onCreate) patchMethod(method, mutable) else method
                }
                val virtualMethods = targetClass.virtualMethods.map { method ->
                    if (method === onCreate) patchMethod(method, mutable) else method
                }
                val patchedClass = ImmutableClassDef(
                    targetClass.type,
                    targetClass.accessFlags,
                    targetClass.superclass,
                    targetClass.interfaces,
                    targetClass.sourceFile,
                    targetClass.annotations,
                    targetClass.staticFields,
                    targetClass.instanceFields,
                    directMethods,
                    virtualMethods
                )
                val patchedDex = File(workDir, "patched-" + dexName)
                val pool = DexPool(opcodes)
                for (cls in dexFile.classes) {
                    pool.internClass(if (cls.type == targetType) patchedClass else cls)
                }
                pool.internClass(injectorClass)
                FileOutputStream(patchedDex).use { pool.writeTo(it) }
                patchedEntryName = dexName
                patchedDexFile = patchedDex
                onLine("injected loadLibrary(" + lib.name + ") into onCreate")
                break
            }
            val targetDexEntry = patchedEntryName
                ?: throw IllegalArgumentException("target class " + launcher + " not found in any dex")
            val targetDexFile = patchedDexFile ?: throw IllegalStateException("patched dex missing")
            onLine("packing apk")
            val counter = CountingOutputStream(FileOutputStream(unsigned))
            val zipOut = ZipOutputStream(counter)
            zipOut.use {
                ZipFile(apkFile).use { source ->
                    val entries = source.entries().toList()
                    for (entry in entries) {
                        if (cancelled.get()) throw PatchCancelledException()
                        val name = entry.name
                        when {
                            entry.isDirectory -> {
                                zipOut.putNextEntry(ZipEntry(name))
                                zipOut.closeEntry()
                            }
                            name.startsWith("META-INF/") -> Unit
                            name == targetDexEntry -> Unit
                            else -> copyEntry(source, entry, zipOut, counter)
                        }
                    }
                }
                val dexEntry = ZipEntry(targetDexEntry)
                dexEntry.method = ZipEntry.DEFLATED
                zipOut.putNextEntry(dexEntry)
                targetDexFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
                for (variant in lib.variants) {
                    val target = "lib/" + variant.abi + "/lib" + lib.name + ".so"
                    val bytes = variant.file.readBytes()
                    val crc = CRC32().apply { update(bytes) }
                    val nameBytes = target.toByteArray(Charsets.UTF_8)
                    val pad = alignmentPad(counter.count + 30L + nameBytes.size, LibraryAlignment)
                    val libEntry = ZipEntry(target).apply {
                        method = ZipEntry.STORED
                        size = bytes.size.toLong()
                        compressedSize = bytes.size.toLong()
                        this.crc = crc.value
                        extra = pad
                    }
                    zipOut.putNextEntry(libEntry)
                    zipOut.write(bytes)
                    zipOut.closeEntry()
                    onLine("added " + target)
                }
            }
            onLine("signing v1 v2 v3")
            val signerEntry = PatcherKeystore.entry(context)
            val signerCertificate = signerEntry.certificate as X509Certificate
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "EditorEs Patcher",
                signerEntry.privateKey,
                listOf(signerCertificate)
            ).build()
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsigned)
                .setOutputApk(signed)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign()
            onLine("done: " + signed.name)
            onPhase(PatchPhase.Done)
            signed
        } catch (cancelledError: PatchCancelledException) {
            onLine("patch cancelled")
            onPhase(PatchPhase.Cancelled)
            null
        } catch (error: Exception) {
            onLine("failed: " + (error.message ?: error.javaClass.simpleName))
            onPhase(PatchPhase.Failed(error.message ?: "patch failed"))
            null
        } finally {
            workDir.deleteRecursively()
            unsigned.delete()
        }
    }

    private fun findLoaderType(dexFile: DexFile): String {
        val existing = dexFile.classes.map { it.type }.toHashSet()
        var index = 0
        while (true) {
            val candidate = LoaderBase + index + ";"
            if (candidate !in existing) return candidate
            index++
        }
    }

    private fun buildInjector(loaderType: String, libName: String): ClassDef {
        val implementation = ImmutableMethodImplementation(
            1,
            listOf(
                BuilderInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(libName)),
                BuilderInstruction35c(
                    Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                    ImmutableMethodReference("Ljava/lang/System;", "loadLibrary", listOf("Ljava/lang/String;"), "V")
                ),
                BuilderInstruction10x(Opcode.RETURN_VOID)
            ),
            emptyList(),
            emptyList()
        )
        val method = ImmutableMethod(loaderType, "load", emptyList(), "V", 0x9, emptySet(), implementation)
        return ImmutableClassDef(
            loaderType,
            0x11,
            "Ljava/lang/Object;",
            emptyList(),
            null,
            emptySet(),
            emptyList(),
            emptyList(),
            listOf(method),
            emptyList()
        )
    }

    private fun patchMethod(method: Method, mutable: MutableMethodImplementation): ImmutableMethod =
        ImmutableMethod(
            method.definingClass,
            method.name,
            method.parameters,
            method.returnType,
            method.accessFlags,
            method.annotations,
            ImmutableMethodImplementation(
                mutable.registerCount,
                mutable.instructions,
                mutable.tryBlocks,
                mutable.debugItems.toList()
            )
        )

    private fun copyEntry(source: ZipFile, entry: ZipEntry, output: ZipOutputStream, counter: CountingOutputStream) {
        val name = entry.name
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        if (entry.method == ZipEntry.STORED) {
            val crc = CRC32()
            var size = 0L
            source.getInputStream(entry).use { input ->
                val buffer = ByteArray(65536)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    crc.update(buffer, 0, read)
                    size += read
                }
            }
            val stored = ZipEntry(name).apply {
                method = ZipEntry.STORED
                this.size = size
                compressedSize = size
                this.crc = crc.value
                extra = alignmentPad(counter.count + 30L + nameBytes.size, 4)
            }
            output.putNextEntry(stored)
            source.getInputStream(entry).use { input -> input.copyTo(output) }
            output.closeEntry()
        } else {
            val deflated = ZipEntry(name)
            output.putNextEntry(deflated)
            source.getInputStream(entry).use { input -> input.copyTo(output) }
            output.closeEntry()
        }
    }

    private fun alignmentPad(base: Long, alignment: Int): ByteArray {
        val remainder = (base % alignment).toInt()
        var pad = (alignment - remainder) % alignment
        if (pad in 1..3) pad += alignment
        if (pad == 0) return ByteArray(0)
        val bytes = ByteArray(pad)
        if (pad >= 4) {
            bytes[0] = 0x35
            bytes[1] = 0xD9.toByte()
            bytes[2] = ((pad - 4) and 0xFF).toByte()
            bytes[3] = (((pad - 4) shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}

private class CountingOutputStream(output: OutputStream) : OutputStream() {

    private val target = output

    var count = 0L
        private set

    override fun write(value: Int) {
        target.write(value)
        count++
    }

    override fun write(buffer: ByteArray) {
        target.write(buffer)
        count += buffer.size
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        target.write(buffer, offset, length)
        count += length
    }

    override fun flush() {
        target.flush()
    }

    override fun close() {
        target.close()
    }
}


