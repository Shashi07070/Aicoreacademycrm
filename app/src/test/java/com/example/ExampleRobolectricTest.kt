package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdminAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tuition CRM", appName)
  }

  @Test
  fun `default admin credentials unlock successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = AdminAuthManager(context)

    assertTrue(authManager.isLocked.value)
    
    // Test wrong credentials
    val wrongResult = authManager.authenticate("wrong_user", "wrong_pass")
    assertFalse(wrongResult)
    assertTrue(authManager.isLocked.value)

    // Test correct credentials (admin1 / masterkey786)
    val success = authManager.authenticate("admin1", "masterkey786")
    assertTrue(success)
    assertFalse(authManager.isLocked.value)

    // Lock again
    authManager.lock()
    assertTrue(authManager.isLocked.value)
  }
}

