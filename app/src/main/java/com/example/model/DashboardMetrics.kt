package com.example.model

data class MonthlyMetricPoint(
    val monthLabel: String,
    val salesAmount: Double,
    val expenseAmount: Double
)

data class DashboardMetrics(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val activeCustomers: Int = 0,
    val totalEmployees: Int = 0,
    val pendingInvoicesCount: Int = 0,
    val pendingInvoicesTotal: Double = 0.0,
    val activeProjects: Int = 0,
    val lowStockCount: Int = 0,
    val openTicketsCount: Int = 0,
    val pipelineValue: Double = 0.0,
    val monthlySeries: List<MonthlyMetricPoint> = emptyList(),
    val recentTransactions: List<FinanceRecord> = emptyList(),
    val recentActivities: List<ActivityLog> = emptyList(),
    val upcomingTasks: List<ProjectTask> = emptyList(),
    val unreadNotifications: Int = 0
)
