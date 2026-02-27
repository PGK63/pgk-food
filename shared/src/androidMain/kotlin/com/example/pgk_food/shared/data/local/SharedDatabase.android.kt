package com.example.pgk_food.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

private const val DATABASE_FILE_NAME = "pgk_food_database"

private lateinit var appContext: Context

fun initAndroidDatabaseContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    check(::appContext.isInitialized) { "Android database context is not initialized" }
    val databasePath = resolveAndroidDatabasePath(appContext)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = databasePath
    ).fallbackToDestructiveMigration(dropAllTables = true)
}

private fun resolveAndroidDatabasePath(context: Context): String {
    val stablePath = context.getDatabasePath(DATABASE_FILE_NAME).absoluteFile
    if (stablePath.exists()) return stablePath.absolutePath

    locateLegacyDatabase(context, stablePath)?.let { legacy ->
        return legacy.absolutePath
    }

    stablePath.parentFile?.mkdirs()
    return stablePath.absolutePath
}

private fun locateLegacyDatabase(context: Context, stablePath: File): File? {
    val candidates = linkedSetOf(
        File(DATABASE_FILE_NAME).absoluteFile,
        File(context.applicationInfo.dataDir, DATABASE_FILE_NAME).absoluteFile,
        File(context.filesDir, DATABASE_FILE_NAME).absoluteFile,
        File(context.noBackupFilesDir, DATABASE_FILE_NAME).absoluteFile,
    ).toMutableSet().apply {
        context.filesDir.parentFile?.let { add(File(it, DATABASE_FILE_NAME).absoluteFile) }
    }

    return candidates.firstOrNull { candidate ->
        candidate.exists() && candidate.absolutePath != stablePath.absolutePath
    }
}
