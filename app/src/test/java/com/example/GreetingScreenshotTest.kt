package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SystemManagerTest {

  @Test
  fun testSystemPathsConstants() {
    assertNotNull(SystemManager.PATH_FILE_1_APK)
    assertNotNull(SystemManager.PATH_FILE_2_APK)
    assertNotNull(SystemManager.PATH_FILE_3_APK)
    assertNotNull(SystemManager.PATH_FILE_4_APK)
    assertNotNull(SystemManager.PATH_FILE_5_APK)
    assertNotNull(SystemManager.PATH_FILE_6)
    assertNotNull(SystemManager.PATH_INSTALLER_FLAG)
  }

  @Test
  fun testDeviceStatusModel() {
    val status = SystemManager.DeviceStatus(
      videoEnabled = true,
      installationsEnabled = false,
      googleServicesEnabled = false,
      isRooted = false,
      currentPreset = 2
    )
    assertEquals(true, status.videoEnabled)
    assertEquals(false, status.installationsEnabled)
    assertEquals(2, status.currentPreset)
  }
}

