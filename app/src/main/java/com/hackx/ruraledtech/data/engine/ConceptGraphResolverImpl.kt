package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.domain.engine.ConceptGraphResolver
import com.hackx.ruraledtech.domain.model.Concept
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ConceptGraphResolver] backed by local Room [ConceptDao].
 */
@Singleton
class ConceptGraphResolverImpl @Inject constructor(
    private val conceptDao: ConceptDao,
) : ConceptGraphResolver {

    override suspend fun getConcept(conceptId: String): Concept? {
        return conceptDao.getById(conceptId)?.toDomain()
    }

    override suspend fun getParentConcept(conceptId: String): Concept? {
        val concept = conceptDao.getById(conceptId) ?: return null
        val parentId = concept.parentConceptId ?: return null
        return conceptDao.getById(parentId)?.toDomain()
    }

    override suspend fun getRemedialTargetConcept(conceptId: String): Concept? {
        return getParentConcept(conceptId) ?: getConcept(conceptId)
    }
}
