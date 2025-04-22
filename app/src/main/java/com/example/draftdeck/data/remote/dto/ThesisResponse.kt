package com.example.draftdeck.data.remote.dto

/**
 * Data class representing the server response for thesis list endpoint
 */
data class ThesisResponse(
    val theses: List<ThesisDto>,
    val count: Int,
    val limit: Int,
    val offset: Int
) 