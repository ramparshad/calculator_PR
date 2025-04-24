package com.metzger100.calculator.di

import android.app.Application
import androidx.room.Room
import com.metzger100.calculator.data.CalculatorRepository
import com.metzger100.calculator.data.local.CalculatorDatabase
import com.metzger100.calculator.data.local.CalculationDao
import com.metzger100.calculator.data.local.CurrencyListDao
import com.metzger100.calculator.data.local.CurrencyRateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): CalculatorDatabase {
        return Room.databaseBuilder(
            app,
            CalculatorDatabase::class.java,
            "calculator.db"
        ).build()
    }

    @Provides
    fun provideCalculationDao(db: CalculatorDatabase): CalculationDao {
        return db.calculationDao()
    }

    @Provides
    fun provideRepository(dao: CalculationDao): CalculatorRepository {
        return CalculatorRepository(dao)
    }

    @Provides
    fun provideCurrencyDao(db: CalculatorDatabase): CurrencyRateDao =
        db.currencyRateDao()

    @Provides
    fun provideCurrencyListDao(db: CalculatorDatabase): CurrencyListDao =
        db.currencyListDao()

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO)
}
