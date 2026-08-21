package com.example.grocerypos.presentation.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Bottom Sheet for payment processing supporting Full Payment, Tendered Cash with Change,
 * Partial Payment, Multi-tender Split Payment, and Khata/Credit with Credit Limit Warnings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    grandTotal: Money,
    selectedCustomer: Customer?,
    selectedCustomerBalance: Money,
    isCompletingSale: Boolean,
    onCompleteSale: (payments: List<PaymentLineUi>, tenderedAmount: Money) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Payments list for split tendering
    var payments by remember {
        mutableStateOf<List<PaymentLineUi>>(
            listOf(
                PaymentLineUi(
                    id = UUID.randomUUID().toString(),
                    method = PaymentMethod.CASH,
                    amount = grandTotal
                )
            )
        )
    }

    // Tendered cash for change calculation (only applicable for Cash payment)
    var cashTenderedText by remember { mutableStateOf("") }
    var selectedMethodForAdd by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountForAddText by remember { mutableStateOf("") }
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    val totalPaid = payments.fold(Money.ZERO) { acc, p -> acc + p.amount }
    val remainingDue = if (grandTotal > totalPaid) grandTotal - totalPaid else Money.ZERO
    val cashTenderedAmount = if (cashTenderedText.isNotBlank()) {
        val rupees = cashTenderedText.toLongOrNull() ?: 0L
        Money.fromRupees(rupees)
    } else {
        Money.ZERO
    }

    val changeAmount = if (cashTenderedAmount > totalPaid) {
        cashTenderedAmount - totalPaid
    } else {
        Money.ZERO
    }

    val proposedNewBalance = selectedCustomerBalance + remainingDue
    val isOverCreditLimit = selectedCustomer != null &&
            selectedCustomer.creditLimit.isPositive() &&
            proposedNewBalance > selectedCustomer.creditLimit

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment Checkout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedCustomer != null) "Customer: ${selectedCustomer.name}" else "Customer: Walk-in (Cash only)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grand Total Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL PAYABLE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = CurrencyFormatter.formatPkr(grandTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PAID: ${CurrencyFormatter.formatPkr(totalPaid)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (remainingDue.isPositive()) {
                            Text(
                                text = "DUE: ${CurrencyFormatter.formatPkr(remainingDue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "FULLY PAID",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Tender Buttons for Cash
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        payments = listOf(PaymentLineUi(UUID.randomUUID().toString(), PaymentMethod.CASH, grandTotal))
                        cashTenderedText = ""
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Exact Cash", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (selectedCustomer != null) {
                    OutlinedButton(
                        onClick = {
                            payments = emptyList() // All credit
                            cashTenderedText = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Full Khata/Credit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = { showAddPaymentDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Split", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Lines List
            Text(
                text = "Applied Payments (${payments.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (payments.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No payments added. Entire Rs. ${CurrencyFormatter.formatPkr(grandTotal, false)} will be charged to customer Khata (Credit).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(payments, key = { it.id }) { payLine ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getPaymentIcon(payLine.method),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = payLine.method.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyFormatter.formatPkr(payLine.amount),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            payments = payments.filterNot { it.id == payLine.id }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cash Tendered & Change Section
            if (payments.any { it.method == PaymentMethod.CASH }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cashTenderedText,
                        onValueChange = { cashTenderedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cash Tendered (Rs.)") },
                        placeholder = { Text("Enter received cash") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Change Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = CurrencyFormatter.formatPkr(changeAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Customer Credit / Khata Status Banner
            if (remainingDue.isPositive()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedCustomer == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Walk-in customers cannot have credit/due amounts. Please select a registered customer or pay the full amount.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOverCreditLimit) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOverCreditLimit) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isOverCreditLimit) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Credit to Customer: ${CurrencyFormatter.formatPkr(remainingDue)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "New Balance: ${CurrencyFormatter.formatPkr(proposedNewBalance)} | Limit: ${CurrencyFormatter.formatPkr(selectedCustomer.creditLimit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverCreditLimit) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isOverCreditLimit) {
                                    Text(
                                        text = "Notice: This sale will exceed the customer's credit limit.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Complete Sale
            val canComplete = (remainingDue.isZero() || selectedCustomer != null) && !isCompletingSale
            Button(
                onClick = {
                    val effectiveTendered = if (cashTenderedAmount > Money.ZERO) cashTenderedAmount else totalPaid
                    onCompleteSale(payments, effectiveTendered)
                },
                enabled = canComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isCompletingSale) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPLETE SALE (${CurrencyFormatter.formatPkr(grandTotal)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Add Payment Dialog (for split payments)
    if (showAddPaymentDialog) {
        val methods = PaymentMethod.values().toList()
        var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
        var splitAmountText by remember { mutableStateOf(if (remainingDue.isPositive()) (remainingDue.amountInMinorUnits / 100).toString() else "") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddPaymentDialog = false },
            title = { Text("Add Split Payment Line", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select Payment Method:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.take(3).forEach { method ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMethod = method },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = method.name,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.drop(3).forEach { method ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMethod = method },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = method.name.replace("_", " "),
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = splitAmountText,
                        onValueChange = { splitAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (Rs.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rupees = splitAmountText.toLongOrNull() ?: 0L
                        if (rupees > 0) {
                            payments = payments + PaymentLineUi(
                                id = UUID.randomUUID().toString(),
                                method = selectedMethod,
                                amount = Money.fromRupees(rupees)
                            )
                        }
                        showAddPaymentDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Receipt / Sale Success Dialog.
 */
@Composable
fun SaleSuccessDialog(
    successData: SaleSuccessUi,
    onDismiss: () -> Unit
) {
    val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sale Completed",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Invoice #${successData.invoiceNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeFormat.format(Date(successData.completedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!successData.customerName.isNullOrBlank()) {
                    Text(
                        text = "Customer: ${successData.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grand Total:", fontWeight = FontWeight.Bold)
                    Text(CurrencyFormatter.formatPkr(successData.grandTotal), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paid Amount:")
                    Text(CurrencyFormatter.formatPkr(successData.paidAmount))
                }

                if (successData.dueAmount.isPositive()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Khata Due:", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text(CurrencyFormatter.formatPkr(successData.dueAmount), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }

                if (successData.changeReturned.isPositive()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Change Returned:", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Text(CurrencyFormatter.formatPkr(successData.changeReturned), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }

                if (successData.paymentBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Payment Tender Breakdown:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    successData.paymentBreakdown.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${p.method.name}:", style = MaterialTheme.typography.bodySmall)
                            Text(CurrencyFormatter.formatPkr(p.amount), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Next Sale")
            }
        }
    )
}

fun getPaymentIcon(method: PaymentMethod): ImageVector {
    return when (method) {
        PaymentMethod.CASH -> Icons.Default.Money
        PaymentMethod.JAZZCASH, PaymentMethod.EASYPAISA -> Icons.Default.PhoneAndroid
        PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
        PaymentMethod.CARD -> Icons.Default.CreditCard
        PaymentMethod.OTHER -> Icons.Default.Payments
    }
}
