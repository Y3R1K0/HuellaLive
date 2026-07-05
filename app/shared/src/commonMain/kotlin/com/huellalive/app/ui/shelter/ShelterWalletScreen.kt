package com.huellalive.app.ui.shelter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.model.ShelterWalletDashboardDto
import com.huellalive.app.data.model.WithdrawalRequest
import com.huellalive.app.data.repository.WalletRepository
import com.huellalive.app.ui.components.HuellaMessageSnackbar
import com.huellalive.app.ui.theme.Background
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Error
import com.huellalive.app.ui.theme.InfoBlue
import com.huellalive.app.ui.theme.Success
import com.huellalive.app.ui.theme.Surface
import com.huellalive.app.ui.theme.TextOnAccent
import com.huellalive.app.ui.theme.TextPrimary
import com.huellalive.app.ui.theme.TextSecondary
import com.huellalive.app.ui.theme.Warning
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class ShelterWalletUiState(
    val dashboard: ShelterWalletDashboardDto? = null,
    val loading: Boolean = false,
    val message: String? = null
)

private class ShelterWalletViewModel(
    private val repository: WalletRepository
) : ScreenModel {
    private val _state = MutableStateFlow(ShelterWalletUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (val result = repository.getShelterDashboard()) {
                is Resource.Success -> _state.value = ShelterWalletUiState(dashboard = result.data)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, message = result.message)
                else -> Unit
            }
        }
    }

    fun requestWithdrawal(request: WithdrawalRequest, onSuccess: () -> Unit) {
        screenModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (val result = repository.requestWithdrawal(request)) {
                is Resource.Success -> {
                    onSuccess()
                    _state.value = _state.value.copy(message = "Solicitud de retiro enviada")
                    load()
                }
                is Resource.Error -> _state.value = _state.value.copy(loading = false, message = result.message)
                else -> Unit
            }
        }
    }

    fun connectMercadoPago(openUrl: (String) -> Unit) {
        screenModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (val result = repository.getMercadoPagoConnectUrl()) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = if (result.data.connected) {
                            "Mercado Pago ya esta conectado. Puedes actualizar la conexion si lo necesitas."
                        } else {
                            "Te llevamos a Mercado Pago para conectar tu cuenta"
                        }
                    )
                    openUrl(result.data.url)
                }
                is Resource.Error -> _state.value = _state.value.copy(loading = false, message = result.message)
                else -> Unit
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}

class ShelterWalletScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<WalletRepository>()
        val viewModel = rememberScreenModel { ShelterWalletViewModel(repository) }
        val state by viewModel.state.collectAsState()
        val uriHandler = LocalUriHandler.current
        var showWithdrawal by remember { mutableStateOf(false) }

        Box(
            Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Donaciones recibidas", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Resumen interno del albergue", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = viewModel::load) {
                            Icon(Icons.Default.Refresh, "Actualizar", tint = DustyRose)
                        }
                    }
                }

                if (state.loading && state.dashboard == null) {
                    item {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DustyRose)
                        }
                    }
                }

                state.dashboard?.let { dashboard ->
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = DustyRose,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("Saldo disponible", color = TextOnAccent.copy(alpha = 0.82f))
                                    Text(
                                        formatMoney(dashboard.totals.availableToWithdraw),
                                        color = TextOnAccent,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { showWithdrawal = true },
                                        enabled = dashboard.totals.availableToWithdraw >= 1,
                                        colors = ButtonDefaults.buttonColors(containerColor = Background, contentColor = TextPrimary),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalance, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Solicitar retiro")
                                    }
                                }
                            }

                            MercadoPagoConnectionCard(
                                connected = dashboard.mercadoPagoConnected,
                                onClick = {
                                    viewModel.connectMercadoPago { url ->
                                        uriHandler.openUri(url)
                                    }
                                }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WalletMetric("Total recibido", dashboard.totals.received, Modifier.weight(1f))
                                WalletMetric("Saldo neto", dashboard.totals.netForShelter, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WalletMetric("Donaciones", dashboard.totals.direct, Modifier.weight(1f))
                                WalletMetric("Ya retirado", dashboard.totals.withdrawn, Modifier.weight(1f))
                            }
                            Text(
                                "La comision de mantenimiento ya esta descontada del saldo disponible. ${dashboard.conversionNote}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (dashboard.withdrawals.isNotEmpty()) {
                        item {
                            Text(
                                "Retiros",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(dashboard.withdrawals) { withdrawal ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                color = Surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, null, tint = DustyRose)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(formatMoney(withdrawal.amount), color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(withdrawal.bankName, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(withdrawal.status.withdrawalLabel(), color = withdrawal.status.withdrawalColor(), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (showWithdrawal) {
                WithdrawalDialog(
                    available = state.dashboard?.totals?.availableToWithdraw?.toInt() ?: 0,
                    loading = state.loading,
                    onDismiss = { showWithdrawal = false },
                    onSubmit = { viewModel.requestWithdrawal(it) { showWithdrawal = false } }
                )
            }

            HuellaMessageSnackbar(
                message = state.message,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun WalletMetric(label: String, value: Double, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text(formatMoney(value), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Soles", color = DustyRose, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MercadoPagoConnectionCard(
    connected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (connected) Success.copy(alpha = 0.12f) else Surface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (connected) Icons.Default.Verified else Icons.Default.Storefront,
                contentDescription = null,
                tint = if (connected) Success else DustyRose
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (connected) "Mercado Pago conectado" else "Mercado Pago marketplace",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (connected) "Este albergue ya puede recibir donaciones." else "Conecta tu cuenta para recibir donaciones.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (connected) "Actualizar" else "Conectar", color = if (connected) Success else DustyRose)
            }
        }
    }
}

@Composable
private fun WithdrawalDialog(
    available: Int,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (WithdrawalRequest) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var document by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Ahorros") }
    var accountNumber by remember { mutableStateOf("") }
    var cci by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Solicitar retiro") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Disponible: ${formatMoney(available.toDouble())}", color = DustyRose, fontWeight = FontWeight.Bold) }
                item { WithdrawalField("Monto en soles", amount) { amount = it.filter(Char::isDigit) } }
                item { WithdrawalField("Titular de la cuenta", holder) { holder = it } }
                item { WithdrawalField("DNI o RUC", document) { document = it.filter(Char::isDigit) } }
                item { WithdrawalField("Banco", bank) { bank = it } }
                item { WithdrawalField("Tipo de cuenta", accountType) { accountType = it } }
                item { WithdrawalField("Numero de cuenta", accountNumber) { accountNumber = it.filter(Char::isDigit) } }
                item { WithdrawalField("CCI", cci) { cci = it.filter(Char::isDigit) } }
                item {
                    Text(
                        "La solicitud sera revisada antes de realizar la transferencia bancaria.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(WithdrawalRequest(amount.toIntOrNull() ?: 0, holder, document, bank, accountType, accountNumber, cci))
                },
                enabled = !loading && amount.toIntOrNull() in 1..available &&
                    listOf(holder, document, bank, accountType, accountNumber, cci).all { it.isNotBlank() }
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Enviar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancelar") } }
    )
}

@Composable
private fun WithdrawalField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun String.withdrawalLabel() = when (this) {
    "PAID" -> "Pagado"
    "APPROVED" -> "Aprobado"
    "REJECTED" -> "Rechazado"
    else -> "Pendiente"
}

private fun String.withdrawalColor() = when (this) {
    "PAID" -> Success
    "APPROVED" -> InfoBlue
    "REJECTED" -> Error
    else -> Warning
}

private fun formatMoney(value: Double): String = "S/ ${value.toInt()}"
