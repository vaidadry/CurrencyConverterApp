package vaida.dryzaite.currencyconverter.util

import kotlinx.coroutines.CoroutineDispatcher

//dispatchers interface so that they could be passed in while testing
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default:CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}