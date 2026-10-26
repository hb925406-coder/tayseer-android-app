package com.tayseer.app.util

import android.content.Context
import android.net.Uri
import com.tayseer.app.data.Student
import java.io.BufferedReader
import java.io.InputStreamReader

object StudentImporter {

    fun parseCsvOrTxt(context: Context, uri: Uri, levelId: Long): List<Student> {
        val students = mutableListOf<Student>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank()) {
                        val parts = trimmed.split(",", "\t", ";")
                        val name = parts.getOrNull(0)?.trim() ?: ""
                        val phone = parts.getOrNull(1)?.trim() ?: ""
                        val parentPhone = parts.getOrNull(2)?.trim() ?: phone
                        
                        if (name.isNotBlank() && !name.contains("الاسم") && !name.contains("name", ignoreCase = true)) {
                            students.add(
                                Student(
                                    levelId = levelId,
                                    name = name,
                                    phone = phone,
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
