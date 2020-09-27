package com.github.asarco.huejetbrains.services

import com.intellij.openapi.project.Project
import com.github.asarco.huejetbrains.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
