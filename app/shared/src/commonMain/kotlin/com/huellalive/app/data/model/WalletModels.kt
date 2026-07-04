package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DonationDto(
    val id: String,
    val humanId: String,
    val shelterId: String,
    val amount: Double,
    val currency: String = "PEN",
    val developerCut: Double,
    val shelterCut: Double,
    val status: String,
    val createdAt: String
)

@Serializable
data class MercadoPagoCheckoutDto(
    val donation: DonationDto,
    val checkoutUrl: String? = null,
    val sandboxCheckoutUrl: String? = null,
    val preferenceId: String? = null
)

@Serializable
data class MercadoPagoConnectDto(
    val url: String,
    val connected: Boolean = false
)

@Serializable
data class ShelterWalletTotalsDto(
    val received: Double,
    val direct: Double,
    val throughAnimals: Double,
    val netForShelter: Double,
    val availableToWithdraw: Double,
    val withdrawn: Double
)

@Serializable
data class AnimalDonationSummaryDto(
    val animalId: String,
    val animalName: String,
    val photoUrl: String? = null,
    val received: Double,
    val netForShelter: Double
)

@Serializable
data class ShelterWithdrawalDto(
    val id: String,
    val shelterId: String,
    val amount: Double,
    val status: String,
    val accountHolder: String,
    val documentNumber: String,
    val bankName: String,
    val accountType: String,
    val accountNumber: String,
    val cci: String,
    val currency: String,
    val receiptUrl: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String,
    val processedAt: String? = null
)

@Serializable
data class ShelterWalletDashboardDto(
    val currency: String,
    val mercadoPagoConnected: Boolean = false,
    val mercadoPagoConnectedAt: String? = null,
    val conversionNote: String,
    val totals: ShelterWalletTotalsDto,
    val byAnimal: List<AnimalDonationSummaryDto> = emptyList(),
    val withdrawals: List<ShelterWithdrawalDto>
)

@Serializable
data class WithdrawalRequest(
    val amount: Int,
    val accountHolder: String,
    val documentNumber: String,
    val bankName: String,
    val accountType: String,
    val accountNumber: String,
    val cci: String
)
