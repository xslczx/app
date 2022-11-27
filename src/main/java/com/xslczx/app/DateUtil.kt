package com.xslczx.app

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {

    /**
     * 将时间戳转为字符串日期
     * */
    fun timeStamp2DateStr(timeStamp : Long) : String{
        val date = Date(timeStamp)
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val dateStr = simpleDateFormat.format(date)
        return dateStr
    }

    /**
     * 日期字符串(2022-06-16)转换为时间戳
     */
    fun dateStr2timeStamp(dateStr : String) : Long{
        val pattern = "yyyy-MM-dd"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date = simpleDateFormat.parse(dateStr)
        val timeStamp = date.time
        return timeStamp
    }
}