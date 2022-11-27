package com.xslczx.app

import android.content.Context
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isDisable: Boolean = false,
    val firstInstallDate: Long = 0L,
    val lastUpdateDate: Long = 0L,
    val lastUseDate: Long = 0L,
    val permissions: List<String>? = null,
    val versionCode: Long = 0L,
    val versionName: String? = null,
    val size: Long = 0L
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int {
        return other.lastUseDate.compareTo(lastUseDate)
    }

    fun isPackageInstalled(context: Context): Boolean {
        return AppUtils.getApplicationInfo(context.packageManager, packageName) != null
    }
}

sealed class AppType {
    object All : AppType()
    object System : AppType()
    object User : AppType()
}
