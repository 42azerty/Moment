package com.example.moments

import java.util.Date

data class Moment(
    val title: String,
    val contents: String,
    val photoPath: String? = null,
    val address: String? = null,
    val date: Date = Date()
)