package com.tayseer.app.util

import android.content.Context
import android.net.Uri
import com.tayseer.app.data.Student
import java.io.BufferedReader
import java.io.InputStreamReader

object StudentImporter {
    fun importStudents(context: Context, uri: Uri): List<Student> {
        val studentsList = mutableListOf<Student>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine() // قراءة السطر الأول (الترويسة)
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.split(",") ?: continue
                    if (tokens.isNotEmpty()) {
                        val name = tokens.getOrNull(0)?.trim().orEmpty()
                        if (name.isNotBlank()) {
                            val section = tokens.getOrNull(1)?.trim().orEmpty()
                            val phone = tokens.getOrNull(2)?.trim().orEmpty()
                            val parentPhone = tokens.getOrNull(3)?.trim().orEmpty()
                            studentsList.add(
                                Student(
                                    name = name,
                                    section = section,
                                    phone = phone,
                                    parentPhone = parentPhone
                                )
                            )
                        }
                    }
                }
            }
        }
        return studentsList
    }
}
