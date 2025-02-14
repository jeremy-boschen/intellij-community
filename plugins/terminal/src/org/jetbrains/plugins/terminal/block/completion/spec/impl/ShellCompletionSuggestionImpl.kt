// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.terminal.block.completion.spec.impl

import com.intellij.terminal.completion.spec.ShellSuggestionType
import org.jetbrains.annotations.Nls
import java.util.function.Supplier
import javax.swing.Icon

internal class ShellCompletionSuggestionImpl(
  name: String,
  override val type: ShellSuggestionType,
  override val displayName: String?,
  descriptionSupplier: Supplier<@Nls String>?,
  override val insertValue: String?,
  override val priority: Int,
  override val icon: Icon?
) : ShellCompletionSuggestionBase(name, descriptionSupplier) {
  init {
    if (priority !in 0..100) {
      error("Priority must be between 0 and 100")
    }
  }

  override fun toString(): String {
    return "ShellCompletionSuggestionImpl(name=$name, type=$type, displayName=$displayName, insertValue=$insertValue, priority=$priority, description=$description)"
  }
}