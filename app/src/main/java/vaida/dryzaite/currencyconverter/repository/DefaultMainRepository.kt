package vaida.dryzaite.currencyconverter.repository

import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.save
import io.realm.Realm
import vaida.dryzaite.currencyconverter.data.CurrencyApi
import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.data.db.UserOperation
import vaida.dryzaite.currencyconverter.data.model.CurrencyResponse
import vaida.dryzaite.currencyconverter.util.Resource
import javax.inject.Inject

class DefaultMainRepository @Inject constructor(
    private val api: CurrencyApi,
    private val realm: Realm
) : MainRepository {

    override suspend fun getRates(base: String): Resource<CurrencyResponse> {
        return try {
            val response = api.getRates(base)
            val result = response.body()
            if (response.isSuccessful && result != null) {
                Resource.Success(result)
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An Error Occurred")
        }
    }

    override fun insertUserOperation(operation: UserOperation) = operation.save()

    override fun getAllOperations(): List<UserOperation> = queryAll()

    override fun getUserBalance(): List<UserBalance> = queryAll()

    override fun insertOrUpdateBalance(balance: UserBalance) = balance.save()
}