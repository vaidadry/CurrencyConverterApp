package vaida.dryzaite.currencyconverter.util

// wraps response so that if error, it would be caught
sealed class Resource<T>(val data: T?, val message: String?) {
    class Success<T>(data: T) : Resource<T>(data, null)
    class Error<T>(message: String?) : Resource<T>(null, message)
}