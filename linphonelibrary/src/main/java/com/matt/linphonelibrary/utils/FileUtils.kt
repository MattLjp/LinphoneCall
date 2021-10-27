/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.matt.linphonelibrary.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.matt.linphonelibrary.R
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

import org.linphone.core.tools.Log

class FileUtils {
    companion object {
        private val TAG = javaClass.simpleName

        const val VFS_PLAIN_FILE_EXTENSION = ".bctbx_evfs_plain"

        fun getNameFromFilePath(filePath: String): String {
            var name = filePath
            val i = filePath.lastIndexOf('/')
            if (i > 0) {
                name = filePath.substring(i + 1)
            }
            return name
        }

        fun getExtensionFromFileName(fileName: String): String {
            val realFileName = if (fileName.endsWith(VFS_PLAIN_FILE_EXTENSION)) {
                fileName.substring(0, fileName.length - VFS_PLAIN_FILE_EXTENSION.length)
            } else fileName

            var extension = MimeTypeMap.getFileExtensionFromUrl(realFileName)
            if (extension.isNullOrEmpty()) {
                val i = realFileName.lastIndexOf('.')
                if (i > 0) {
                    extension = realFileName.substring(i + 1)
                }
            }

            return extension
        }

        fun isPlainTextFile(path: String): Boolean {
            val extension = getExtensionFromFileName(path).toLowerCase(Locale.getDefault())
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return type?.startsWith("text/plain") ?: false
        }

        fun isExtensionPdf(path: String): Boolean {
            val extension = getExtensionFromFileName(path).toLowerCase(Locale.getDefault())
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return type?.startsWith("application/pdf") ?: false
        }

        fun isExtensionImage(path: String): Boolean {
            val extension = getExtensionFromFileName(path).toLowerCase(Locale.getDefault())
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return type?.startsWith("image/") ?: false
        }

        fun isExtensionVideo(path: String): Boolean {
            val extension = getExtensionFromFileName(path).toLowerCase(Locale.getDefault())
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return type?.startsWith("video/") ?: false
        }

        fun isExtensionAudio(path: String): Boolean {
            val extension = getExtensionFromFileName(path).toLowerCase(Locale.getDefault())
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return type?.startsWith("audio/") ?: false
        }

        fun getFileStorageDir(context: Context, isPicture: Boolean = false): File {
            var path: File? = null
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                Log.w("[File Utils] External storage is mounted")
                var directory = Environment.DIRECTORY_DOWNLOADS
                if (isPicture) {
                    Log.w("[File Utils] Using pictures directory instead of downloads")
                    directory = Environment.DIRECTORY_PICTURES
                }
                path = context.getExternalFilesDir(directory)
            }

            val returnPath: File = path ?: context.filesDir
            if (path == null) Log.w("[File Utils] Couldn't get external storage path, using internal")

            return returnPath
        }

        fun getFileStoragePath(context: Context, fileName: String): File {
            val path = getFileStorageDir(context, isExtensionImage(fileName))
            var file = File(path, fileName)

            var prefix = 1
            while (file.exists()) {
                file = File(path, prefix.toString() + "_" + fileName)
                Log.w("[File Utils] File with that name already exists, renamed to ${file.name}")
                prefix += 1
            }
            return file
        }

        fun deleteFile(filePath: String) {
            val file = File(filePath)
            if (file.exists()) {
                try {
                    if (file.delete()) {
                        Log.i("[File Utils] Deleted $filePath")
                    } else {
                        Log.e("[File Utils] Can't delete $filePath")
                    }
                } catch (e: Exception) {
                    Log.e("[File Utils] Can't delete $filePath, exception: $e")
                }
            } else {
                Log.e("[File Utils] File $filePath doesn't exists")
            }
        }


        private fun getNameFromUri(uri: Uri, context: Context): String {
            var name = ""
            if (uri.scheme == "content") {
                val returnCursor =
                    context.contentResolver.query(uri, null, null, null, null)
                if (returnCursor != null) {
                    returnCursor.moveToFirst()
                    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        try {
                            name = returnCursor.getString(nameIndex)
                        } catch (e: CursorIndexOutOfBoundsException) {
                            Log.e("[File Utils] Failed to get the display name for URI $uri, exception is $e")
                        }
                    } else {
                        Log.e("[File Utils] Couldn't get DISPLAY_NAME column index for URI: $uri")
                    }
                    returnCursor.close()
                }
            } else if (uri.scheme == "file") {
                name = uri.lastPathSegment ?: ""
            }
            return name
        }

    }


}
