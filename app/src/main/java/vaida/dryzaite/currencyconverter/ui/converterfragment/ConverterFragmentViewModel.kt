package vaida.dryzaite.currencyconverter.ui.converterfragment

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _balanceUpdate =
        MutableStateFlow<ConverterManager.BalanceUpdateEvent>(ConverterManager.BalanceUpdateEvent.Empty)
    val balanceUpdate: StateFlow<ConverterManager.BalanceUpdateEvent> = _balanceUpdate

    private val _conversion =
        MutableStateFlow<ConverterManager.ExchangeEvent>(ConverterManager.ExchangeEvent.Empty)
    val conversion: StateFlow<ConverterManager.ExchangeEvent> = _conversion

    private val _amountInput = MutableLiveData<String>()
    val amountInput: LiveData<String> = _amountInput

    fun updateInputQuery(input: String) {
        _amountInput.value = input
    }

    private val _currencyFromInput = MutableLiveData<String>()
    val currencyFromInput: LiveData<String> = _currencyFromInput

    fun updateCurrencyFromQuery(input: String) {
        _currencyFromInput.value = input
    }

    private val _currencyToInput = MutableLiveData<String>()
    val currencyToInput: LiveData<String> = _currencyToInput

    fun updateCurrencyToQuery(input: String) {
        _currencyToInput.value = input
    }

    private val _balances = MutableLiveData<List<UserBalance>>()
    val balances: LiveData<List<UserBalance>> = _balances

    fun getInitBalances() {
        viewModelScope.launch(dispatchers.io) {
            val balanceList = manager.getBalances()
            withContext(dispatchers.main) {
                _balances.value = balanceList
            }
        }
    }

    fun updateBalances(
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

    suspend fun convert(
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