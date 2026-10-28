@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.GuardianSendResult
import com.playerid.app.domain.memory.GuardianShareContact
import com.playerid.app.domain.memory.validate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Contacts.*
import platform.ContactsUI.*
import platform.Foundation.*
import platform.MessageUI.*
import platform.UIKit.UIViewController
import platform.darwin.NSObject

class IosGuardianSharing : NSObject(), CNContactPickerDelegateProtocol,
    MFMessageComposeViewControllerDelegateProtocol, MFMailComposeViewControllerDelegateProtocol {
    private var contactResult: CompletableDeferred<GuardianShareContact?>? = null
    private var sendResult: CompletableDeferred<GuardianSendResult>? = null

    suspend fun pickContact(): GuardianShareContact? {
        if (contactResult != null || sendResult != null) throw CameraCaptureException("Close the current sharing sheet first.")
        val presenter = memorySharePresenter()
        val picker = CNContactPickerViewController().apply {
            delegate = this@IosGuardianSharing
            displayedPropertyKeys = listOf(CNContactPhoneNumbersKey, CNContactEmailAddressesKey)
            predicateForSelectionOfContact = NSPredicate.predicateWithValue(false)
            predicateForSelectionOfProperty = NSPredicate.predicateWithValue(true)
        }
        val result = CompletableDeferred<GuardianShareContact?>()
        contactResult = result
        try {
            presenter.presentViewController(picker, animated = true, completion = null)
            return result.await()
        } finally {
            contactResult = null
        }
    }

    /** Phone contacts whose name matches [name]; empty if access is denied so the manual picker remains the fallback. */
    suspend fun findContacts(name: String): List<GuardianShareContact> {
        val store = CNContactStore()
        val granted = CompletableDeferred<Boolean>()
        store.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { ok, _ -> granted.complete(ok) }
        if (!granted.await()) return emptyList()
        return withContext(Dispatchers.Default) {
            val keys = listOf(
                CNContactFormatter.descriptorForRequiredKeysForStyle(CNContactFormatterStyle.CNContactFormatterStyleFullName),
                CNContactPhoneNumbersKey, CNContactEmailAddressesKey
            )
            val found = store.unifiedContactsMatchingPredicate(CNContact.predicateForContactsMatchingName(name), keys, null)
                ?: return@withContext emptyList()
            found.filterIsInstance<CNContact>().flatMap { contact ->
                val full = CNContactFormatter.stringFromContact(contact, CNContactFormatterStyle.CNContactFormatterStyleFullName).orEmpty()
                val phone = contact.phoneNumbers.firstNotNullOfOrNull { (it as? CNLabeledValue)?.value as? CNPhoneNumber }?.stringValue
                val email = contact.emailAddresses.firstNotNullOfOrNull { (it as? CNLabeledValue)?.value as? String }
                listOfNotNull(
                    phone?.let { GuardianShareContact(displayName = full, phoneNumber = it) },
                    if (phone == null) email?.let { GuardianShareContact(displayName = full, email = it) } else null
                )
            }.filter { runCatching { it.validate() }.isSuccess }
        }
    }

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
        picker.dismissViewControllerAnimated(true) { contactResult?.complete(null) }
    }

    override fun contactPicker(picker: CNContactPickerViewController, didSelectContactProperty: CNContactProperty) {
        val property = didSelectContactProperty
        val name = CNContactFormatter.stringFromContact(property.contact, CNContactFormatterStyle.CNContactFormatterStyleFullName).orEmpty()
        val contact = when (property.key) {
            CNContactPhoneNumbersKey -> GuardianShareContact(
                displayName = name,
                phoneNumber = (property.value as? CNPhoneNumber)?.stringValue
            )
            CNContactEmailAddressesKey -> GuardianShareContact(displayName = name, email = property.value as? String)
            else -> null
        }
        picker.dismissViewControllerAnimated(true) {
            if (contact == null) {
                contactResult?.completeExceptionally(CameraCaptureException("Choose a contact's phone number or email address."))
            } else {
                try {
                    contact.validate()
                    contactResult?.complete(contact)
                } catch (failure: IllegalArgumentException) {
                    contactResult?.completeExceptionally(CameraCaptureException(failure.message ?: "This contact cannot be used."))
                }
            }
        }
    }

    suspend fun send(
        memories: List<MediaReference>,
        contacts: List<GuardianShareContact>,
        message: String,
        email: Boolean
    ): GuardianSendResult {
        if (contactResult != null || sendResult != null) throw CameraCaptureException("Close the current sharing sheet first.")
        if (contacts.isEmpty()) throw CameraCaptureException("Select people to share with.")
        contacts.forEach { it.validate() }
        val unique = memories.distinctBy { it.identifier }
        val urls = validatedMemoryShareUrls(unique)
        if (email && !MFMailComposeViewController.canSendMail()) {
            throw CameraCaptureException("Mail isn't configured on this iPhone. Configure an account in Mail, or use General share.")
        }
        val mailAttachments = if (email) withContext(Dispatchers.Default) {
            urls.map { url ->
                NSData.dataWithContentsOfURL(url)
                    ?: throw CameraCaptureException("An attachment couldn't be read. Nothing was sent.")
            }
        } else emptyList()
        val controller: UIViewController = if (email) {
            val addresses = contacts.mapNotNull { it.email?.takeIf(String::isNotBlank) }
            if (addresses.size != contacts.size) throw CameraCaptureException("Every Mail recipient needs an email address.")
            MFMailComposeViewController().apply {
                mailComposeDelegate = this@IosGuardianSharing
                setToRecipients(addresses)
                setSubject("PlayerID memories")
                setMessageBody(message, isHTML = false)
                urls.forEachIndexed { index, url ->
                    val type = unique[index].mimeType ?: when (url.pathExtension?.lowercase()) {
                        "jpg" -> "image/jpeg"
                        "mov" -> "video/quicktime"
                        else -> "video/mp4"
                    }
                    addAttachmentData(mailAttachments[index], mimeType = type, fileName = url.lastPathComponent ?: "memory")
                }
            }
        } else {
            val numbers = contacts.mapNotNull { it.phoneNumber?.takeIf(String::isNotBlank) }
            if (numbers.size != contacts.size) throw CameraCaptureException("Every Messages recipient needs a phone number.")
            if (!MFMessageComposeViewController.canSendText() || !MFMessageComposeViewController.canSendAttachments()) {
                throw CameraCaptureException("Messages with attachments isn't available on this iPhone. Use General share instead.")
            }
            MFMessageComposeViewController().apply {
                messageComposeDelegate = this@IosGuardianSharing
                recipients = numbers
                body = message
                urls.forEach { url ->
                    if (!addAttachmentURL(url, withAlternateFilename = url.lastPathComponent)) {
                        throw CameraCaptureException("Messages cannot attach one of these files. Nothing was sent. Try General share.")
                    }
                }
            }
        }
        val presenter = memorySharePresenter()
        val result = CompletableDeferred<GuardianSendResult>()
        sendResult = result
        try {
            presenter.presentViewController(controller, animated = true, completion = null)
            return result.await()
        } finally {
            sendResult = null
        }
    }

    override fun messageComposeViewController(controller: MFMessageComposeViewController, didFinishWithResult: MessageComposeResult) {
        controller.dismissViewControllerAnimated(true) {
            when (didFinishWithResult) {
                MessageComposeResult.MessageComposeResultSent -> sendResult?.complete(GuardianSendResult.SENT)
                MessageComposeResult.MessageComposeResultCancelled -> sendResult?.complete(GuardianSendResult.CANCELLED)
                else -> sendResult?.completeExceptionally(CameraCaptureException("Messages could not send these memories. Check the draft and retry."))
            }
        }
    }

    override fun mailComposeController(controller: MFMailComposeViewController, didFinishWithResult: MFMailComposeResult, error: NSError?) {
        controller.dismissViewControllerAnimated(true) {
            if (error != null) {
                sendResult?.completeExceptionally(CameraCaptureException("Mail failed: ${error.localizedDescription}"))
            } else when (didFinishWithResult) {
                MFMailComposeResult.MFMailComposeResultSent -> sendResult?.complete(GuardianSendResult.SENT)
                MFMailComposeResult.MFMailComposeResultSaved -> sendResult?.complete(GuardianSendResult.DRAFT_SAVED)
                MFMailComposeResult.MFMailComposeResultCancelled -> sendResult?.complete(GuardianSendResult.CANCELLED)
                else -> sendResult?.completeExceptionally(CameraCaptureException("Mail could not send these memories. Please retry."))
            }
        }
    }
}
