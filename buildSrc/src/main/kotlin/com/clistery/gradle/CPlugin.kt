package com.clistery.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate

internal val Project.isRoot get() = this == this.rootProject

class CPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        if(project.isRoot){
            project.tasks.register("clean", Delete::class.java) {
                delete(project.buildDir)
            }
        }
    }
}
