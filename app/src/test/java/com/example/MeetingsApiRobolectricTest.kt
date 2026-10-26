package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MeetingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeetingsApiRobolectricTest {

    private lateinit var repository: MeetingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = MeetingsRepository(context)
    }

    @Test
    fun testCreateAudioAndVideoMeetings() {
        runBlocking {
            // Scénario 1: Créer une réunion audio
            val audioRes = repository.createMeeting(
                topic = "Réunion Audio Equipe Finuseco",
                type = "audio",
                durationMin = 45,
                priceAmount = 0.0,
                priceCurrency = "USD"
            )
            assertTrue(audioRes.isSuccess)
            val audioMeeting = audioRes.getOrNull()
            assertNotNull(audioMeeting)
            assertEquals("audio", audioMeeting?.type)
            assertNotNull(audioMeeting?.meetCode)
            assertTrue(audioMeeting?.joinUrl?.contains(audioMeeting.meetCode!!) == true)

            // Scénario 2: Créer une réunion vidéo
            val videoRes = repository.createMeeting(
                topic = "Visioconférence Direction",
                type = "video",
                durationMin = 60,
                priceAmount = 10.0,
                priceCurrency = "USD"
            )
            assertTrue(videoRes.isSuccess)
            val videoMeeting = videoRes.getOrNull()
            assertNotNull(videoMeeting)
            assertEquals("video", videoMeeting?.type)
            assertEquals(10.0, videoMeeting?.billing?.priceAmount ?: 0.0, 0.01)
        }
    }

    @Test
    fun testListAndStatusAndJoinMeeting() {
        runBlocking {
            // Scénario 4: Lister réunions récentes
            val listRes = repository.getMeetingsList(20)
            assertTrue(listRes.isSuccess)
            val list = listRes.getOrNull()
            assertNotNull(list)
            assertTrue(list!!.isNotEmpty())

            val sampleCode = list.first().code

            // Scénario 3.3: Statut d'une réunion
            val statusRes = repository.getMeetingStatus(sampleCode)
            assertTrue(statusRes.isSuccess)
            assertEquals(sampleCode, statusRes.getOrNull()?.meetCode)

            // Scénario 5: Rejoindre réunion active
            val joinRes = repository.joinMeeting(sampleCode)
            assertTrue(joinRes.isSuccess)
            assertEquals(sampleCode, joinRes.getOrNull()?.meetCode)
        }
    }

    @Test
    fun testParticipantsAndSpeechManagement() {
        runBlocking {
            val meetCode = "cp-meet-8821"

            // Scénario 7: Consulter les participants
            val partRes = repository.getMeetingParticipants(meetCode)
            assertTrue(partRes.isSuccess)
            val participants = partRes.getOrNull()?.participants
            assertNotNull(participants)
            assertTrue(participants!!.isNotEmpty())

            // Scénario 8: Lever puis baisser la main
            val raiseRes = repository.raiseHand(meetCode, true)
            assertTrue(raiseRes.isSuccess)
            assertTrue(raiseRes.getOrNull() == true)

            val lowerRes = repository.raiseHand(meetCode, false)
            assertTrue(lowerRes.isSuccess)
            assertTrue(lowerRes.getOrNull() == false)

            // Scénario 9: Accorder et retirer droit de parole
            val grantRes = repository.grantSpeech(meetCode, targetUserId = 303L, canSpeak = true)
            assertTrue(grantRes.isSuccess)
            assertEquals("speaker", grantRes.getOrNull()?.role)

            val revokeRes = repository.grantSpeech(meetCode, targetUserId = 303L, canSpeak = false)
            assertTrue(revokeRes.isSuccess)
            assertEquals("viewer", revokeRes.getOrNull()?.role)
        }
    }

    @Test
    fun testMeetingBillingCycle() {
        runBlocking {
            val meetCode = "cp-meet-8821"

            // 3.10 Consulter la facturation
            val getBillRes = repository.getMeetingBilling(meetCode)
            assertTrue(getBillRes.isSuccess)
            assertNotNull(getBillRes.getOrNull()?.billing)

            // 3.11 Modifier la facturation
            val updateBillRes = repository.updateMeetingBilling(meetCode, priceAmount = 25.0, priceCurrency = "USD")
            assertTrue(updateBillRes.isSuccess)
            assertEquals(25.0, updateBillRes.getOrNull()?.priceAmount ?: 0.0, 0.01)
            assertEquals("USD", updateBillRes.getOrNull()?.priceCurrency)
        }
    }

    @Test
    fun testLeaveAndEndMeeting() {
        runBlocking {
            val meetCode = "cp-meet-4409"

            // Scénario 11: Quitter sans terminer
            val leaveRes = repository.leaveMeeting(meetCode)
            assertTrue(leaveRes.isSuccess)

            // Scénario 12: Terminer réunion par l'hôte
            val endRes = repository.endMeeting(meetCode)
            assertTrue(endRes.isSuccess)

            // Scénario 6: Refuser de rejoindre une réunion terminée (410)
            val joinAfterEnd = repository.joinMeeting(meetCode)
            assertTrue(joinAfterEnd.isFailure)
        }
    }

    @Test
    fun testCallsCycleAndHistory() {
        runBlocking {
            val walletA = "CD0100000001"
            val walletB = "CD0100000002"

            // Scénario 14: Refus d'appel vers soi-même
            val selfCall = repository.initiateCall(walletA, walletA, isVideo = false)
            assertTrue("Self call must fail", selfCall.isFailure)

            // Scénario 13: Initier appel valide
            val callRes = repository.initiateCall(walletA, walletB, isVideo = true)
            assertTrue("Call must succeed", callRes.isSuccess)
            val call = callRes.getOrNull()
            assertNotNull("Call must not be null", call)
            val callId = call!!.id

            // Scénario 15: Répondre avec statut autorisé
            val respondBusy = repository.respondCall(callId, walletB, "BUSY")
            assertTrue("Respond must succeed", respondBusy.isSuccess)
            assertEquals("Status must be BUSY", "BUSY", respondBusy.getOrNull()?.status)

            // Scénario 16: Terminer appel avec calcul de durée
            val endCallRes = repository.endCall(callId, walletA, durationSec = 120)
            assertTrue("End call must succeed", endCallRes.isSuccess)
            assertEquals("Duration must be 120", 120, endCallRes.getOrNull()?.durationSeconds)
            assertEquals("Status must be COMPLETED", "COMPLETED", endCallRes.getOrNull()?.status)

            // Scénario 17: Récupérer historique
            val historyRes = repository.getCallsHistory(walletA)
            assertTrue("History must succeed", historyRes.isSuccess)
            val historyList = historyRes.getOrNull()
            assertNotNull("History list must not be null", historyList)
            assertTrue("History must not be empty", historyList!!.isNotEmpty())
        }
    }
}
