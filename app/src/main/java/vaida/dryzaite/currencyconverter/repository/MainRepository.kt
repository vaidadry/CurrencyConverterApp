package vaida.dryzaite.currencyconverter.repository

import vaida.dryzaite.currencyconverter.data.model.CurrencyResponse
import vaida.dryzaite.currencyconverter.util.Resource

interface MainRepository {

    suspend fun getRates(base: String): Resource<CurrencyResponse>
}