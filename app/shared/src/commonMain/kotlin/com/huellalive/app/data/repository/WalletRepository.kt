package com.huellalive.app.data.repository

import com.huellalive.app.data.model.DonationDto
import com.huellalive.app.data.model.MercadoPagoCheckoutDto
import com.huellalive.app.data.model.ShelterWalletDashboardDto
import com.huellalive.app.data.model.ShelterWithdrawalDto
import com.huellalive.app.data.model.WithdrawalRequest
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class WalletRepository(private val api: ApiService) {
    suspend fun donate(shelterId: String, amount: Double): Resource<MercadoPagoCheckoutDto> = safeApiCall { api.donate(shelterId, amount) }
    suspend fun getShelterDashboard(): Resource<ShelterWalletDashboardDto> = safeApiCall { api.getShelterWalletDashboard() }
    suspend fun getMercadoPagoConnectUrl() = safeApiCall { api.getMercadoPagoConnectUrl() }
    suspend fun requestWithdrawal(request: WithdrawalRequest): Resource<ShelterWithdrawalDto> =
        safeApiCall { api.requestShelterWithdrawal(request) }
}
