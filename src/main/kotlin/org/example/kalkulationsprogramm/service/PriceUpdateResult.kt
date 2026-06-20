package org.example.kalkulationsprogramm.service
import java.util.List
/**
 * Result object for price update operations.
 */
data class PriceUpdateResult(
    val updated: List<OfferItem>,
    val skipped: List<OfferItem>,
    val unmatched: List<OfferItem>
) {
    fun updated(): List<OfferItem> = updated
    fun skipped(): List<OfferItem> = skipped
    fun unmatched(): List<OfferItem> = unmatched
}
