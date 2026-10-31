package com.tayseer.app.util

import android.content.Context
import android.net.Uri
import com.tayseer.app.data.Student
import java.io.BufferedReader
import java.io.InputStreamReader

object StudentImporter {
    fun parseCsv(context: Context, uri: Uri): List<Student> {
        val students = mutableListOf<Student>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                var isFirstLine = true
                while (line != null) {
                    if (isFirstLine) {
                        isFirstLine = false
                        line = reader.readLine()
                        continue
                    }
                    val tokens = line.split(",")
                    if (tokens.size >= 2) {
                        val name = tokens[0].trim()
                        val section = tokens[1].trim()
                        val parentPhone = if (tokens.size >= 3) tokens[2].trim() else ""
                        if (name.isNotEmpty() && section.isNotEmpty()) {
                            students.add(
                                Student(
                                    name = name,
                                    section = section,
                                    parentPhone = parentPhone
                                )
                            )
                        }
                    }
                    line = reader.readLine()
                }
            }
        }
        return students
    }
}
