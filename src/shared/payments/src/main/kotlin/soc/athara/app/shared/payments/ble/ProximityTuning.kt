package soc.athara.app.shared.payments.ble

/**
 * Client-side numbers for Bluetooth proximity payments. The SERVER owns the real signal thresholds
 * (PROXIMITY_RSSI_* in the backend config) and decides; these only keep the apps from hammering it with noise.
 * All of them are placeholders until calibrated on real phones (design section 8).
 */
object ProximityTuning {
    /** A beacon weaker than this is not worth sending. Looser than the server floor on purpose, so the server gives the "move closer" answer. */
    const val CLIENT_FLOOR_DBM = -78

    /** How often the apps look at the signal readings. */
    const val TICK_MS = 250L

    /** How often the payer asks the server what happened. */
    const val POLL_MS = 1_000L

    /** The payer gives up looking for a business after this long. */
    const val SEARCH_TIMEOUT_MS = 30_000L

    /** After a failed or refused claim, the business ignores that beacon id for this long. */
    const val IGNORE_BEACON_MS = 20_000L

    /** How often a scanning business asks the server for reduced-mode requests. */
    const val INBOX_POLL_MS = 2_000L

    /** A business replaces its beacon nonce this long before the server would expire it. */
    const val NONCE_REFRESH_MARGIN_SECONDS = 4
}
