package vaida.dryzaite.currencyconverter.util

import android.content.Context
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.data.db.UserOperation
import vaida.dryzaite.currencyconverter.data.model.Rates
import vaida.dryzaite.currencyconverter.repository.MainRepository
import java.util.*
import javax.inject.Inject
import kotlin.math.round

class ConverterManager @Inject constructor (private val repository: MainRepository) {

    sealed class ExchangeEvent {
        class Success(val resultText: String) : ExchangeEvent()
        class Failure(val errorText: String) : ExchangeEvent()
        object Loading : ExchangeEvent()
        object Empty : ExchangeEvent()
    }

    sealed class BalanceUpdateEvent {
        class Success(val balances: List<UserBalance>, val dialog: String) : BalanceUpdateEvent()
        class Failure(val errorText: String) : BalanceUpdateEvent()
        object Loading : BalanceUpdateEvent()
        object Empty : BalanceUpdateEvent()
    }

    suspend fun getRates(
        fromCurrency: String,
        fromAmount: Double,
        toCurrency: String,
        context: Context
    ): ExchangeEvent {
        return try {
            when (val response = repository.getRates(fromCurrency)) {
                is Resource.Success ->
                    response.data?.let {
                        val rates = it.rates
                        val rate = getRateForCurrency(toCurrency, rates)
                        if (rate == null) {
                            return@let ExchangeEvent.Failure(context.getString(R.string.converter_error_unexpected))
                        } else {
                            val convertedToAmount = round(fromAmount * rate * 100) / 100
                            return@let ExchangeEvent.Success(context.getString(R.string.balance_item_converted).format(convertedToAmount))
                        }
                    } ?: ExchangeEvent.Failure(context.getString(R.string.converter_error_unexpected))
                is Resource.Error -> ExchangeEvent.Failure(context.getString(R.string.converter_error_internet))
            }
        } catch (e: Exception) {
            return ExchangeEvent.Failure(context.getString(R.string.converter_error_internet))
        }
    }

    fun getBalances() = repository.getUserBalance().run {
        if (this.isNullOrEmpty()) {
            // initial
            repository.insertOrUpdateBalance(UserBalance("EUR", 1000.0))
            repository.getUserBalance()
        } else
            this
    }

    fun updateBalances(
        fromCurrency: String,
        fromAmount: Double,
        toCurrency: String,
        toAmount: Double,
        context: Context
    ): BalanceUpdateEvent {
        try {
            val balance = repository.getUserBalance()
            val feeApplicable = checkFeeApplicable()
            val fee = calculateFee(feeApplicable, fromAmount)

            val fromCurrencyBalanceInDb = balance.firstOrNull { it.currency == fromCurrency }
            val toCurrencyBalanceInDb = balance.firstOrNull { it.currency == toCurrency }
            val fromCurrencyRemainderEnough = balance
                .filter { it.currency.trim() == fromCurrency.trim() }
                .map { it.amount >= (fromAmount + fee) }
                .firstOrNull()

            // validation
            if (fromCurrencyBalanceInDb == null) {
                return BalanceUpdateEvent.Failure(context.getString(R.string.converter_error_currencyNotOwned))
            } else {
                if (!fromCurrencyRemainderEnough!!) {
                    return BalanceUpdateEvent.Failure(context.getString(R.string.converter_error_fundsNotSufficient))
                } else {
                    if (toCurrencyBalanceInDb != null) {
                        toCurrencyBalanceInDb.let {
                            repository.insertOrUpdateBalance(UserBalance(it.currency, (it.amount + toAmount)))
                        }
                    } else {
                        repository.insertOrUpdateBalance(UserBalance(toCurrency, toAmount))
                    }

                    fromCurrencyBalanceInDb.let {
                        repository.insertOrUpdateBalance(UserBalance(it.currency, (it.amount - fromAmount)))
                        applyFee(it, fee)
                    }
                }
            }

            repository.insertUserOperation(
                UserOperation(
                    UUID.randomUUID().toString(),
                    fromCurrency,
                    fromAmount,
                    toCurrency,
                    toAmount,
                    feeApplicable))

            val updatedBalance = repository.getUserBalance()
            val dialogText =
                if (fee != 0.0) {
                    context.getString(R.string.converter_dialog_message).format(fromAmount, fromCurrency, toAmount, toCurrency, fee, fromCurrency)
                } else {
                    context.getString(R.string.converter_dialog_message_no_fee).format(fromAmount, fromCurrency, toAmount, toCurrency)
                }

            return BalanceUpdateEvent.Success(updatedBalance, dialogText)
        } catch (exception: Exception) {
            return BalanceUpdateEvent.Failure(context.getString(R.string.converter_error_unexpected))
        }
    }

    private fun applyFee(balance: UserBalance, fee: Double) {
        val updatedBalance = repository.getUserBalance().find { it.currency == balance.currency }
        updatedBalance?.let {
            repository.insertOrUpdateBalance(UserBalance(it.currency, (it.amount - fee)))
        }
    }

    private fun checkFeeApplicable(): Boolean {
        val operations = repository.getAllOperations().size
        return operations > 5 ||
                operations % 10 == 0 // optional
    }

    private fun calculateFee(applyFee: Boolean, fromAmount: Double): Double {
        return if (applyFee) round(fromAmount * 0.007 * 100) / 100 else 0.0
    }

    private fun getRateForCurrency(currency: String, rates: Rates) = when (currency) {
        "CAD" -> rates.cAD
        "HKD" -> rates.hKD
        "ISK" -> rates.iSK
        "EUR" -> rates.eUR
        "PHP" -> rates.pHP
        "DKK" -> rates.dKK
        "HUF" -> rates.hUF
        "CZK" -> rates.cZK
        "AUD" -> rates.aUD
        "RON" -> rates.rON
        "SEK" -> rates.sEK
        "IDR" -> rates.iDR
        "INR" -> rates.iNR
        "BRL" -> rates.bRL
        "RUB" -> rates.rUB
        "HRK" -> rates.hRK
        "JPY" -> rates.jPY
        "THB" -> rates.tHB
        "CHF" -> rates.cHF
        "SGD" -> rates.sGD
        "PLN" -> rates.pLN
        "BGN" -> rates.bGN
        "CNY" -> rates.cNY
        "NOK" -> rates.nOK
        "NZD" -> rates.nZD
        "ZAR" -> rates.zAR
        "USD" -> rates.uSD
        "MXN" -> rates.mXN
        "ILS" -> rates.iLS
        "GBP" -> rates.gBP
        "KRW" -> rates.kRW
        "MYR" -> rates.mYR
        else -> null
    }
}