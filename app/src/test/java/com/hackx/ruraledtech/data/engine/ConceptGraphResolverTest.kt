package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.entities.ConceptEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConceptGraphResolverTest {

    private val conceptDao: ConceptDao = mockk()
    private val resolver = ConceptGraphResolverImpl(conceptDao)

    @Test
    fun `getConcept returns mapped domain concept when exists`() = runTest {
        coEvery { conceptDao.getById("c_fractions") } returns ConceptEntity(
            conceptId = "c_fractions",
            subject = "Math",
            grade = 6,
            parentConceptId = "c_math_basics",
            name = "Fractions",
        )

        val concept = resolver.getConcept("c_fractions")

        assertThat(concept).isNotNull()
        assertThat(concept?.conceptId).isEqualTo("c_fractions")
        assertThat(concept?.name).isEqualTo("Fractions")
    }

    @Test
    fun `getParentConcept returns parent concept when parentConceptId is present`() = runTest {
        coEvery { conceptDao.getById("c_fractions_addition") } returns ConceptEntity(
            conceptId = "c_fractions_addition",
            subject = "Math",
            grade = 6,
            parentConceptId = "c_fractions_basics",
            name = "Fraction Addition",
        )
        coEvery { conceptDao.getById("c_fractions_basics") } returns ConceptEntity(
            conceptId = "c_fractions_basics",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Fraction Basics",
        )

        val parent = resolver.getParentConcept("c_fractions_addition")

        assertThat(parent).isNotNull()
        assertThat(parent?.conceptId).isEqualTo("c_fractions_basics")
        assertThat(parent?.name).isEqualTo("Fraction Basics")
    }

    @Test
    fun `getRemedialTargetConcept returns parent concept if exists otherwise self`() = runTest {
        coEvery { conceptDao.getById("c_child") } returns ConceptEntity(
            conceptId = "c_child",
            subject = "Math",
            grade = 6,
            parentConceptId = "c_parent",
            name = "Child Concept",
        )
        coEvery { conceptDao.getById("c_parent") } returns ConceptEntity(
            conceptId = "c_parent",
            subject = "Math",
            grade = 6,
            parentConceptId = null,
            name = "Parent Concept",
        )

        val target = resolver.getRemedialTargetConcept("c_child")

        assertThat(target?.conceptId).isEqualTo("c_parent")
    }

    @Test
    fun `unknown concept returns null gracefully`() = runTest {
        coEvery { conceptDao.getById("unknown") } returns null

        val concept = resolver.getConcept("unknown")
        val parent = resolver.getParentConcept("unknown")

        assertThat(concept).isNull()
        assertThat(parent).isNull()
    }
}
