package vaida.dryzaite.currencyconverter.data.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class UserOperation(
    @PrimaryKey
    var id: String = "",
    var currencyFrom: String = "",
    var fromAmount: Double = 0.0,
    var currencyTo: String = "",
    var toAmount: Double = 0.0,
    var feeApplied: Boolean = false
) : RealmObject()