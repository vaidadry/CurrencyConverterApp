package vaida.dryzaite.currencyconverter.ui.converterfragment

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.util.ConverterManager
import vaida.dryzaite.currencyconverter.util.DispatcherProvider

class ConverterFragmentViewModel @ViewModelInject constructor(
    private val manager: ConverterManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    val channel = Channel<ConverterEvents> ()

    init {
        viewModelScope.launch {
            channel.consumeAsFlow().collect { event ->
                when (event) {
                    is ConverterEvents.InitBalancesEvent -> getInitBalances()
                    is ConverterEvents.UpdateInputQueryEvent -> _amountInput.value = event.input
                    is ConverterEvents.UpdateFromCurrencyEvent -> _currencyFromInput.value =
                        event.input
                    is ConverterEvents.UpdateToCurrencyEvent -> _currencyToInput.value = event.input
                    is ConverterEvents.UpdateBalancesEvent -> {
                        updateBalances(
                            event.fromAmountStr,
                            event.fromCurrency,
                            event.toCurrency,
                            event.toAmountStr
                        )
                    }
                    is ConverterEvents.ConvertEvent -> {
                        convert(
                            event.amountStr,
                            event.fromCurrency,
                            event.toCurrency
                        )
                    }
                }
            }
        }
    }

    sealed class ConverterEvents {
        data class UpdateInputQueryEvent(val input: String) : ConverterEvents()
        data class UpdateFromCurrencyEvent(val input: String) : ConverterEvents()
        data class UpdateToCurrencyEvent(val input: String) : ConverterEvents()
        object InitBalancesEvent : ConverterEvents()
        data class UpdateBalancesEvent(
            val fromAmountStr: String,
            val fromCurrency: String,
            val toCurrency: String,
            val toAmountStr: String
        ) : ConverterEvents()
        data class ConvertEvent(
            val amountStr: String,
            val fromCurrency: String,
            val toCurrency: String
        ) : ConverterEvents()
    }

    private val _balanceUpdate =
        MutableStateFlow<ConverterManager.BalanceUpdateEvent>(ConverterManager.BalanceUpdateEvent.Empty)
    val balanceUpdate: StateFlow<ConverterManager.BalanceUpdateEvent> = _balanceUpdate

    private val _conversion =
        MutableStateFlow<ConverterManager.ExchangeEvent>(ConverterManager.ExchangeEvent.Empty)
    val conversion: StateFlow<ConverterManager.ExchangeEvent> = _conversion

    private val _amountInput = MutableLiveData<String>()
    val amountInput: LiveData<String> = _amountInput

    private val _currencyFromInput = MutableLiveData<String>()
    val currencyFromInput: LiveData<String> = _currencyFromInput

    private val _currencyToInput = MutableLiveData<String>()
    val currencyToInput: LiveData<String> = _currencyToInput

    private val _balances = MutableLiveData<List<UserBalance>>()
    val balances: LiveData<List<UserBalance>> = _balances

    private fun getInitBalances() {
        viewModelScope.launch(dispatchers.io) {
            val balanceList = manager.getBalances()
            withContext(dispatchers.main) {
                _balances.value = balanceList
            }
        }
    }

    private fun updateBalances(
        fromAmountStr: String,
        fromCurrency: String,
        toCurrency: String,
        toAmountStr: String
    ) {
        val fromAmount = fromAmountStr.toDoubleOrNull()
        val toAmount = toAmountStr.drop(1).toDoubleOrNull()
        if (fromAmount == null || toAmount == null) {
            _balanceUpdate.value =
                ConverterManager.BalanceUpdateEvent.Failure(
                    R.string.converter_error_invalid_operation
                )
            return
        }
        viewModelScope.launch(dispatchers.io) {
            val result = manager.updateBalances(
                fromCurrency,
                fromAmount,
                toCurrency,
                toAmount
            )
            _balanceUpdate.value = result
        }
    }

    private suspend fun convert(
        amountStr: String,
        fromCurrency: String,
        toCurrency: String
    ) {
        val fromAmount = amountStr.toDoubleOrNull()
        if (fromAmount == null) {
            _conversion.value = ConverterManager.ExchangeEvent.Failure(
                R.string.converter_error_invalid_amount
            )
            return
        }
        viewModelScope.launch(dispatchers.io) {
            _conversion.value = ConverterManager.ExchangeEvent.Loading
            val response = manager.getRates(
                fromCurrency,
                fromAmount,
                toCurrency
            )
            _conversion.value = response
        }
    }
}