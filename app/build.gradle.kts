plugins {
    id("app")
    id("org.jetbrains.kotlin.android")
}

//android {
//    defaultConfig {
//        val groupName: String by project
//        val artifactName: String by project
//        val targetSdkName: String by project
//        val versionName: String by project
//        val versionCode: String by project
//
//        this.applicationId = "${groupName}.${artifactName}.demo1"
//        this.targetSdk = targetSdkName.toInt()
//        this.versionName = versionName
//        this.versionCode = versionCode.toInt()
//
//        multiDexEnabled = true
//
//        vectorDrawables.useSupportLibrary = true
//    }
//}

dependencies {
    implementation(libs.androidXCoreKtx)
    implementation(libs.androidXAppcompat)
    implementation(libs.material)
    
    testImplementation("junit:junit:4.13.2")
    
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test.ext:truth:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    
//    implementation(project(":lib_webbasic"))
    implementation("io.github.clistery:webbasic:1.1.0")
    
    implementation("com.google.zxing:core:3.3.3")
    implementation("com.journeyapps:zxing-android-embedded:3.6.0") {
        isTransitive = false
    }
}
