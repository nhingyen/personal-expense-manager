package com.ngyennhi.expensemanager.data

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class OfflineReleasePolicyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun installedManifestDisablesBackupAndRequestsNoInternet() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        val permissions = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
    }

    @Test fun packagedRulesExcludeEveryStorageDomainFromCloudAndDeviceTransfer() {
        val exclusions = mutableMapOf<String, MutableSet<String>>()
        var section: String? = null
        context.resources.getXml(R.xml.data_extraction_rules).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> {
                            section = parser.name
                            exclusions.getOrPut(parser.name) { mutableSetOf() }
                        }
                        "exclude" -> if (section != null && parser.getAttributeValue(null, "path") == ".") {
                            exclusions.getValue(section).add(parser.getAttributeValue(null, "domain"))
                        }
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == section) {
                    section = null
                }
                parser.next()
            }
        }
        val domains = setOf("root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref")
        assertEquals(mapOf("cloud-backup" to domains, "device-transfer" to domains), exclusions)
    }
}
