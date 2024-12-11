package com.seoultech.onde

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.widget.Toast
import java.nio.charset.Charset

class InstagramNfcUtils(private val activity: Activity) {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var instagramUsername: String? = null
    private var onNfcReceived: ((String) -> Unit)? = null

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter == null) {
            Toast.makeText(activity, "이 기기는 NFC를 지원하지 않습니다.", Toast.LENGTH_LONG).show()
        }

        // PendingIntent 초기화
        val intent = Intent(activity, activity.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
    }

    fun setInstagramUsername(username: String) {
        instagramUsername = username
    }

    fun setNfcCallback(callback: (String) -> Unit) {
        onNfcReceived = callback
    }

    fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            try {
                val intentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    addDataType("application/vnd.com.seoultech.onde.instagram")
                }
                val intentFilters = arrayOf(intentFilter)
                val techLists = arrayOf(arrayOf<String>(Ndef::class.java.name))
                adapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists)
            } catch (e: Exception) {
                Toast.makeText(activity, "NFC 활성화 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun createNdefMessage(): NdefMessage? {
        val username = instagramUsername ?: return null
        val record = NdefRecord.createMime(
            "application/vnd.com.seoultech.onde.instagram",
            username.toByteArray(Charset.forName("UTF-8"))
        )
        return NdefMessage(arrayOf(record))
    }

    fun writeNdefMessage(tag: Tag) {
        val ndefMessage = createNdefMessage() ?: return
        val ndef = Ndef.get(tag)

        try {
            ndef?.let {
                it.connect()
                it.writeNdefMessage(ndefMessage)
                it.close()
                Toast.makeText(activity, "인스타그램 정보를 전송했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "NFC 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }

    fun handleIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val message = rawMessages[0] as NdefMessage
                val record = message.records[0]
                val receivedUsername = String(record.payload, Charset.forName("UTF-8"))
                onNfcReceived?.invoke(receivedUsername)
            }

            // Tag 객체를 통해 쓰기 작업 수행
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let { writeNdefMessage(it) }
        }
    }
}