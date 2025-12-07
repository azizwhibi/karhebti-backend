package com.example.karhebti_android.util

import com.example.karhebti_android.data.api.MaintenanceExtendedResponse
import java.util.Date

// Return the best available date for an extended maintenance record.
// Some backend responses historically include `date` but may omit `dueAt` at runtime
// (despite the API model). This helper centralizes safe access and prevents NPEs.
fun MaintenanceExtendedResponse.effectiveDateSafe(): Date? {
    return try { this.dueAt } catch (_: Throwable) { null } ?: try { this.date } catch (_: Throwable) { null }
}

