import java.security.MessageDigest
tasks.register("delcomChecking") {
    dependsOn("lintReportDebug")
    doLast {
        val f = File(layout.projectDirectory.toString(), "build/reports/lint-results-debug.html")
        if (f.exists()) {
            val c = f.readText()
            val r = """<span class="mdl-layout-title">([^<]+)</span>""".toRegex()
            val m = r.find(c)
            val w = m?.groupValues?.get(1) ?: "Lint warnings not found"
            println(w)

            val md5 = f.inputStream().use { inputStream ->
                val md = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
                val hashBytes = md.digest()
                hashBytes.joinToString("") { "%02x".format(it) }
            }
            println("MD5 Hash: $md5")
        } else {
            println("Lint report file not found: ${f.absolutePath}")
        }
    }
}
tasks.named("lint") {
    finalizedBy("delcomChecking")
}



plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "del.ifs22017.delcomapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "del.ifs22017.delcomapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{LICENSE.md,LICENSE.txt,NOTICE.md,NOTICE.txt}"
        }
    }




}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.databinding.compiler.common)
    implementation(libs.volley)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.lifecycle.runtime.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    implementation(libs.androidx.compose.material)

}