package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.DataOutputStream
import java.io.File

/**
 * Handles system-level operations, file integrity validation, and custom firmware parameter remapping.
 * Supports a fully functional local Simulation Mode for non-rooted or sandbox development preview testing,
 * while executing real shell commands securely on real rooted devices.
 */
object SystemManager {
    private const val TAG = "SystemManager"
    private const val PREFS_NAME = "KosherPlayerPrefs"
    
    // Preference keys
    private const val KEY_SIM_MODE = "simulation_mode"
    private const val KEY_PASSWORD = "management_password"
    private const val KEY_THEME = "app_theme_mode" // -1=system, 0=light, 1=dark
    
    // Keys for simulation states
    private const val KEY_SIM_FILE_1 = "sim_file_1" // app5
    private const val KEY_SIM_FILE_2 = "sim_file_2" // app4
    private const val KEY_SIM_FILE_3 = "sim_file_3" // app2
    private const val KEY_SIM_FILE_4 = "sim_file_4" // FilesGoogle
    private const val KEY_SIM_FILE_5 = "sim_file_5" // GmsCore
    private const val KEY_SIM_FILE_6 = "sim_file_6" // current package installer status (from 7 or 8)

    // Real system paths defined under Android system specifications
    const val PATH_FILE_1_APK = "/system/preinstall/app5.apk"
    const val PATH_FILE_1_APKR = "/system/preinstall/app5.apkr"
    
    const val PATH_FILE_2_APK = "/system/preinstall/app4.apk"
    const val PATH_FILE_2_APKR = "/system/preinstall/app4.apkr"
    
    const val PATH_FILE_3_APK = "/system/preinstall/app2.apk"
    const val PATH_FILE_3_APKR = "/system/preinstall/app2.apkr"
    
    const val PATH_FILE_4_APK = "/system/product/priv-app/FilesGoogle/FilesGoogle.apk"
    const val PATH_FILE_4_APKR = "/system/product/priv-app/FilesGoogle/FilesGoogle.apkr"
    
    const val PATH_FILE_5_APK = "/system/product/priv-app/GmsCore/GmsCore.apk"
    const val PATH_FILE_5_APKR = "/system/product/priv-app/GmsCore/GmsCore.apkr"
    
    const val PATH_FILE_6 = "/system/priv-app/GooglePackageInstaller/GooglePackageInstaller.apk"
    const val PATH_FILE_7 = "/system/apps/x/googlepackageinstaller.apk"
    const val PATH_FILE_8 = "/system/apps/y/googlepackageinstaller.apk"
    
    const val PATH_INSTALLER_FLAG = "/system/priv-app/GooglePackageInstaller/installs_allowed.flag"

    // Representation of current system version state
    data class DeviceStatus(
        val videoEnabled: Boolean,
        val installationsEnabled: Boolean,
        val googleServicesEnabled: Boolean,
        val isRooted: Boolean,
        val currentPreset: Int // 1: With Video + Inst, 2: With Video, No Inst, 3: No Video, No Inst, -1: Custom/Drifted
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Set up defaults for simulator if not already initialized.
     */
    fun initializeDefaults(context: Context) {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_SIM_MODE)) {
            // Check if we are running in a root environment. If not, auto-enable simulation.
            val rootAvailable = checkRootMethod()
            val edit = prefs.edit()
            edit.putBoolean(KEY_SIM_MODE, !rootAvailable)
            
            // Set initial simulation files to Choice 1 with Google Services enabled
            edit.putString(KEY_SIM_FILE_1, "apkr") // app5 disabled
            edit.putString(KEY_SIM_FILE_2, "apk")  // app4 enabled (video)
            edit.putString(KEY_SIM_FILE_3, "apk")  // app2 enabled
            edit.putString(KEY_SIM_FILE_4, "apk")  // Google Files enabled
            edit.putString(KEY_SIM_FILE_5, "apk")  // Google services enabled
            edit.putString(KEY_SIM_FILE_6, "original") // PackageInstaller copied from 7
            edit.apply()
        }
    }

    private fun checkRootMethod(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun requestRootPermission(): Boolean {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            os.close()
            return process.waitFor() == 0
        } catch (e: Exception) {
            return false
        }
    }

    fun isSimulationMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SIM_MODE, true)
    }

    fun setSimulationMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SIM_MODE, enabled).apply()
    }

    fun getPassword(context: Context): String? {
        return getPrefs(context).getString(KEY_PASSWORD, null)
    }

    fun setPassword(context: Context, newPass: String) {
        getPrefs(context).edit().putString(KEY_PASSWORD, newPass).commit()
    }
    
    fun getThemeMode(context: Context): Int {
        return getPrefs(context).getInt(KEY_THEME, -1)
    }
    
    fun setThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_THEME, mode).apply()
    }

    /**
     * Evaluates files states and resolves current system configuration
     */
    fun fetchDeviceStatus(context: Context): DeviceStatus {
        val isSim = isSimulationMode(context)
        val isRooted = checkRootMethod()

        if (isSim) {
            val prefs = getPrefs(context)
            val file1 = prefs.getString(KEY_SIM_FILE_1, "apkr") // app5
            val file2 = prefs.getString(KEY_SIM_FILE_2, "apk")  // app4
            val file3 = prefs.getString(KEY_SIM_FILE_3, "apk")  // app2
            val file4 = prefs.getString(KEY_SIM_FILE_4, "apk")  // FilesGoogle
            val file5 = prefs.getString(KEY_SIM_FILE_5, "apk")  // GmsCore
            val file6 = prefs.getString(KEY_SIM_FILE_6, "original")  // installer

            val videoEnabled = (file2 == "apk")
            val installationsEnabled = (file6 == "original")
            val googleServicesEnabled = (file5 == "apk")

            // Determine active option preset
            val currentPreset = when {
                videoEnabled && installationsEnabled && file1 == "apkr" && file3 == "apk" && file4 == "apk" -> 1
                videoEnabled && !installationsEnabled && file1 == "apkr" && file3 == "apk" && file4 == "apk" -> 2
                !videoEnabled && !installationsEnabled && file1 == "apk" && file3 == "apkr" && file4 == "apkr" -> 3
                else -> -1 // Custom/Unsynchronized state
            }

            return DeviceStatus(
                videoEnabled = videoEnabled,
                installationsEnabled = installationsEnabled,
                googleServicesEnabled = googleServicesEnabled,
                isRooted = isRooted,
                currentPreset = currentPreset
            )
        } else {
            // Real physical system checks
            val file1ApkExists = File(PATH_FILE_1_APK).exists()
            val file1ApkrExists = File(PATH_FILE_1_APKR).exists()
            
            val file2ApkExists = File(PATH_FILE_2_APK).exists()
            val file2ApkrExists = File(PATH_FILE_2_APKR).exists()
            
            val file3ApkExists = File(PATH_FILE_3_APK).exists()
            val file3ApkrExists = File(PATH_FILE_3_APKR).exists()
            
            val file4ApkExists = File(PATH_FILE_4_APK).exists()
            val file4ApkrExists = File(PATH_FILE_4_APKR).exists()
            
            val file5ApkExists = File(PATH_FILE_5_APK).exists()
            val file5ApkrExists = File(PATH_FILE_5_APKR).exists()

            // Calculate state
            val videoEnabled = file2ApkExists && !file2ApkrExists
            // We read package installer flag or trust our tracked configuration
            val flagFile = File(PATH_INSTALLER_FLAG)
            
            val installationsEnabled = if (flagFile.exists()) {
                true
            } else if (File(PATH_FILE_8).exists() && File(PATH_FILE_6).exists() && File(PATH_FILE_6).length() == File(PATH_FILE_8).length()) {
                false
            } else {
                // If files do not exist or we can't tell, fall back to default (true/approved in initial state)
                true
            }

            val googleServicesEnabled = file5ApkExists && !file5ApkrExists

            val currentPreset = when {
                videoEnabled && installationsEnabled && file1ApkrExists && file3ApkExists && file4ApkExists -> 1
                videoEnabled && !installationsEnabled && file1ApkrExists && file3ApkExists && file4ApkExists -> 2
                !videoEnabled && !installationsEnabled && file1ApkExists && file3ApkrExists && file4ApkrExists -> 3
                else -> -1
            }

            return DeviceStatus(
                videoEnabled = videoEnabled,
                installationsEnabled = installationsEnabled,
                googleServicesEnabled = googleServicesEnabled,
                isRooted = isRooted,
                currentPreset = currentPreset
            )
        }
    }

    /**
     * Commits state changes.
     * @param selectedPreset The preset to apply (1, 2, or 3)
     * @param enableGoogleServices If Google services should be enabled
     * @return Pair of (SuccessStatus, OptionalErrorMessage)
     */
    fun saveAndApplyChanges(
        context: Context,
        selectedPreset: Int,
        enableGoogleServices: Boolean
    ): Pair<Boolean, String?> {
        val isSim = isSimulationMode(context)
        Log.d(TAG, "Saving system configuration: Preset=$selectedPreset, GoogleServices=$enableGoogleServices, Simulator=$isSim")

        if (isSim) {
            // Apply in-memory simulation variables
            val prefs = getPrefs(context)
            val edit = prefs.edit()
            
            when (selectedPreset) {
                1 -> {
                    edit.putString(KEY_SIM_FILE_1, "apkr")
                    edit.putString(KEY_SIM_FILE_2, "apk")
                    edit.putString(KEY_SIM_FILE_3, "apk")
                    edit.putString(KEY_SIM_FILE_4, "apk")
                    edit.putString(KEY_SIM_FILE_6, "original")
                }
                2 -> {
                    edit.putString(KEY_SIM_FILE_1, "apkr")
                    edit.putString(KEY_SIM_FILE_2, "apk")
                    edit.putString(KEY_SIM_FILE_3, "apk")
                    edit.putString(KEY_SIM_FILE_4, "apk")
                    edit.putString(KEY_SIM_FILE_6, "edited")
                }
                3 -> {
                    edit.putString(KEY_SIM_FILE_1, "apk")
                    edit.putString(KEY_SIM_FILE_2, "apkr")
                    edit.putString(KEY_SIM_FILE_3, "apkr")
                    edit.putString(KEY_SIM_FILE_4, "apkr")
                    edit.putString(KEY_SIM_FILE_6, "edited")
                }
            }

            if (enableGoogleServices) {
                edit.putString(KEY_SIM_FILE_5, "apk")
            } else {
                edit.putString(KEY_SIM_FILE_5, "apkr")
            }
            
            edit.apply()
            return Pair(true, null)
        } else {
            // REAL Device Root modification script
            try {
                val commands = mutableListOf<String>()
                
                // Mount system read-write
                commands.add("mount -o remount,rw /system 2>/dev/null || mount -o remount,rw / 2>/dev/null")
                
                // Define actual operations for renaming/moving files based on option rules
                when (selectedPreset) {
                    1 -> {
                        // app5 (1) -> apkr
                        commands.add("if [ -f $PATH_FILE_1_APK ]; then mv $PATH_FILE_1_APK $PATH_FILE_1_APKR; fi")
                        // app4 (2) -> apk
                        commands.add("if [ -f $PATH_FILE_2_APKR ]; then mv $PATH_FILE_2_APKR $PATH_FILE_2_APK; fi")
                        // app2 (3) -> apk
                        commands.add("if [ -f $PATH_FILE_3_APKR ]; then mv $PATH_FILE_3_APKR $PATH_FILE_3_APK; fi")
                        // FilesGoogle (4) -> apk
                        commands.add("if [ -f $PATH_FILE_4_APKR ]; then mv $PATH_FILE_4_APKR $PATH_FILE_4_APK; fi")
                        // Copy Original Installer (7) to PackageInstaller (6) and create flag
                        commands.add("if [ -f $PATH_FILE_7 ]; then cp $PATH_FILE_7 $PATH_FILE_6; chmod 644 $PATH_FILE_6; touch $PATH_INSTALLER_FLAG; chmod 644 $PATH_INSTALLER_FLAG; fi")
                    }
                    2 -> {
                        // app5 (1) -> apkr
                        commands.add("if [ -f $PATH_FILE_1_APK ]; then mv $PATH_FILE_1_APK $PATH_FILE_1_APKR; fi")
                        // app4 (2) -> apk
                        commands.add("if [ -f $PATH_FILE_2_APKR ]; then mv $PATH_FILE_2_APKR $PATH_FILE_2_APK; fi")
                        // app2 (3) -> apk
                        commands.add("if [ -f $PATH_FILE_3_APKR ]; then mv $PATH_FILE_3_APKR $PATH_FILE_3_APK; fi")
                        // FilesGoogle (4) -> apk
                        commands.add("if [ -f $PATH_FILE_4_APKR ]; then mv $PATH_FILE_4_APKR $PATH_FILE_4_APK; fi")
                        // Copy Edited Installer (8) to PackageInstaller (6) and remove flag
                        commands.add("if [ -f $PATH_FILE_8 ]; then cp $PATH_FILE_8 $PATH_FILE_6; chmod 644 $PATH_FILE_6; rm -f $PATH_INSTALLER_FLAG; fi")
                    }
                    3 -> {
                        // app5 (1) -> apk
                        commands.add("if [ -f $PATH_FILE_1_APKR ]; then mv $PATH_FILE_1_APKR $PATH_FILE_1_APK; fi")
                        // app4 (2) -> apkr
                        commands.add("if [ -f $PATH_FILE_2_APK ]; then mv $PATH_FILE_2_APK $PATH_FILE_2_APKR; fi")
                        // app2 (3) -> apkr
                        commands.add("if [ -f $PATH_FILE_3_APK ]; then mv $PATH_FILE_3_APK $PATH_FILE_3_APKR; fi")
                        // FilesGoogle (4) -> apkr
                        commands.add("if [ -f $PATH_FILE_4_APK ]; then mv $PATH_FILE_4_APK $PATH_FILE_4_APKR; fi")
                        // Copy Edited Installer (8) to PackageInstaller (6) and remove flag
                        commands.add("if [ -f $PATH_FILE_8 ]; then cp $PATH_FILE_8 $PATH_FILE_6; chmod 644 $PATH_FILE_6; rm -f $PATH_INSTALLER_FLAG; fi")
                    }
                }

                // GmsCore (5) -> apk or apkr
                if (enableGoogleServices) {
                    commands.add("if [ -f $PATH_FILE_5_APKR ]; then mv $PATH_FILE_5_APKR $PATH_FILE_5_APK; fi")
                } else {
                    commands.add("if [ -f $PATH_FILE_5_APK ]; then mv $PATH_FILE_5_APK $PATH_FILE_5_APKR; fi")
                }

                // Sync and mount read-only
                commands.add("sync")
                commands.add("mount -o remount,ro /system 2>/dev/null || mount -o remount,ro / 2>/dev/null")

                // Execute root process shell
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                for (command in commands) {
                    os.writeBytes(command + "\n")
                }
                os.writeBytes("exit\n")
                os.flush()
                os.close()

                val exitValue = process.waitFor()
                if (exitValue == 0) {
                    // Update cache state
                    getPrefs(context).edit().apply {
                        putString(KEY_SIM_FILE_6, if (selectedPreset == 1) "original" else "edited")
                        apply()
                    }
                    return Pair(true, null)
                } else {
                    return Pair(false, "Root shell returned failure code: $exitValue. Please make sure the app was granted root permissions in Magisk/KernelSU.")
                }
            } catch (e: Exception) {
                return Pair(false, "Exception raised while running root script: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Reboots the Android device.
     */
    fun rebootDevice(context: Context): Boolean {
        val isSim = isSimulationMode(context)
        Log.w(TAG, "Rebooting device. Simulator=$isSim")
        
        if (isSim) {
            // Simulated reboot
            return true
        } else {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("reboot\n")
                os.flush()
                os.close()
                process.waitFor()
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to run reboot shell: ${e.localizedMessage}")
                return false
            }
        }
    }
}
