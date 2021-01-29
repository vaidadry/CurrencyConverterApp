package vaida.dryzaite.currencyconverter.data.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.joda.money.CurrencyUnit
import org.joda.money.Money

open class UserBalance(
    @PrimaryKey
    var currency: String = "",
    var amount: String = ""
) : RealmObject() {

    fun getMoney(): Money {
        val currency = CurrencyUnit.of(currency)
        return Money.of(currency, amount.toDouble())
    }
}