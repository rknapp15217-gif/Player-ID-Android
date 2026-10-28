package com.playerid.app.domain.memory

import com.playerid.app.domain.team.CaptureOrganization
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.Test
import kotlin.test.assertEquals

class GuardianShareRecipientsTest {
    private fun player(id: String, team: String = "Hawks", active: Boolean = true) =
        PlayerProfile(id = id, number = "1", name = id, position = "", teamName = team, academicYear = "",
            isActive = active, createdAt = 0, updatedAt = 0)

    private val mom = GuardianShareContact("Mom", phoneNumber = "555-0100", linkedPlayerIds = setOf("a"))
    private val dad = GuardianShareContact("Dad", email = "dad@example.com", linkedPlayerIds = setOf("b"))
    private val coach = GuardianShareContact("Coach", phoneNumber = "555-0101", email = "c@example.com", linkedPlayerIds = setOf("a", "b"))
    private val clip = MediaReference("file:///media/clip.mp4", MediaKind.VIDEO)
    private val other = MediaReference("file:///media/other.mp4", MediaKind.VIDEO)
    private val snapshot = TeamWorkspaceSnapshot(
        players = listOf(player("a"), player("b"), player("c", active = false), player("x", team = "Owls")),
        teamShareContacts = mapOf("Hawks" to listOf(mom, dad, coach)),
        captureOrganizations = mapOf(
            "clip.mp4" to CaptureOrganization("Hawks", playerId = "a"),
            "other.mp4" to CaptureOrganization("Hawks", playerId = "c")
        )
    )

    @Test
    fun suggestionsUseAssignedActivePlayersOnTheShareTeam() {
        assertEquals(listOf("a"), suggestedMemorySharePlayerIds(listOf(clip, other), snapshot, "Hawks"))
        assertEquals(emptyList(), suggestedMemorySharePlayerIds(listOf(clip), snapshot, "Owls"))
    }

    @Test
    fun selectedPlayersResolveToLinkedGuardiansWithoutDuplicates() {
        val recipients = memoryShareRecipients(snapshot, "Hawks", setOf("a"), setOf(coach.key), emptyList())
        assertEquals(listOf(mom, coach), recipients)
    }

    @Test
    fun phoneContactsAreAddedOnceAlongsideFavorites() {
        val picked = GuardianShareContact("Grandma", phoneNumber = "555-0199")
        val recipients = memoryShareRecipients(snapshot, "Hawks", emptySet(), setOf(dad.key), listOf(picked, picked))
        assertEquals(listOf(dad, picked), recipients)
    }

    @Test
    fun phoneRecipientsUseMessagesAndEmailOnlyRecipientsUseMail() {
        val routes = guardianShareRoutes(listOf(mom, dad, coach))
        assertEquals(listOf(mom, coach), routes.messages)
        assertEquals(listOf(dad), routes.mail)
    }
}
