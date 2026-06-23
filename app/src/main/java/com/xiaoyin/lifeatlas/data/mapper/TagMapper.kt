package com.xiaoyin.lifeatlas.data.mapper

import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.data.entity.TagEntity

fun TagEntity.toModel(): Tag {
    return Tag(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )
}

