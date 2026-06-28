package org.example.kalkulationsprogramm.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

@Service
class CodebaseIndexService {
    @Value("\${user.dir}")
    private lateinit var projectRoot: String

    @Volatile
    private var cachedIndex: String = ""

    val index: String
        get() = cachedIndex

    @PostConstruct
    fun init() {
        rebuildIndex()
    }

    @Scheduled(fixedDelay = 600_000)
    fun scheduledRebuild() {
        rebuildIndex()
    }

    private fun rebuildIndex() {
        try {
            val root = Paths.get(projectRoot).toAbsolutePath().normalize()
            val sb = StringBuilder()

            sb.append("# === FRONTEND: Seiten (Pages) ===\n\n")
            appendDirectory(sb, root, "react-pc-frontend/src/pages", ".tsx", 3)

            sb.append("\n# === FRONTEND: Komponenten ===\n\n")
            appendDirectory(sb, root, "react-pc-frontend/src/components", ".tsx", 4)
            appendDirectory(sb, root, "react-pc-frontend/src/components", ".ts", 4)

            sb.append("\n# === FRONTEND: Routing & App-Struktur ===\n\n")
            appendSingleFile(sb, root, "react-pc-frontend/src/App.tsx")
            appendSingleFile(sb, root, "react-pc-frontend/src/main.tsx")
            appendSingleFile(sb, root, "react-pc-frontend/src/types.ts")

            sb.append("\n# === FRONTEND: Lib & Utils ===\n\n")
            appendDirectory(sb, root, "react-pc-frontend/src/lib", ".ts", 2)

            sb.append("\n# === ZEITERFASSUNG-APP: Seiten ===\n\n")
            appendDirectory(sb, root, "react-zeiterfassung/src/pages", ".tsx", 3)
            sb.append("\n# === ZEITERFASSUNG-APP: Komponenten & Services ===\n\n")
            appendDirectory(sb, root, "react-zeiterfassung/src/components", ".tsx", 3)
            appendDirectory(sb, root, "react-zeiterfassung/src/services", ".ts", 2)
            appendSingleFile(sb, root, "react-zeiterfassung/src/App.tsx")

            sb.append("\n# === BACKEND: Controller (REST-Endpoints) ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/controller", ".java", 3)

            sb.append("\n# === BACKEND: Services ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/service", ".java", 3)

            sb.append("\n# === BACKEND: Domain-Entities ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/domain", ".java", 3)

            sb.append("\n# === BACKEND: DTOs ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/dto", ".java", 4)

            sb.append("\n# === BACKEND: Mapper ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/mapper", ".java", 2)

            sb.append("\n# === BACKEND: Config & Utils ===\n\n")
            appendDirectory(sb, root, "$JAVA_BASE/config", ".java", 2)
            appendDirectory(sb, root, "$JAVA_BASE/util", ".java", 2)

            sb.append("\n# === Dokumentation ===\n\n")
            appendDirectory(sb, root, "docs", ".md", 3)

            var result = sb.toString()
            if (result.length > MAX_TOTAL_SIZE) {
                result = result.substring(0, MAX_TOTAL_SIZE) +
                    "\n\n[...Index bei $MAX_TOTAL_SIZE Zeichen gekürzt]\n"
            }

            cachedIndex = result
            log.info("KI-Hilfe Codebase-Index erstellt: {} Zeichen", cachedIndex.length)
        } catch (e: Exception) {
            log.error("Fehler beim Erstellen des Codebase-Index", e)
        }
    }

    private fun appendDirectory(sb: StringBuilder, root: Path, relPath: String, extension: String, maxDepth: Int) {
        val dir = root.resolve(relPath)
        if (!dir.isDirectory()) {
            return
        }

        val normalizedDir = dir.toAbsolutePath().normalize()
        try {
            Files.walk(dir, maxDepth).use { files ->
                files
                    .filter { it.isRegularFile() }
                    .filter { it.toString().endsWith(extension) }
                    .filter { it.toAbsolutePath().normalize().startsWith(normalizedDir) }
                    .filter { !it.name.contains("Test") }
                    .sorted()
                    .forEach { appendFileContent(sb, root, it) }
            }
        } catch (_: IOException) {
            log.warn("Konnte Verzeichnis nicht lesen: {}", dir)
        }
    }

    private fun appendSingleFile(sb: StringBuilder, root: Path, relPath: String) {
        val file = root.resolve(relPath)
        if (file.isRegularFile()) {
            appendFileContent(sb, root, file)
        }
    }

    private fun appendFileContent(sb: StringBuilder, root: Path, file: Path) {
        try {
            var content = Files.readString(file, StandardCharsets.UTF_8)
            content = SECRET_PATTERN.matcher(content).replaceAll("$1***REDACTED***")

            if (content.length > MAX_FILE_SIZE) {
                content = content.substring(0, MAX_FILE_SIZE) +
                    "\n// [...Datei gekürzt bei $MAX_FILE_SIZE Zeichen]\n"
            }

            val relativePath = root.relativize(file).toString().replace('\\', '/')
            sb.append("## Datei: ").append(relativePath).append("\n```\n")
            sb.append(content)
            sb.append("\n```\n\n")
        } catch (_: IOException) {
            log.warn("Konnte {} nicht lesen", file)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodebaseIndexService::class.java)
        private const val MAX_FILE_SIZE = 30_000
        private const val MAX_TOTAL_SIZE = 1_500_000
        private const val JAVA_BASE = "src/main/java/org/example/kalkulationsprogramm"
        private val SECRET_PATTERN: Pattern = Pattern.compile(
            "((?:password|passwd|secret|api[._-]?key|token|credentials)\\s*[=:]\\s*)([^\\s,;\"'}{]+)",
            Pattern.CASE_INSENSITIVE,
        )
    }
}
