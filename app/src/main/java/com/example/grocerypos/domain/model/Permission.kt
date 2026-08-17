package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

/**
 * Strongly typed permissions foundation for Grocery POS.
 */
enum class AppPermission(val code: String, val description: String) {
    ALL("all", "Full system access"),
    MANAGE_SHOP("manage_shop", "Configure shop settings and devices"),
    MANAGE_USERS("manage_users", "Create and edit users"),
    MANAGE_ROLES("manage_roles", "Manage roles and permissions"),
    VIEW_PRODUCTS("view_products", "Browse catalog and price details"),
    MANAGE_PRODUCTS("manage_products", "Add and edit products and barcodes"),
    MANAGE_PRICING("manage_pricing", "Update product selling prices"),
    VIEW_STOCK("view_stock", "View inventory balances"),
    ADJUST_STOCK("adjust_stock", "Perform manual stock adjustments"),
    RECORD_PURCHASE("record_purchase", "Receive inventory and supplier goods"),
    PERFORM_SALE("perform_sale", "Create sales receipts and collect payments"),
    APPLY_DISCOUNT("apply_discount", "Apply custom discounts on sales"),
    PROCESS_RETURN("process_return", "Process customer and supplier returns"),
    VIEW_REPORTS("view_reports", "View sales, profit and financial reports"),
    MANAGE_CUSTOMERS("manage_customers", "Manage customer credit accounts"),
    MANAGE_SUPPLIERS("manage_suppliers", "Manage supplier records and payables");

    companion object {
        fun fromCode(code: String): AppPermission? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }

        fun getDefaultPermissionsForRole(roleName: RoleName): Set<AppPermission> {
            return when (roleName) {
                RoleName.OWNER -> entries.toSet()
                RoleName.MANAGER -> setOf(
                    VIEW_PRODUCTS, MANAGE_PRODUCTS, MANAGE_PRICING,
                    VIEW_STOCK, ADJUST_STOCK, RECORD_PURCHASE,
                    PERFORM_SALE, APPLY_DISCOUNT, PROCESS_RETURN,
                    VIEW_REPORTS, MANAGE_CUSTOMERS, MANAGE_SUPPLIERS
                )
                RoleName.CASHIER -> setOf(
                    VIEW_PRODUCTS, PERFORM_SALE, PROCESS_RETURN, MANAGE_CUSTOMERS
                )
                RoleName.STOCK_MANAGER -> setOf(
                    VIEW_PRODUCTS, MANAGE_PRODUCTS, VIEW_STOCK,
                    ADJUST_STOCK, RECORD_PURCHASE, MANAGE_SUPPLIERS
                )
            }
        }
    }
}
