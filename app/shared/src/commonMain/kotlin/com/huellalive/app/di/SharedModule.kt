package com.huellalive.app.di

import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.data.remote.createHttpClient
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.UserRepository
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import com.huellalive.app.data.repository.SearchRepository


val sharedModule = module {
    single<Settings> { createSettings() }
    single<SessionManager> { SessionManager(get()) }
    single { createHttpClient(get()) }
    single<ApiService> { ApiService(get()) }
    single<AuthRepository> { AuthRepository(get(), get()) }
    single<AnimalRepository> { AnimalRepository(get()) }
    single<ShelterRepository> { ShelterRepository(get()) }
    single<UserRepository> { UserRepository(get()) }
    single<FeedRepository> { FeedRepository(get()) }
    single<SearchRepository> { SearchRepository(get()) }
}
