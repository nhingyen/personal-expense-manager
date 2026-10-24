package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.YearMonth

class CalculateMonthlySummaryUseCase {

    operator fun invoke(
        transactions: List<Transaction>,
        month: YearMonth
    ) : MonthlySummary {
        var incomeVnd = 0L
        var expenseVnd = 0L

        for (transaction in transactions) {

            if(YearMonth.from(transaction.date) != month) {
                continue
            }

            when (transaction.type) {
                TransactionType.INCOME -> {
                    incomeVnd = Math.addExact(
                        incomeVnd,
                        transaction.amountVnd
                    )
                }
                TransactionType.EXPENSE -> {
                    expenseVnd = Math.addExact(
                        expenseVnd,
                        transaction.amountVnd
                    )
                }
            }
        }
        val netVnd = Math.subtractExact(
            incomeVnd,
            expenseVnd
        )

        return MonthlySummary(
            month = month,
            incomeVnd = incomeVnd,
            expenseVnd = expenseVnd,
            netVnd = netVnd
        )
    }
}