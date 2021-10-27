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

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager.*
import com.matt.linphonelibrary.core.CorePreferences
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import org.linphone.core.*

/**
 * Various utility methods for Linphone SDK
 */
class LinphoneUtils {
    companion object {
        private const val RECORDING_DATE_PATTERN = "dd-MM-yyyy-HH-mm-ss"

        fun getDisplayName(address: Address): String {
            return address.displayName ?: address.username ?: ""
        }

        fun getDisplayableAddress(corePreferences: CorePreferences, address: Address?): String {
            if (address == null) return "[null]"
            return if (corePreferences.replaceSipUriByUsername) {
                address.username ?: address.asStringUriOnly()
            } else {
                address.asStringUriOnly()
            }
        }

        fun isLimeAvailable(core: Core): Boolean {
            return core.limeX3DhAvailable() && core.limeX3DhEnabled() &&
                    core.limeX3DhServerUrl != null &&
                    core.defaultAccount?.params?.conferenceFactoryUri != null
        }

        fun isGroupChatAvailable(core: Core): Boolean {
            return core.defaultAccount?.params?.conferenceFactoryUri != null
        }


        fun getRecordingFilePathForAddress(context: Context, address: Address): String {
            val displayName = getDisplayName(address)
            val dateFormat: DateFormat = SimpleDateFormat(
                RECORDING_DATE_PATTERN,
                Locale.getDefault()
            )
            val fileName = "${displayName}_${dateFormat.format(Date())}.mkv"
            return FileUtils.getFileStoragePath(context, fileName).absolutePath
        }

        fun getRecordingDateFromFileName(name: String): Date {
            return SimpleDateFormat(RECORDING_DATE_PATTERN, Locale.getDefault()).parse(name)
        }

        @SuppressLint("MissingPermission")
        fun checkIfNetworkHasLowBandwidth(context: Context): Boolean {
            val connMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    return when (networkInfo.subtype) {
                        NETWORK_TYPE_EDGE, NETWORK_TYPE_GPRS, NETWORK_TYPE_IDEN -> true
                        else -> false
                    }
                }
            }
            // In doubt return false
            return false
        }

        fun isCallLogMissed(callLog: CallLog): Boolean {
            return (callLog.dir == Call.Dir.Incoming &&
                    (callLog.status == Call.Status.Missed ||
                            callLog.status == Call.Status.Aborted ||
                            callLog.status == Call.Status.EarlyAborted))
        }

        fun getChatRoomId(localAddress: String, remoteAddress: String): String {
            return "$localAddress~$remoteAddress"
        }
    }
}
