package com.example

import com.example.data.api.ApiClient
import com.example.data.api.AuthenticatedMedia
import com.example.data.model.ApiMessage
import com.example.data.model.SendMessageRequest
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

/** Offline contracts only; never call a deployed CashPay API. */
class OnChatContractsTest {
    @Test fun meetingLinksPreserveTicketsAndRejectUntrustedOrigins() {
        val code = "room-1234"
        assertEquals("https://app.cashpay-all.com/feedback/meet/$code", com.example.data.api.MeetingLinks.canonical(code))
        assertEquals("https://app.cashpay-all.com/feedback/meet/$code?ticket=OC-code", com.example.data.api.MeetingLinks.canonical("https://app.cashpay-all.com/feedback/meet/$code?ticket=OC-code"))
        assertNull(com.example.data.api.MeetingLinks.canonical("https://evil.example/feedback/meet/$code"))
        assertNull(com.example.data.api.MeetingLinks.canonical("http://app.cashpay-all.com/feedback/meet/$code"))
        assertNull(com.example.data.api.MeetingLinks.canonical("https://name@app.cashpay-all.com/feedback/meet/$code"))
    }
    @Test fun attachmentReferencesSurviveSerialization() {
        val adapter = Moshi.Builder().build().adapter(SendMessageRequest::class.java)
        val source = SendMessageRequest("", "doc", mapOf("fileName" to "report.pdf"),
            "/api/uploads/onchat/7/file.pdf", "report.pdf")
        assertEquals(source, adapter.fromJson(adapter.toJson(source)))
    }

    @Test fun serverAttachmentMetadataIsDecodedAsAnObject() {
        val adapter = Moshi.Builder().build().adapter(ApiMessage::class.java)
        val message = adapter.fromJson("""{"id":"msg_7","type":"audio","status":"sent","metadata":{"fileUrl":"/api/uploads/onchat/7/audio.m4a","fileName":"voice.m4a"}}""")!!
        assertEquals("sent", message.status)
        assertEquals("voice.m4a", message.metadata?.get("fileName"))
    }
}
