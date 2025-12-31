package util

import java.io.InputStreamReader

fun loadResource(path: String): String {
    val stream = object {}.javaClass.classLoader
        .getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

    return InputStreamReader(stream).readText()
}
