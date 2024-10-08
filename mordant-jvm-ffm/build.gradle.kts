plugins {
    id("mordant-kotlin-conventions")
    id("mordant-publishing-conventions")
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mordant"))
        }
        jvmMain.dependencies {
            compileOnly(libs.graalvm.svm)
        }
    }
}
