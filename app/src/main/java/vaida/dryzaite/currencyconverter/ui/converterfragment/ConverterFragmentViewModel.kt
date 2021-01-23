package vaida.dryzaite.currencyconverter.ui.converterfragment

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vaida.dryzaite.currencyconverter.data.model.Rates
import vaida.dryzaite.currencyconverter.repository.MainRepository
import vaida.dryzaite.currencyconverter.util.DispatcherProvider
import vaida.dryzaite.currencyconverter.util.Resource
import java.lang.Math.round

class ConverterFragmentViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    //event class - that represents events that will be send from VM to UI
    sealed class CurrencyEvent {
        class Success(val resultText: String): CurrencyEvent()
        class Failure(val errorText: String): CurrencyEvent()
        object Loading: CurrencyEvent()
        object Empty: CurrencyEvent()
    }

    private val _conversion = MutableStateFlow<CurrencyEvent>(
        CurrencyEvent.Empty
    )
    val conversion: StateFlow<CurrencyEvent> = _conversion

    fun convert(
        amountStr: String,
        fromCurrency: String,
        toCurrency: String
    ) {
        val fromAmount = amountStr.toFloatOrNull()
        if (fromAmount == null) {
            _conversion.value =
                CurrencyEvent.Failure(
                    "Invalid amount"
                )
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _conversion.value =
                CurrencyEvent.Loading
            when(val responseRates = repository.getRates(fromCurrency)) {
                is Resource.Error -> _conversion.value =
                    CurrencyEvent.Failure(
                        responseRates.message!!
                    )
                is Resource.Success -> {
                    val rates = responseRates.data!!.rates
                    val rate = getRateForCurrency(toCurrency, rates)
                    if (rate == null) {
                        _conversion.value =
                            CurrencyEvent.Failure(
                                "Unexpected error"
                            )
                    } else {
                        val convertedCurrency = round(fromAmount * rate * 100) / 100 // makes sure that 2 digits after comma only
                        _conversion.value =
                            CurrencyEvent.Success(
                                "+ $convertedCurrency"
                            )
                    }
                }
            }
        }
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