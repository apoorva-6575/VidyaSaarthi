package com.hackx.ruraledtech.domain.engine

import com.hackx.ruraledtech.domain.model.Concept

/**
 * Resolves concept relationships and prerequisite hierarchies using existing Concept data.
 * Operates deterministically and offline.
 */
interface ConceptGraphResolver {
    suspend fun getConcept(conceptId: String): Concept?
    suspend fun getParentConcept(conceptId: String): Concept?
    suspend fun getRemedialTargetConcept(conceptId: String): Concept?
}
