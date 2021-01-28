package vaida.dryzaite.currencyconverter.util

import android.util.Log
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.data.db.UserOperation
import vaida.dryzaite.currencyconverter.data.model.Rates.Companion.getRateForCurrency
import vaida.dryzaite.currencyconverter.repository.MainRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.math.round

class ConverterManager @Inject constructor (private val repository: MainRepository) {

    sealed class ExchangeEvent {
        class Success(val result: Double) : ExchangeEvent()
        class Failure(val errorResource: Int) : ExchangeEvent()
        object Loading : ExchangeEvent()
        object Empty : ExchangeEvent()
    }

    sealed class BalanceUpdateEvent {
        class Success(
            val balances: List<UserBalance>,
            val latestOperation: UserOperation,
            val fee: Double
        ) : BalanceUpdateEvent()
        class Failure(val errorResource: Int) : BalanceUpdateEvent()
        object Loading : BalanceUpdateEvent()
        object Empty : BalanceUpdateEvent()
    }

    suspend fun getRates(
        fromCurrency: String,
        fromAmount: Double,
        toCurrency: String
    ): ExchangeEvent {
        return try {
            Log.i("MSG", "api called")
            when (val response = repository.getRates(fromCurrency)) {
                is Resource.Success ->
                    response.data?.let {
                        val rates = it.rates
                        val rate = getRateForCurrency(toCurrency, rates)
                        if (rate == null) {
                            return@let ExchangeEvent.Failure(R.string.converter_error_unexpected)
                        } else {
                            val convertedToAmount = roundTo2Decimals(fromAmount, rate)
                            return@let ExchangeEvent.Success(convertedToAmount)
                        }
                    } ?: ExchangeEvent.Failure(R.string.converter_error_unexpected)
                is Resource.Error -> ExchangeEvent.Failure(R.string.converter_error_internet)
            }
        } catch (e: Exception) {
            return ExchangeEvent.Failure(R.string.converter_error_internet)
        }
    }

    fun getBalances() = repository.getUserBalance().run {
        if (this.isNullOrEmpty()) {
            // initial
            repository.insertOrUpdateBalance(UserBalance("EUR", "1000.00"))
            repository.getUserBalance()
        } else
            this
    }

    fun updateBalances(
        fromCurrency: String,
        fromAmount: Double,
        toCurrency: String,
        toAmount: Double
    ): BalanceUpdateEvent {
        try {
            val balance = repository.getUserBalance()
            val feeApplicable = checkFeeApplicable()
            val fee = calculateFee(feeApplicable, fromAmount)

            val fromCurrencyBalanceInDb = balance.firstOrNull { it.currency == fromCurrency }
            val toCurrencyBalanceInDb = balance.firstOrNull { it.currency == toCurrency }
            val fromCurrencyRemainderEnough = balance
                .filter { it.currency.trim() == fromCurrency.trim() }
                .map { it.amount.toFloat() >= (fromAmount + fee) }
                .firstOrNull()

            // validation
            if (fromCurrencyBalanceInDb == null) {
                return BalanceUpdateEvent.Failure(R.string.converter_error_currencyNotOwned)
            } else {
                if (!fromCurrencyRemainderEnough!!) {
                    return BalanceUpdateEvent.Failure(R.string.converter_error_fundsNotSufficient)
                } else {
                    if (toCurrencyBalanceInDb != null) {
                        toCurrencyBalanceInDb.let {
                            repository.insertOrUpdateBalance(
                                UserBalance(
                                    it.currency,
                                    roundTo2Decimals(
                                        (it.amount.toFloat() + toAmount),
                                        null
                                    ).toString()
                                )
                            )
                        }
                    } else {
                        repository.insertOrUpdateBalance(
                            UserBalance(
                                toCurrency,
                                toAmount.toString()
                            )
                        )
                    }

                    fromCurrencyBalanceInDb.let {
                        repository.insertOrUpdateBalance(
                            UserBalance(
                                it.currency,
                                roundTo2Decimals((it.amount.toDouble() - fromAmount),
                                    null
                                ).toString()
                            )
                        )
                        applyFee(it, fee)
                    }
                }
            }

            val operationID = UUID.randomUUID().toString()
            repository.insertUserOperation(
                UserOperation(
                    operationID,
                    fromCurrency,
                    fromAmount.toString(),
                    toCurrency,
                    toAmount.toString(),
                    feeApplicable))

            val updatedBalance = repository.getUserBalance()
            val dialogInfo = repository.getAllOperations().find { it.id == operationID }

            return BalanceUpdateEvent.Success(updatedBalance, dialogInfo!!, fee)
        } catch (exception: Exception) {
            return BalanceUpdateEvent.Failure(R.string.converter_error_unexpected)
        }
    }

    private fun applyFee(balance: UserBalance, fee: Double) {
        val updatedBalance = repository.getUserBalance()
            .find { it.currency == balance.currency }
        updatedBalance?.let {
            repository.insertOrUpdateBalance(
                UserBalance(
                    it.currency,
                    roundTo2Decimals(
                        (it.amount.toDouble() - fee),
                        null
                    ).toString()
                )
            )
        }
    }

    private fun checkFeeApplicable(): Boolean {
        val operations = repository.getAllOperations().size
        val lessThan5 = operations < 5
        val every10th = operations % 10 == 0

        return !(lessThan5 || every10th)
    }

    private fun calculateFee(applyFee: Boolean, fromAmount: Double): Double {
        return if (applyFee) roundTo2Decimals(fromAmount, 0.007) else 0.00
    }

    private fun roundTo2Decimals(amount: Double, rate: Double?): Double {
        return round(amount * (rate ?: 1.0) * 100) / 100
    }
}