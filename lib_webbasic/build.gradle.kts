plugins {
    id("kre-publish")
}

dependencies {
    api(com.clistery.gradle.AppDependencies.clistery.appbasic)
    api(libs.tencentX5)
    api("org.java-websocket:Java-WebSocket:1.5.3")
    
    implementation(libs.androidXCoreKtx)
    implementation(libs.androidXAppcompat)
    implementation(libs.material)
    implementation("androidx.webkit:webkit:1.4.0")
    
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test.ext:truth:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}