package vaida.dryzaite.currencyconverter.data.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class UserBalance(
    @PrimaryKey
    var currency: String = "",
    var amount: Double = 0.0
) : RealmObject()