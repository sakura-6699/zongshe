package com.example.loan.data.local.db

import androidx.room.TypeConverter
import com.example.loan.domain.model.KycStatus
import com.example.loan.domain.model.LoanStatus
import java.time.LocalDate

class LoanConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLoanStatus(status: LoanStatus?): String? = status?.name

    @TypeConverter
    fun toLoanStatus(name: String?): LoanStatus? = name?.let(LoanStatus::valueOf)

    @TypeConverter
    fun fromKycStatus(status: KycStatus?): String? = status?.name

    @TypeConverter
    fun toKycStatus(name: String?): KycStatus? = name?.let(KycStatus::valueOf)
}
