/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Firebase Admin 초기화
admin.initializeApp();

exports.sendNotificationOnMessage = onDocumentCreated(
    "chats/{chatId}/messages/{messageId}",
    async (event) => {
        try {
            const messageData = event.data.data();
            const chatId = event.params.chatId;

            if (!messageData) {
                console.error("No message data found");
                return;
            }

            const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
            if (!chatDoc.exists) {
                console.error(`Chat document with ID ${chatId} not found`);
                return;
            }

            const chatData = chatDoc.data();
            const participants = chatData?.participants || [];
            const senderId = messageData.senderId;

            const otherUserId = participants.find(uid => uid !== senderId);

            if (!otherUserId) {
                console.error("No recipient found for the message");
                return;
            }

            const userDoc = await admin.firestore().collection("users").doc(otherUserId).get();
            if (!userDoc.exists) {
                console.error(`User document with ID ${otherUserId} not found`);
                return;
            }

            const fcmToken = userDoc.get("fcmToken");
            if (!fcmToken) {
                console.log(`No FCM token for user: ${otherUserId}`);
                return;
            }

            // FCM 메시지 페이로드 생성
            const payload = {
                token: fcmToken,
                notification: {
                    title: "새 메시지 도착",
                    body: messageData.content || "메시지가 도착했습니다.",
                },
                data: {
                    chatId: String(chatId),
                    senderId: String(senderId),
                    userId: String(otherUserId),
                },
            };

            // FCM v1 API로 메시지 전송
            await admin.messaging().send(payload);
            console.log(`Notification sent to user ${otherUserId}`);
        } catch (error) {
            console.error("Error sending notification:", error);
        }
    }
);
