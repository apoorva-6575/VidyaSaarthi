package com.hackx.ruraledtech.domain.model

data class Concept(
    val conceptId: String,
    val subject: String,
    val grade: Int,
    val parentConceptId: String?,
    val name: String,
)
