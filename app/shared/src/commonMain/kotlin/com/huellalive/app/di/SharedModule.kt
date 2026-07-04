package com.huellalive.app.di

import com.huellalive.app.auth.FirebaseAuthClient
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.data.remote.createHttpClient
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AdminRepository
import com.huellalive.app.data.repository.AdoptionRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.ChatRepository
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.NotificationRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.data.repository.WalletRepository
import com.russhwolf.settings.Settings
import org.koin.dsl.module


val sharedModule = module {
    single<Settings> { createSettings() }
    single<SessionManager> { SessionManager(get()) }
    single<FirebaseAuthClient> { FirebaseAuthClient() }
    single { createHttpClient(get()) }
    single<ApiService> { ApiService(get()) }
    single<AuthRepository> { AuthRepository(get(), get(), get()) }
    single<AnimalRepository> { AnimalRepository(get()) }
    single<AdminRepository> { AdminRepository(get()) }
    single<ShelterRepository> { ShelterRepository(get()) }
    single<UserRepository> { UserRepository(get()) }
    single<FeedRepository> { FeedRepository(get()) }
    single<ExploreRepository> { ExploreRepository(get()) }
    single<EngagementRepository> { EngagementRepository(get()) }
    single<ChatRepository> { ChatRepository(get()) }
    single<AdoptionRepository> { AdoptionRepository(get()) }
    single<WalletRepository> { WalletRepository(get()) }
    single<NotificationRepository> { NotificationRepository(get()) }
    single<MediaRepository> { MediaRepository(get()) }
}
