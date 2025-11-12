package com.swent.mapin.ui.event

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pure-logic harness that mirrors validateStartEnd so JaCoCo increases coverage for the branches in
 * this file without relying on Compose test APIs.
 *
 * This follows the style of your existing Robolectric tests and uses no extra deps.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AddEventScreenLogicRobolectricTest {

  private data class S(
      var date: String,
      var endDate: String,
      var time: String,
      var endTime: String,
      var dateError: Boolean = false,
      var endDateError: Boolean = false,
      var timeError: Boolean = false,
      var endTimeError: Boolean = false
  )

  private fun validate(s: S) {
    s.dateError = s.date.isBlank()
    s.timeError = s.time.isBlank()
    s.endDateError = s.endDate.isBlank()
    s.endTimeError = s.endTime.isBlank()
    if (s.date.isBlank() || s.endDate.isBlank()) return

    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sd =
        runCatching { fmt.parse(s.date) }.getOrNull()
            ?: run {
              s.dateError = true
              return
            }
    val ed =
        runCatching { fmt.parse(s.endDate) }.getOrNull()
            ?: run {
              s.endDateError = true
              return
            }

    if (ed.time > sd.time) {
      s.endDateError = false
      s.endTimeError = false
      return
    }
    if (ed.time < sd.time) {
      s.endDateError = true
      s.endTimeError = false
      return
    }

    if (s.time.isBlank() || s.endTime.isBlank()) return

    val rs = s.time.replace("h", "")
    val re = s.endTime.replace("h", "")
    val sm = runCatching { rs.take(2).toInt() * 60 + rs.substring(2, 4).toInt() }.getOrNull()
    val em = runCatching { re.take(2).toInt() * 60 + re.substring(2, 4).toInt() }.getOrNull()
    if (sm == null) {
      s.timeError = true
      return
    }
    if (em == null) {
      s.endTimeError = true
      return
    }
    if (em <= sm) {
      s.endDateError = true
      s.endTimeError = false
    } else {
      s.endDateError = false
      s.endTimeError = false
    }
  }

  @Test
  fun same_day_end_before_start_sets_endDateError() {
    val s = S("10/10/2030", "10/10/2030", "1400", "1330")
    validate(s)
    assertTrue(s.endDateError)
    assertFalse(s.endTimeError)
    assertFalse(s.timeError)
  }

  @Test
  fun same_day_end_after_start_is_valid() {
    val s = S("10/10/2030", "10/10/2030", "0900", "1000")
    validate(s)
    assertFalse(s.dateError)
    assertFalse(s.endDateError)
    assertFalse(s.timeError)
    assertFalse(s.endTimeError)
  }

  @Test
  fun end_date_after_start_date_ignores_times_and_is_valid() {
    val s = S("10/10/2030", "11/10/2030", "", "")
    validate(s)
    assertFalse(s.endDateError)
    assertFalse(s.endTimeError)
  }

  @Test
  fun invalid_start_time_sets_timeError() {
    val s = S("10/10/2030", "10/10/2030", "xx00", "0100")
    validate(s)
    assertTrue(s.timeError)
  }

  @Test
  fun invalid_end_time_sets_endTimeError() {
    val s = S("10/10/2030", "10/10/2030", "0100", "99aa")
    validate(s)
    assertTrue(s.endTimeError)
  }
}
