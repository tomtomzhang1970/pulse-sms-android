package xyz.klinker.messenger.shared.service.notification.conversation

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.RemoteInput
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.NotificationAction
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.receiver.NotificationDismissedReceiver
import xyz.klinker.messenger.shared.service.*
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.ActivityUtils

class NotificationActionHelper(private val service: NotificationService) {

    fun addReplyAction(builder: NotificationCompat.Builder, wearableExtender: NotificationCompat.WearableExtender, remoteInput: RemoteInput, conversation: NotificationConversation) {
        val actionExtender = NotificationCompat.Action.WearableExtender()
                .setHintLaunchesActivity(true)
                .setHintDisplayActionInline(true)

        val pendingReply: PendingIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !NotificationConstants.DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            val reply = Intent(service, ReplyService::class.java)
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
            pendingReply = PendingIntent.getService(service, conversation.id.toInt(), reply, PendingIntent.FLAG_UPDATE_CURRENT)

            val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                    service.getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .extend(actionExtender)
                    .build()

            if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                builder.addAction(action)
            }

            wearableExtender.addAction(action)
        } else {
            // on older versions, we have to show the reply activity button as an action and add the remote input to it
            // this will allow it to be used on android wear (we will have to handle this from the activity)
            // as well as have a reply quick action button.
            val reply = ActivityUtils.buildForComponent(ActivityUtils.NOTIFICATION_REPLY)
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
            reply.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pendingReply = PendingIntent.getActivity(service,
                    conversation.id.toInt(), reply, PendingIntent.FLAG_UPDATE_CURRENT)

            if (NotificationConstants.DEBUG_QUICK_REPLY) {
                // if we are debugging, the assumption is that we are on android N, we have to be stop showing
                // the remote input or else it will keep using the direct reply
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .extend(actionExtender)
                        .setAllowGeneratedReplies(true)
                        .build()

                if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                action.icon = R.drawable.ic_reply_white
                wearableExtender.addAction(action)
            } else {
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .build()

                if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                val wearReply = Intent(service, ReplyService::class.java)
                val extras = Bundle()
                extras.putLong(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
                wearReply.putExtras(extras)
                val wearPendingReply = PendingIntent.getService(service,
                        conversation.id.toInt() + 1, wearReply, PendingIntent.FLAG_UPDATE_CURRENT)

                val wearAction = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                        service.getString(R.string.reply), wearPendingReply)
                        .addRemoteInput(remoteInput)
                        .extend(actionExtender)
                        .build()

                wearableExtender.addAction(wearAction)
            }
        }
    }

    fun addNonReplyActions(builder: NotificationCompat.Builder, wearableExtender: NotificationCompat.WearableExtender, conversation: NotificationConversation) {
        if (!conversation.groupConversation && Settings.notificationActions.contains(NotificationAction.CALL)
                && (!Account.exists() || Account.primary)) {
            val call = Intent(service, NotificationCallService::class.java)
            call.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id)
            call.putExtra(NotificationCallService.EXTRA_PHONE_NUMBER, conversation.phoneNumbers)
            val callPending = PendingIntent.getService(service, conversation.id.toInt(),
                    call, PendingIntent.FLAG_UPDATE_CURRENT)

            builder.addAction(NotificationCompat.Action(R.drawable.ic_call_dark, service.getString(R.string.call), callPending))
        }

        val deleteMessage = Intent(service, NotificationDeleteService::class.java)
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_CONVERSATION_ID, conversation.id)
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_MESSAGE_ID, conversation.unseenMessageId)
        val pendingDeleteMessage = PendingIntent.getService(service, conversation.id.toInt(),
                deleteMessage, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.DELETE)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_delete_dark, service.getString(R.string.delete), pendingDeleteMessage))
        }

        val read = Intent(service, NotificationMarkReadService::class.java)
        read.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingRead = PendingIntent.getService(service, conversation.id.toInt(),
                read, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.READ)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_done_dark, service.getString(R.string.read), pendingRead))
        }

        wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_done_white, service.getString(R.string.read), pendingRead))
        wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_delete_white, service.getString(R.string.delete), pendingDeleteMessage))
    }

    fun addContentIntents(builder: NotificationCompat.Builder, conversation: NotificationConversation) {
        val delete = Intent(service, NotificationDismissedReceiver::class.java)
        delete.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingDelete = PendingIntent.getBroadcast(service, conversation.id.toInt(),
                delete, PendingIntent.FLAG_UPDATE_CURRENT)

        val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversation.id)
        open.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpen = PendingIntent.getActivity(service,
                conversation.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setDeleteIntent(pendingDelete)
        builder.setContentIntent(pendingOpen)
    }
}