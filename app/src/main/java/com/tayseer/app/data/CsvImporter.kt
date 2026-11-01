package com.tayseer.app.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvImporter {

    fun parseCsv(context: Context, uri: Uri): List<Student> {
        val studentList = mutableListOf<Student>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var isFirstLine = true
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty()) {
                            if (isFirstLine) {
                                isFirstLine = false
                                if (trimmedLine.contains("اسم") || trimmedLine.contains("name", ignoreCase = true)) {
                                    line = reader.readLine()
                                    continue
                                }
                            }
                            
                            val tokens = trimmedLine.split(",", ";", "\t")
                            if (tokens.isNotEmpty()) {
                                val name = tokens.getOrNull(0)?.trim().orEmpty()
                                val section = tokens.getOrNull(1)?.trim().orEmpty().ifEmpty { "عام" }
                                val phone = tokens.getOrNull(2)?.trim().orEmpty()

                                if (name.isNotBlank()) {
                                    studentList.add(
                                        Student(
                                            name = name,
                                            section = section,
                                            parentPhone = phone
                                        )
                                    )
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return studentList
    }
}
