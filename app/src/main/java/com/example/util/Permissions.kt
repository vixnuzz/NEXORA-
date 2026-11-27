package com.example.util

import com.example.model.UserRole

enum class AppModule(val route: String, val title: String) {
    DASHBOARD("dashboard", "Dashboard"),
    CRM("crm", "CRM & Pipeline"),
    FINANCE("finance", "Finance & P&L"),
    HR("hr", "HR Management"),
    PROJECTS("projects", "Projects & Kanban"),
    INVENTORY("inventory", "Inventory & POs"),
    SALES("sales", "Sales Orders"),
    MARKETING("marketing", "Marketing"),
    SUPPORT("support", "Support Desk"),
    AI_ASSISTANT("ai_assistant", "AI Copilot"),
    NOTIFICATIONS("notifications", "Activity & Alerts"),
    SETTINGS("settings", "Org & Profile")
}

object RolePermissions {
    fun accessibleModules(role: UserRole): Set<AppModule> = when (role) {
        UserRole.SUPER_ADMIN, UserRole.BUSINESS_OWNER -> AppModule.entries.toSet()
        UserRole.MANAGER -> setOf(
            AppModule.DASHBOARD, AppModule.CRM, AppModule.FINANCE, AppModule.HR,
            AppModule.PROJECTS, AppModule.INVENTORY, AppModule.SALES, AppModule.MARKETING,
            AppModule.SUPPORT, AppModule.AI_ASSISTANT, AppModule.NOTIFICATIONS, AppModule.SETTINGS
        )
        UserRole.ACCOUNTANT -> setOf(
            AppModule.DASHBOARD, AppModule.FINANCE, AppModule.SALES, AppModule.INVENTORY,
            AppModule.AI_ASSISTANT, AppModule.NOTIFICATIONS, AppModule.SETTINGS
        )
        UserRole.SALES_REP -> setOf(
            AppModule.DASHBOARD, AppModule.CRM, AppModule.SALES, AppModule.MARKETING,
            AppModule.SUPPORT, AppModule.AI_ASSISTANT, AppModule.NOTIFICATIONS, AppModule.SETTINGS
        )
        UserRole.EMPLOYEE -> setOf(
            AppModule.DASHBOARD, AppModule.PROJECTS, AppModule.SUPPORT,
            AppModule.AI_ASSISTANT, AppModule.NOTIFICATIONS, AppModule.SETTINGS
        )
    }

    fun canCreateOrEdit(role: UserRole, module: AppModule): Boolean = when (role) {
        UserRole.SUPER_ADMIN, UserRole.BUSINESS_OWNER -> true
        UserRole.MANAGER -> module != AppModule.SETTINGS
        UserRole.ACCOUNTANT -> module in setOf(AppModule.FINANCE, AppModule.SALES, AppModule.INVENTORY)
        UserRole.SALES_REP -> module in setOf(AppModule.CRM, AppModule.SALES, AppModule.MARKETING, AppModule.SUPPORT)
        UserRole.EMPLOYEE -> module in setOf(AppModule.PROJECTS, AppModule.SUPPORT)
    }

    fun canDelete(role: UserRole, module: AppModule): Boolean = when (role) {
        UserRole.SUPER_ADMIN, UserRole.BUSINESS_OWNER -> true
        UserRole.MANAGER -> module !in setOf(AppModule.FINANCE, AppModule.HR, AppModule.SETTINGS)
        UserRole.ACCOUNTANT -> module == AppModule.FINANCE
        UserRole.SALES_REP -> module == AppModule.CRM
        UserRole.EMPLOYEE -> false
    }

    fun canManageOrg(role: UserRole): Boolean =
        role == UserRole.SUPER_ADMIN || role == UserRole.BUSINESS_OWNER

    fun canExportCsv(role: UserRole): Boolean =
        role != UserRole.EMPLOYEE

    fun summaryFor(role: UserRole): String = when (role) {
        UserRole.SUPER_ADMIN -> "Full platform control across all organizations, modules, exports, and security."
        UserRole.BUSINESS_OWNER -> "Full organization ownership, financials, HR payroll, settings, and exports."
        UserRole.MANAGER -> "Operational write access across CRM, HR, Projects, Inventory, Sales, and Support."
        UserRole.ACCOUNTANT -> "Finance ledger, invoices, P&L, sales reconciliation, inventory valuation, and exports."
        UserRole.SALES_REP -> "CRM leads & contacts, sales orders, marketing campaigns, and customer support."
        UserRole.EMPLOYEE -> "Projects & Kanban tasks, support desk collaboration, AI Copilot, and personal profile."
    }
}
