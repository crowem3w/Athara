package soc.athara.app.modules.personal.home.components

import soc.athara.app.shared.payments.PaymentError

internal sealed interface PayOutcome {
    data class Completed(val amountMinor: Long) : PayOutcome
    data object Cancelled : PayOutcome
    data object Expired : PayOutcome
    data object NoBusinessFound : PayOutcome
    data class Failed(val error: PaymentError) : PayOutcome
}
