package com.example

import com.example.data.models.ThreadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AgentToggleUnitTest {

    @Test
    fun testThreadEntityDefaultAgentStates() {
        val thread = ThreadEntity(
            id = "test-thread-1",
            title = "Research and Development"
        )

        // Verify that all agents are enabled by default for seamless workflow orchestration
        assertTrue("Researcher should be enabled by default", thread.researcherEnabled)
        assertTrue("Coder should be enabled by default", thread.coderEnabled)
        assertTrue("Image Generator should be enabled by default", thread.imageGenEnabled)
        assertTrue("Video Generator should be enabled by default", thread.videoGenEnabled)
        assertTrue("Spec Planner should be enabled by default", thread.plannerEnabled)
    }

    @Test
    fun testThreadEntityCustomAgentToggles() {
        // Create a custom orchestrated workflow with specific agents toggled off
        val thread = ThreadEntity(
            id = "test-thread-2",
            title = "Focused Coding Chat",
            researcherEnabled = false,
            coderEnabled = true,
            imageGenEnabled = false,
            videoGenEnabled = false,
            plannerEnabled = true
        )

        // Verify custom configurations are preserved accurately
        assertFalse("Researcher should be disabled in focused coding chat", thread.researcherEnabled)
        assertTrue("Coder should be enabled in focused coding chat", thread.coderEnabled)
        assertFalse("Image Generator should be disabled in focused coding chat", thread.imageGenEnabled)
        assertFalse("Video Generator should be disabled in focused coding chat", thread.videoGenEnabled)
        assertTrue("Spec Planner should be enabled in focused coding chat", thread.plannerEnabled)
    }

    @Test
    fun testThreadEntityCopyAndToggle() {
        val thread = ThreadEntity(
            id = "test-thread-3",
            title = "Creative Workflow"
        )

        // Toggle off Researcher and Coder, leave others enabled
        val customOrchestrated = thread.copy(
            researcherEnabled = false,
            coderEnabled = false
        )

        assertFalse(customOrchestrated.researcherEnabled)
        assertFalse(customOrchestrated.coderEnabled)
        assertTrue(customOrchestrated.imageGenEnabled)
        assertTrue(customOrchestrated.videoGenEnabled)
        assertTrue(customOrchestrated.plannerEnabled)
        assertEquals("Creative Workflow", customOrchestrated.title)
    }
}
