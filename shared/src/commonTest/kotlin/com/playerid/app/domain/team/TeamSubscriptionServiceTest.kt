package com.playerid.app.domain.team

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamSubscriptionServiceTest {
    @Test
    fun replaceWithTeamClearsExistingSubscriptionsAndAddsNormalizedTeam() = runTest {
        val repository = RecordingSubscriptionRepository()
        val service = TeamSubscriptionService(repository)

        val replaced = service.replaceWithTeam("parent-1", "  Tigers  ", 42)

        assertTrue(replaced)
        assertEquals(listOf("clear:parent-1", "subscribe:parent-1:Tigers:42"), repository.operations)
    }

    @Test
    fun replaceWithTeamRejectsBlankTeamWithoutChangingSubscriptions() = runTest {
        val repository = RecordingSubscriptionRepository()
        val service = TeamSubscriptionService(repository)

        val replaced = service.replaceWithTeam("parent-1", "   ", 42)

        assertFalse(replaced)
        assertTrue(repository.operations.isEmpty())
    }

    private class RecordingSubscriptionRepository : TeamSubscriptionRepository {
        val operations = mutableListOf<String>()

        override fun observeSubscribedTeams(userId: String): Flow<List<TeamProfile>> = flowOf(emptyList())

        override suspend fun subscribe(subscription: TeamSubscription) {
            operations += "subscribe:${subscription.userId}:${subscription.teamName}:${subscription.subscribedAt}"
        }

        override suspend fun clear(userId: String) {
            operations += "clear:$userId"
        }

        override suspend fun unsubscribe(userId: String, teamName: String) = Unit
    }
}