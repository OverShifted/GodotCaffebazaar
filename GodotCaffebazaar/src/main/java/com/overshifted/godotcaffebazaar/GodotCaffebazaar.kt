package com.overshifted.godotcaffebazaar

import androidx.activity.result.ActivityResultRegistry
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArraySet
import androidx.fragment.app.FragmentActivity
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import ir.cafebazaar.poolakey.request.PurchaseRequest
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.UsedByGodot

class GodotCaffebazaar(godot: Godot) : GodotPlugin(godot) {

    private lateinit var payment_connection: Connection
    private lateinit var payment: Payment
    private lateinit var registry: ActivityResultRegistry

    override fun getPluginName(): String {
        return "GodotCaffebazaar"
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals = ArraySet<SignalInfo>()
        signals.add(SignalInfo("connection_succeed"))
        signals.add(SignalInfo("connection_failed", String::class.java))
        signals.add(SignalInfo("disconnected"))

        signals.add(SignalInfo("purchase_flow_began"))
        signals.add(SignalInfo("failed_to_begin_flow", String::class.java))
        signals.add(SignalInfo("purchase_succeed", Dictionary::class.java))
        signals.add(SignalInfo("purchase_canceled"))
        signals.add(SignalInfo("purchase_failed", String::class.java))

        signals.add(SignalInfo("consume_succeed"))
        signals.add(SignalInfo("consume_failed", String::class.java))

        signals.add(SignalInfo("query_succeed", Dictionary::class.java))
        signals.add(SignalInfo("query_failed", String::class.java))

        return signals
    }

    // Utils
    private fun encodePurchaseInfo(info: PurchaseInfo): Dictionary {
        val data = Dictionary()
        data["order_id"] = info.orderId
        data["purchase_token"] = info.purchaseToken
        data["payload"] = info.payload
        data["package_name"] = info.packageName
        data["purchase_state"] = if (info.purchaseState == PurchaseState.PURCHASED) "PURCHASED" else "REFUNDED"
        data["purchase_time"] = info.purchaseTime
        data["product_id"] = info.productId
        data["original_json"] = info.originalJson
        data["data_signature"] = info.dataSignature

        return data
    }

    @UsedByGodot
    fun init(p_rsa_public_key: String) {
        activity?.let {
            payment = Payment(
                context = it,
                config = PaymentConfiguration(SecurityCheck.Enable(p_rsa_public_key))
            )

            payment_connection = payment.connect {
                connectionSucceed {
                    emitSignal("connection_succeed")
                }
                connectionFailed { throwable ->
                    emitSignal("connection_failed", throwable.message)
                }
                disconnected {
                    emitSignal("disconnected")
                }
            }
        }
    }

    @UsedByGodot
    fun purchase_or_subscribe(p_subscribe: Boolean, p_product_id: String, p_payload: String, p_dynamic_price_token: String) {
        val fn = if (p_subscribe) Payment::subscribeProduct else Payment::purchaseProduct

        godot.activity?.let {
            fn(payment, it.activityResultRegistry, PurchaseRequest(
                productId = p_product_id,
                payload = p_payload,
                dynamicPriceToken = p_dynamic_price_token
            )) {
                purchaseFlowBegan {
                    emitSignal("purchase_flow_began")
                }
                failedToBeginFlow { throwable ->
                    emitSignal("failed_to_begin_flow", throwable.message)
                }
                purchaseSucceed { purchaseInfo ->
                    emitSignal("purchase_succeed", encodePurchaseInfo(purchaseInfo))
                }
                purchaseCanceled {
                    emitSignal("purchase_canceled")
                }
                purchaseFailed { throwable ->
                    emitSignal("purchase_failed", throwable.message)
                }
            }
        }
    }

    @UsedByGodot
    fun consume_product(p_purchase_token: String) {
        payment.consumeProduct(p_purchase_token) {
            consumeSucceed {
                emitSignal("consume_succeed")
            }
            consumeFailed { throwable ->
                emitSignal("consume_failed", throwable.message)
            }
        }
    }

    @UsedByGodot
    fun get_purchased_or_subscribed_products(p_subscribed: Boolean) {
        val fn = if (p_subscribed) Payment::getSubscribedProducts else Payment::getPurchasedProducts

        fn(payment) {
            querySucceed { results ->
                val data = Dictionary()
                for ((i, result) in results.withIndex()) {
                    data[i.toString()] = encodePurchaseInfo(result)
                }

                emitSignal("query_succeed", data)
            }
            queryFailed { throwable ->
                emitSignal("query_failed", throwable.message)
            }
        }
    }
}