package vaida.dryzaite.currencyconverter.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vaida.dryzaite.currencyconverter.BuildConfig
import vaida.dryzaite.currencyconverter.data.CurrencyApi
import vaida.dryzaite.currencyconverter.repository.DefaultMainRepository
import vaida.dryzaite.currencyconverter.repository.MainRepository
import vaida.dryzaite.currencyconverter.util.DispatcherProvider
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideCurrencyApi(): CurrencyApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CurrencyApi::class.java)

    @Singleton
    @Provides
    fun provideMainRepository(api: CurrencyApi, realm: Realm): MainRepository =
        DefaultMainRepository(api, realm)

    @Singleton
    @Provides
    fun provideDispatchers(): DispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher
            get() = Dispatchers.Main
        override val io: CoroutineDispatcher
            get() = Dispatchers.IO
        override val default: CoroutineDispatcher
            get() = Dispatchers.Default
        override val unconfined: CoroutineDispatcher
            get() = Dispatchers.Unconfined
    }

    @Singleton
    @Provides
    fun provideRealm(@ApplicationContext context: Context): Realm {
        Realm.init(context)
        val builder = RealmConfiguration.Builder()
            .name("user_operations.realm")
            .schemaVersion(1)
            .build()
        return Realm.getInstance(builder)
    }
}