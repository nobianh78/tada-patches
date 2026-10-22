import com.android.build.api.dsl.ApplicationExtension

dependencies {
    compileOnly(libs.annotation)
}

configure<ApplicationExtension> {
    lint { abortOnError = false }
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
}
