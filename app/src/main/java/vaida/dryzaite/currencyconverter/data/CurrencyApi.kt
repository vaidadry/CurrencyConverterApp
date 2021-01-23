package vaida.dryzaite.currencyconverter.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import vaida.dryzaite.currencyconverter.data.model.CurrencyResponse

interface CurrencyApi {

    @GET("/latest")
    suspend fun getRates(
        @Query("base") base: String
    ): Response<CurrencyResponse>
}