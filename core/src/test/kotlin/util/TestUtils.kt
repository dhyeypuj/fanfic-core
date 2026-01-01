package util

import java.io.File

/**
 * Loads a resource file from the samples directory at project root.
 * Path format: "ffn/story.html" or "ao3/ao3_os.html"
 */
fun loadResource(path: String): String {
    // When tests run, working directory can be the module folder (e.g., "core")
    // We need to find the project root which contains "samples" directory
    var currentDir = File("").absoluteFile
    
    // Walk up until we find a directory containing "samples"
    while (currentDir.parentFile != null) {
        val samplesDir = File(currentDir, "samples")
        if (samplesDir.exists() && samplesDir.isDirectory) {
            val resourceFile = File(samplesDir, path)
            if (resourceFile.exists()) {
                return resourceFile.readText()
            }
        }
        currentDir = currentDir.parentFile
    }

    throw IllegalArgumentException("Resource not found: $path (searched from ${File("").absolutePath})")
}

