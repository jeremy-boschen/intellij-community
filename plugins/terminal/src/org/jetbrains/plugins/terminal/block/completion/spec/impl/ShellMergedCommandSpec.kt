// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.terminal.block.completion.spec.impl

import com.intellij.terminal.completion.spec.*
import com.intellij.util.containers.MultiMap
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecConflictStrategy
import org.jetbrains.plugins.terminal.block.completion.spec.ShellRuntimeDataGenerator
import javax.swing.Icon

internal class ShellMergedCommandSpec(
  val baseSpec: ShellCommandSpec?,
  val overridingSpecs: List<ShellCommandSpec>,
  val parentNames: List<String> = emptyList()
) : ShellCommandSpec {
  init {
    assert(overridingSpecs.isNotEmpty()) { "Overriding specs must not be empty. Command: ${parentNames + baseSpec?.name}" }
  }

  override val name: String
    get() = overridingSpecs.first().name

  override val displayName: String?
    get() = overridingSpecs.firstNotNullOfOrNull { it.displayName }

  override val description: String?
    get() = overridingSpecs.firstNotNullOfOrNull { it.description }

  override val insertValue: String?
    get() = overridingSpecs.firstNotNullOfOrNull { it.insertValue }

  override val priority: Int
    get() = overridingSpecs.maxOf { it.priority }

  override val icon: Icon?
    get() = overridingSpecs.firstNotNullOfOrNull { it.icon }

  override val requiresSubcommand: Boolean
    get() = overridingSpecs.any { it.requiresSubcommand }

  override val parserOptions: ShellCommandParserOptions
    get() = overridingSpecs.first().parserOptions

  private val parentNamesWithSelf: List<String> = parentNames + name

  override val subcommandsGenerator: ShellRuntimeDataGenerator<List<ShellCommandSpec>> = createSubcommandsGenerator()

  override val options: List<ShellOptionSpec> by lazy {
    val optionsMap = mutableMapOf<String, ShellOptionSpec>()
    for (option in baseSpec?.options ?: emptyList()) {
      optionsMap[option.name] = option
    }
    // Override the options, the last of the same name will be effective
    for (spec in overridingSpecs) {
      for (option in spec.options) {
        optionsMap[option.name] = option
      }
    }
    optionsMap.values.toList()
  }

  /**
   * Arguments are just taken from the first override spec.
   * They can't be merged because their number and order is important.
   * So, the overriding spec should define all existing arguments.
   */
  override val arguments: List<ShellArgumentSpec>
    get() = overridingSpecs.first().arguments

  private fun createSubcommandsGenerator(): ShellRuntimeDataGenerator<List<ShellCommandSpec>> {
    val firstGenerator = overridingSpecs.first().subcommandsGenerator
    // Use the same caching as in the first found generator
    return ShellRuntimeDataGenerator(
      debugName = "$parentNamesWithSelf merged subcommands",
      getCacheKey = { if (firstGenerator is ShellCacheableDataGenerator) firstGenerator.getCacheKey(it) else null }
    ) { context ->
      val specInfoMap = MultiMap<String, CommandSpecInfo>()

      val baseSubcommands = baseSpec?.subcommandsGenerator?.generate(context) ?: emptyList()
      specInfoMap.addSpecs(baseSubcommands, ShellCommandSpecConflictStrategy.DEFAULT)
      val overridingSubcommands = overridingSpecs.flatMap { spec -> spec.subcommandsGenerator.generate(context) }
      specInfoMap.addSpecs(overridingSubcommands, ShellCommandSpecConflictStrategy.OVERRIDE)

      // Create a merged command spec if some subcommand is present in both base and overridden specs.
      specInfoMap.entrySet().map { (_, specInfos) ->
        val base = specInfos.find { it.strategy == ShellCommandSpecConflictStrategy.DEFAULT }?.spec
        val overriding = specInfos.mapNotNull { if (it.strategy == ShellCommandSpecConflictStrategy.OVERRIDE) it.spec else null }
        if (overriding.isEmpty()) {
          base!! // if there are no overriding specs, base spec must be present
        }
        else ShellMergedCommandSpec(base, overriding, parentNamesWithSelf)
      }
    }
  }

  private fun MultiMap<String, CommandSpecInfo>.addSpecs(specs: List<ShellCommandSpec>, strategy: ShellCommandSpecConflictStrategy) {
    for (spec in specs) {
      putValue(spec.name, CommandSpecInfo(spec, strategy))
    }
  }

  private data class CommandSpecInfo(val spec: ShellCommandSpec, val strategy: ShellCommandSpecConflictStrategy)

  override fun toString(): String {
    return "ShellMergedCommandSpec(parentNamesWithSelf=$parentNamesWithSelf, baseSpec=$baseSpec, overridingSpecs=$overridingSpecs)"
  }
}