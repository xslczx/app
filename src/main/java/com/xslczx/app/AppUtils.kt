package com.xslczx.app

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Service
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.*
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.CompletableDeferred
import java.lang.reflect.Field
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max


@Suppress("DEPRECATION")
@SuppressLint("QueryPermissionsNeeded")
object AppUtils {

    suspend fun loadApps(context: Context, appType: AppType): ArrayList<AppInfo> {
        val allApps = ArrayList<AppInfo>()
        val systemApps = ArrayList<AppInfo>()
        val otherApps = ArrayList<AppInfo>()
        val pckManager = context.packageManager
        val packageInfo = getInstalledPackages(context)
        val instance = Calendar.getInstance()
        instance.add(Calendar.DAY_OF_YEAR, -7)
        val usageStatsList = queryUsageStats(
            context, UsageStatsManager.INTERVAL_DAILY, getZeroClockTimestamp(instance.timeInMillis),
            System.currentTimeMillis()
        )
        packageInfo.let {
            for (info in packageInfo) {
                val usageStats = usageStatsList.firstOrNull { it.packageName == info.packageName }
                var l = usageStats?.lastTimeUsed ?: 0
                l = max(info.lastUpdateTime, l)
                val entry = AppInfo(
                    label = info.applicationInfo.loadLabel(pckManager) as String,
                    packageName = info.packageName,
                    icon = info.applicationInfo.loadIcon(pckManager),
                    isDisable = isDisable(context, info.packageName),
                    firstInstallDate = info.firstInstallTime,
                    lastUpdateDate = info.lastUpdateTime,
                    lastUseDate = l,
                    size = getAllAppTotalSize(context, info.packageName)
                )
                if (isSystemPackage(info.applicationInfo)) {
                    systemApps.add(entry)
                } else {
                    otherApps.add(entry)
                }
            }
        }
        systemApps.sort()
        otherApps.sort()
        allApps.addAll(systemApps)
        allApps.addAll(otherApps)
        return when (appType) {
            AppType.System -> systemApps
            AppType.User -> otherApps
            else -> allApps
        }
    }

    private fun getInstalledPackages(context: Context): List<PackageInfo> {
        val pckManager = context.packageManager
        return pckManager?.getInstalledPackages(PackageManager.GET_META_DATA) ?: emptyList()
    }

    private fun isSystemPackage(info: ApplicationInfo): Boolean {
        return (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /**
     * 判断应用是否被禁用
     *
     * @param context
     * @param packageName
     * @return
     */
    private fun isDisable(context: Context, packageName: String): Boolean {
        val pckManager = context.packageManager
        val flag = pckManager.getApplicationEnabledSetting(packageName)
        if (flag == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            return true
        } else if (flag == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            || flag == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        ) {
            return false
        }
        return false
    }

    private fun getAppIconUri(appInfo: ApplicationInfo): Uri {
        var resUri = Uri.EMPTY
        try {
            appInfo.takeIf { it.icon != 0 }?.let {
                resUri =
                    Uri.parse("android.resource://" + appInfo.packageName + "/" + appInfo.icon)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            resUri = Uri.EMPTY
            e.printStackTrace()
        }
        return resUri
    }

    fun getPackageInfo(manager: PackageManager, packageName: String): PackageInfo? {
        try {
            return manager.getPackageInfo(packageName, 0)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return null
    }

    fun getApplicationInfo(manager: PackageManager, packageName: String): ApplicationInfo? {
        try {
            return manager.getApplicationInfo(packageName, 0)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return null
    }

    fun isAccessUsageStats(context: Context): Boolean {
        val ts = System.currentTimeMillis()
        val usageStatsManager =
            context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager
        val queryUsageStats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, 0, ts)
        if (queryUsageStats == null || queryUsageStats.isEmpty()) {
            return false
        }
        return true
    }

    fun queryUsageStats(
        context: Context,
        intervalType: Int,
        beginTime: Long,
        endTime: Long
    ): List<UsageStats> {
        try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            return manager.queryUsageStats(
                intervalType, beginTime,
                endTime
            )
        } catch (ignored: Exception) {
        }
        return emptyList()
    }

    @SuppressLint("DiscouragedPrivateApi")
    fun getLaunchCount(usageStats: UsageStats): Int {
        try {
            val field: Field = usageStats.javaClass.getDeclaredField("mLaunchCount")
            return field.get(usageStats) as Int
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getZeroClockTimestamp(time: Long): Long {
        return time - (time + TimeZone.getDefault().rawOffset) % (24 * 60 * 60 * 1000)
    }

    fun checkUsageStats(context: Context): Boolean {
        val granted: Boolean
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        granted = if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
        return granted
    }


    suspend fun getAllAppTotalSize(context: Context, pkgName: String): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return getAppTotalSizeBelowO(context, pkgName)
        }
        val storageStatsManager: StorageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE)
                    as StorageStatsManager
        val storageManager: StorageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumes: List<StorageVolume> = storageManager.storageVolumes
        for (item in storageVolumes) {
            val uuidStr = item.uuid
            val uuid: UUID =
                if (uuidStr == null) StorageManager.UUID_DEFAULT else UUID.fromString(uuidStr)
            val uid = getUid(context, pkgName)
            return try {
                val storageStats = storageStatsManager.queryStatsForUid(uuid, uid)
                storageStats.appBytes + storageStats.cacheBytes + storageStats.dataBytes
            } catch (e: Exception) {
                0L
            }
        }
        return 0L
    }

    /**
     * 根据应用包名获取对应uid
     */
    fun getUid(context: Context, pakName: String): Int {
        val pm = context.packageManager
        try {
            val ai = pm.getApplicationInfo(pakName, PackageManager.GET_META_DATA)
            return ai.uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return -1
    }

    suspend fun getAppTotalSizeBelowO(context: Context, pkgName: String): Long {
        val mPackManager: PackageManager = context.packageManager
        val method = PackageManager::class.java.getMethod(
            "getPackageSizeInfo",
            String::class.java,
            IPackageStatsObserver::class.java
        )
        val appSizeL = CompletableDeferred<Long>()
        return try {
            method.invoke(mPackManager, pkgName, object : IPackageStatsObserver.Stub() {
                @Throws(RemoteException::class)
                override fun onGetStatsCompleted(pStats: PackageStats, succeeded: Boolean) {
                    appSizeL.complete(pStats.cacheSize + pStats.dataSize + pStats.codeSize)
                }
            })
            appSizeL.await()
        } catch (e: Exception) {
            appSizeL.cancel()
            0L
        }
    }

    fun size(size: Long): String {
        return if (size / (1024 * 1024 * 1024) > 0) {
            val tmpSize = size.toFloat() / (1024 * 1024 * 1024).toFloat()
            val df = DecimalFormat("#.##")
            "" + df.format(tmpSize).toString() + "GB"
        } else if (size / (1024 * 1024) > 0) {
            val tmpSize = size.toFloat() / (1024 * 1024).toFloat()
            val df = DecimalFormat("#.##")
            "" + df.format(tmpSize).toString() + "MB"
        } else if (size / 1024 > 0) {
            "" + size / 1024 + "KB"
        } else "" + size + "B"
    }
}