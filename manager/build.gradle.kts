// [FIX] Menggunakan blok buildscript agar plugin StringFog punya akses ke library XOR
// [CRITICAL] Blok buildscript harus jadi blok pertama
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {

    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
