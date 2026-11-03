package com.menash.myself.core.time

import java.time.LocalDate
import java.time.ZoneId

internal fun defaultToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Jerusalem"))
