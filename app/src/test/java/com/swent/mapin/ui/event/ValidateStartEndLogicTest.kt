package com.swent.mapin.ui.event

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.*
import org.junit.Test

class ValidateStartEndLogicTest {

  @Test
  fun `invalid start date sets dateError`() {
    val date = mutableStateOf("invalid")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("")
    val endTime = mutableStateOf("")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertTrue(dateError.value)
  }

  @Test
  fun `invalid end date sets endDateError`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("bad")
    val time = mutableStateOf("")
    val endTime = mutableStateOf("")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertTrue(endDateError.value)
  }

  @Test
  fun `end date after start clears end errors`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("02/01/2026")
    val time = mutableStateOf("")
    val endTime = mutableStateOf("")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(true)
    val timeError = mutableStateOf(true)
    val endTimeError = mutableStateOf(true)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertFalse(endDateError.value)
    assertFalse(endTimeError.value)
  }

  @Test
  fun `end date before start sets endDateError`() {
    val date = mutableStateOf("02/01/2026")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("")
    val endTime = mutableStateOf("")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertTrue(endDateError.value)
    assertFalse(endTimeError.value)
  }

  @Test
  fun `equal dates missing times leaves presence errors`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("")
    val endTime = mutableStateOf("")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertTrue(timeError.value)
    assertTrue(endTimeError.value)
  }

  @Test
  fun `equal dates invalid time formats set time errors`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("aa")
    val endTime = mutableStateOf("bb")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    // startMinutes parsing fails -> timeError true
    assertTrue(timeError.value)
  }

  @Test
  fun `equal dates end before or equal start marks endDateError`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("1200")
    val endTime = mutableStateOf("1200")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertTrue(endDateError.value)
    assertFalse(endTimeError.value)
  }

  @Test
  fun `equal dates end after start clears errors`() {
    val date = mutableStateOf("01/01/2026")
    val endDate = mutableStateOf("01/01/2026")
    val time = mutableStateOf("0900")
    val endTime = mutableStateOf("1000")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(true)
    val timeError = mutableStateOf(true)
    val endTimeError = mutableStateOf(true)

    validateStartEndLogic(
        date, time, endDate, endTime, dateError, endDateError, timeError, endTimeError)

    assertFalse(endDateError.value)
    assertFalse(endTimeError.value)
  }
}
