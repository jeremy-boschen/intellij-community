// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gradle.service.syncAction

import com.intellij.gradle.toolingExtension.modelAction.GradleModelFetchPhase
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

/**
 * The [GradleSyncContributor] is used for the IDE project configuration in the [com.intellij.platform.backend.workspace.WorkspaceModel].
 *
 * The [com.intellij.openapi.externalSystem.util.Order] annotation defines the execution order for these contributors.
 *
 * @see com.intellij.platform.backend.workspace.WorkspaceModel
 * @see com.intellij.openapi.externalSystem.util.Order
 */
@ApiStatus.Experimental
interface GradleSyncContributor {

  val name: String
    get() = javaClass.simpleName

  /**
   * Called when Gradle model building phase is completed.
   * Guaranteed that all phases will be handled for the successful execution in the strict order.
   *
   * @param context contain all information about the current state of the Gradle sync.
   * Use this context to access to the fetched Gradle models.
   * @param phase current phase of the model fetching action.
   *
   * @see GradleModelFetchPhase
   */
  suspend fun onModelFetchPhaseCompleted(
    context: ProjectResolverContext,
    storage: MutableEntityStorage,
    phase: GradleModelFetchPhase
  ) = Unit

  /**
   * Called once Gradle has finished executing everything, including any tasks that might need to be run.
   * The models are obtained separately and in some cases before this method is called.
   *
   * @param context contain all information about the current state of the Gradle sync.
   * Use this context to access to the fetched Gradle models.
   */
  suspend fun onModelFetchCompleted(
    context: ProjectResolverContext,
    storage: MutableEntityStorage
  ) = Unit

  /**
   * Called once Gradle has failed to execute everything.
   * The models are obtained separately and in some cases before this method is called.
   *
   * @param context contain all information about the current state of the Gradle sync.
   * Use this context to access to the fetched Gradle models.
   * @param exception the exception thrown by Gradle, if everything completes successfully, then this will be null.
   */
  suspend fun onModelFetchFailed(
    context: ProjectResolverContext,
    storage: MutableEntityStorage,
    exception: Throwable
  ) = Unit

  /**
   * Called once Gradle has loaded projects but before any task execution.
   * These models do not contain those models that are created when the build finished.
   *
   * @param context contain all information about the current state of the Gradle sync.
   * Use this context to access to the fetched Gradle models.
   *
   * @see org.gradle.tooling.BuildActionExecuter.Builder.projectsLoaded
   * @see org.gradle.tooling.IntermediateResultHandler
   */
  @ApiStatus.Internal
  suspend fun onProjectLoadedActionCompleted(
    context: ProjectResolverContext,
    storage: MutableEntityStorage
  ) = Unit

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<GradleSyncContributor> = create("org.jetbrains.plugins.gradle.syncContributor")
  }
}
