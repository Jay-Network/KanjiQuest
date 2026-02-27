package com.jworks.kanjiquest.android.data

import android.os.Build
import com.jworks.kanjiquest.android.BuildConfig
import com.jworks.kanjiquest.core.data.sync.DeviceInfo

object DeviceInfoProvider {
    fun get(): DeviceInfo = DeviceInfo(
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
        platform = "android",
        appVersion = BuildConfig.VERSION_NAME
    )
}
