package vaida.dryzaite.currencyconverter.repository

import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.data.db.UserOperation
import vaida.dryzaite.currencyconverter.data.model.CurrencyResponse
import vaida.dryzaite.currencyconverter.util.Resource

interface MainRepository {

    suspend fun getRates(base: String): Resource<CurrencyResponse>

    fun insertUserOperation(operation: UserOperation)

    fun getAllOperations(): List<UserOperation>

    fun getUserBalance(): List<UserBalance>

    fun insertOrUpdateBalance(balance: UserBalance)
}