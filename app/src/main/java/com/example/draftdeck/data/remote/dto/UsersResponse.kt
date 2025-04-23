package com.example.draftdeck.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UsersResponse(
    @SerializedName("users")
    val users: List<UserDto> = emptyList(),
    
    @SerializedName("total")
    val total: Int = 0,
    
    @SerializedName("page")
    val page: Int = 0,
    
    @SerializedName("limit")
    val limit: Int = 20
) 