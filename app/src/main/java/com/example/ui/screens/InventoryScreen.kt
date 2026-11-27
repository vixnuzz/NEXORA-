package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InventoryScreen(
    products: List<InventoryProduct>,
    purchaseOrders: List<PurchaseOrder>,
    currency: String,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertProduct: (InventoryProduct) -> Unit,
    onDeleteProduct: (InventoryProduct) -> Unit,
    onUpsertPurchaseOrder: (PurchaseOrder) -> Unit,
    onDeletePurchaseOrder: (PurchaseOrder) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<InventoryProduct?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showPoDialog by remember { mutableStateOf(false) }

    val lowStockCount = remember(products) { products.count { it.needsRestock } }
    val filteredProducts = remember(products, searchQuery) {
        products.filter { p ->
            searchQuery.isBlank() ||
                p.name.contains(searchQuery, true) ||
                p.sku.contains(searchQuery, true) ||
                p.supplierName.contains(searchQuery, true)
        }
    }
    val filteredPos = remember(purchaseOrders, searchQuery) {
        purchaseOrders.filter { po ->
            searchQuery.isBlank() ||
                po.poNumber.contains(searchQuery, true) ||
                po.productName.contains(searchQuery, true) ||
                po.supplierName.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Inventory & Procurement",
            subtitle = "${products.size} SKUs • $lowStockCount low-stock",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = if (selectedTab == 0) "Add Product" else "New PO",
            addTag = "inventory_add_button",
            onAddClick = {
                if (selectedTab == 0) {
                    editingProduct = null; showProductDialog = true
                } else {
                    showPoDialog = true
                }
            },
            onExportCsv = {
                if (selectedTab == 0) {
                    onExportCsv(
                        "inventory_products",
                        listOf("SKU", "Name", "Category", "Supplier", "UnitPrice", "StockQty", "ReorderLevel", "LastMovement"),
                        filteredProducts.map { listOf(it.sku, it.name, it.category, it.supplierName, it.unitPrice.toString(), it.stockQty.toString(), it.reorderLevel.toString(), it.lastMovementNote) }
                    )
                } else {
                    onExportCsv(
                        "purchase_orders",
                        listOf("PONumber", "Supplier", "Product", "Quantity", "TotalCost", "Status"),
                        filteredPos.map { listOf(it.poNumber, it.supplierName, it.productName, it.quantity.toString(), it.totalCost.toString(), it.status) }
                    )
                }
            }
        )

        if (lowStockCount > 0) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StatusWarning.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = StatusWarning)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "$lowStockCount product(s) are at or below reorder threshold. Create a Purchase Order to replenish.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stock Catalog (${products.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Purchase Orders (${purchaseOrders.size})") })
        }

        if (selectedTab == 0) {
            if (filteredProducts.isEmpty()) {
                EmptyModuleCard(
                    title = "No Inventory Products",
                    description = "Track SKUs, stock levels, suppliers, reorder alerts, and stock movement history.",
                    icon = Icons.Filled.Inventory2,
                    actionLabel = if (canEdit) "Add Product" else null,
                    onAction = if (canEdit) { { editingProduct = null; showProductDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${prod.sku} • ${prod.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(prod.name, style = MaterialTheme.typography.titleMedium)
                                        Text("Supplier: ${prod.supplierName} • ${formatMoney(prod.unitPrice, currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        if (prod.needsRestock) "LOW: ${prod.stockQty}" else "IN STOCK: ${prod.stockQty}",
                                        if (prod.needsRestock) StatusWarning else NexoraEmerald
                                    )
                                }
                                if (prod.lastMovementNote.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Movement: ${prod.lastMovementNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (canEdit) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                if (prod.stockQty > 0) {
                                                    onUpsertProduct(
                                                        prod.copy(
                                                            stockQty = prod.stockQty - 1,
                                                            lastMovementNote = "Issued -1 unit (${prod.stockQty - 1} left)"
                                                        )
                                                    )
                                                }
                                            }
                                        ) { Text("-1") }
                                        OutlinedButton(
                                            onClick = {
                                                onUpsertProduct(
                                                    prod.copy(
                                                        stockQty = prod.stockQty + 5,
                                                        lastMovementNote = "Restocked +5 units (${prod.stockQty + 5} total)"
                                                    )
                                                )
                                            }
                                        ) { Text("+5") }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { editingProduct = prod; showProductDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit product")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteProduct(prod) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredPos.isEmpty()) {
                EmptyModuleCard(
                    title = "No Purchase Orders",
                    description = "Create supplier purchase orders. Receiving a PO automatically updates catalog stock.",
                    icon = Icons.Filled.LocalShipping,
                    actionLabel = if (canEdit) "Create PO" else null,
                    onAction = if (canEdit) { { showPoDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredPos, key = { it.id }) { po ->
                        val st = po.statusEnum()
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(po.poNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text("${po.productName} (x${po.quantity})", style = MaterialTheme.typography.titleMedium)
                                        Text("${po.supplierName} • ${formatMoney(po.totalCost, currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        st.label,
                                        when (st) {
                                            PoStatus.RECEIVED -> NexoraEmerald
                                            PoStatus.ORDERED -> StatusWarning
                                            PoStatus.CANCELLED -> StatusError
                                        }
                                    )
                                }
                                if (canEdit) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        if (st == PoStatus.ORDERED) {
                                            FilledTonalButton(onClick = { onUpsertPurchaseOrder(po.copy(status = PoStatus.RECEIVED.name)) }) {
                                                Text("Mark Received (+Stock)")
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (canDelete) {
                                            IconButton(onClick = { onDeletePurchaseOrder(po) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete PO", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        ProductFormDialog(editingProduct, { showProductDialog = false }, onUpsertProduct)
    }
    if (showPoDialog) {
        PurchaseOrderFormDialog(products, { showPoDialog = false }, onUpsertPurchaseOrder)
    }
}
