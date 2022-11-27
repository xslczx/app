package com.xslczx.app.ui.main

import android.content.Context
import androidx.lifecycle.*
import com.xslczx.app.AppInfo
import com.xslczx.app.AppType
import com.xslczx.app.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    private val _apps = MutableLiveData<MutableList<AppInfo>>()
    val apps: LiveData<MutableList<AppInfo>> = _apps

    fun setIndex(index: Int,context: Context) {
        _index.value = index
        loadApps(context)
    }

    fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appType = if (_index.value == 1) AppType.User else AppType.System
            val loadApps = AppUtils.loadApps(context, appType = appType)
            withContext(Dispatchers.Main) {
                _apps.value = loadApps
            }
        }
    }
}