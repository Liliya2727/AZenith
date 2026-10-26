/*
 * Copyright (C) 2026 Rem01Gaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.binder

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

import org.lsposed.hiddenapibypass.HiddenApiBypass

object MainKt {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.contains("--fps")) {
            FpsClient.main(args)
            return
        }
        val writer = if (args.isNotEmpty()) {
            PrintWriter(File(args[0]).bufferedWriter())
        } else {
            PrintWriter(System.out.bufferedWriter())
        }

        try {
            HiddenApiBypass.setHiddenApiExemptions("L")
        } catch (t: Throwable) {
            System.err.println("WARNING: Failed to set hidden API exemptions: ${t.message}")
        }

        val reader = BufferedReader(InputStreamReader(System.`in`))
        var line: String?

        try {
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.isEmpty()) continue

                val parts = currentLine.split("::")
                if (parts.size != 2) {
                    System.err.println("ERROR: Invalid format '$currentLine'. Use Class::TRANSACTION_name")
                    continue
                }

                val className = sanitizeClassName(parts[0])
                val fieldName = parts[1]

                try {
                    val code = resolveTransactionCode(className, fieldName)
                    writer.println("$currentLine $code")
                    writer.flush()
                } catch (t: Throwable) {
                    System.err.println("ERROR: Failed to resolve $currentLine -> ${t.message}")
                }
            }
        } finally {
            writer.close()
            reader.close()
        }
    }

    /**
     * Resolves the integer value of a static field within a specified class.
     *
     * @param className The fully qualified class name.
     * @param fieldName The name of the static field to resolve.
     * @return The integer value of the field.
     */
    private fun resolveTransactionCode(className: String, fieldName: String): Int {
        val clazz = Class.forName(className)
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.getInt(null)
    }

    /**
     * Normalizes inner class notation by replacing dot-separators with dollar signs where necessary.
     *
     * @param className The raw class name string from input.
     * @return The sanitized class name suitable for reflection lookup.
     */
    private fun sanitizeClassName(className: String): String {
        if (className.contains(".Stub")) {
            return className.replace(".Stub", "\$Stub")
        }
        return className
    }
}
