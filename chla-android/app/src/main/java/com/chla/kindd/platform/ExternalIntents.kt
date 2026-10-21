package com.chla.kindd.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

internal enum class ExternalIntentAction {
    DIAL,
    VIEW,
    SENDTO
}

internal data class ExternalIntentRequest(
    val action: ExternalIntentAction,
    val uri: String
)

internal fun dialRequest(phone: String): ExternalIntentRequest? {
    val digits = phone.filter(Char::isDigit).takeIf(String::isNotEmpty) ?: return null
    return ExternalIntentRequest(ExternalIntentAction.DIAL, "tel:$digits")
}

internal fun websiteRequest(website: String): ExternalIntentRequest? {
    val value = website.trim().takeIf(String::isNotEmpty) ?: return null
    val candidate = if (SCHEME_PATTERN.containsMatchIn(value)) value else "https://$value"
    val uri = try {
        URI(candidate)
    } catch (_: Exception) {
        return null
    }
    if (uri.scheme?.lowercase() !in WEB_SCHEMES || uri.host.isNullOrBlank()) return null
    return ExternalIntentRequest(ExternalIntentAction.VIEW, uri.toASCIIString())
}

internal fun emailRequest(address: String): ExternalIntentRequest? {
    val value = address.trim().takeIf { EMAIL_PATTERN.matches(it) } ?: return null
    return ExternalIntentRequest(ExternalIntentAction.SENDTO, "mailto:$value")
}

internal fun launchExternalIntent(
    request: ExternalIntentRequest?,
    canResolve: (ExternalIntentRequest) -> Boolean,
    launch: (ExternalIntentRequest) -> Unit
): Boolean {
    request ?: return false
    return try {
        if (!canResolve(request)) {
            false
        } else {
            launch(request)
            true
        }
    } catch (_: RuntimeException) {
        false
    }
}

internal fun Context.launchDialer(phone: String): Boolean = launch(dialRequest(phone))

internal fun Context.launchWebsite(website: String): Boolean = launch(websiteRequest(website))

internal fun Context.launchEmail(address: String): Boolean = launch(emailRequest(address))

private fun Context.launch(request: ExternalIntentRequest?): Boolean {
    val intent = request?.toAndroidIntent() ?: return false
    return launchExternalIntent(
        request = request,
        canResolve = { intent.resolveActivity(packageManager) != null },
        launch = { startActivity(intent) }
    )
}

private fun ExternalIntentRequest.toAndroidIntent(): Intent = Intent(
    when (action) {
        ExternalIntentAction.DIAL -> Intent.ACTION_DIAL
        ExternalIntentAction.VIEW -> Intent.ACTION_VIEW
        ExternalIntentAction.SENDTO -> Intent.ACTION_SENDTO
    },
    Uri.parse(uri)
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
private val WEB_SCHEMES = setOf("http", "https")
private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
