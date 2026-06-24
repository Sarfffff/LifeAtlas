package com.xiaoyin.lifeatlas.core.map

import android.content.Context
import com.amap.api.maps.MapsInitializer

object AmapPrivacyInitializer {
    fun initialize(context: Context) {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
    }
}
