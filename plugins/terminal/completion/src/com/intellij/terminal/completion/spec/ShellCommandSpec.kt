package com.intellij.terminal.completion.spec

import org.jetbrains.annotations.ApiStatus

/**
 * Represents the specification of the Shell command with [names]: its subcommands, options, arguments, and some settings.
 * These command specifications are used for providing command completion in the New Terminal.
 *
 * **Please do not override this interface**, use [helper function][org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpec]
 * to create the instance of it.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
interface ShellCommandSpec : ShellCompletionSuggestion {
  override val type: ShellSuggestionType
    get() = ShellSuggestionType.COMMAND

  /**
   * Whether this command can't be executed without mentioning the subcommand.
   *
   * False by default.
   */
  val requiresSubcommand: Boolean

  /**
   * Parser options that define the rules of generating suggestions when using this command spec.
   * @see [ShellCommandParserOptions]
   */
  val parserOptions: ShellCommandParserOptions

  /**
   * Generator that provides the actual subcommands of the current command for the given [ShellRuntimeContext].
   */
  val subcommandsGenerator: ShellRuntimeDataGenerator<List<ShellCommandSpec>>

  /**
   * Available options of this shell command.
   */
  val options: List<ShellOptionSpec>

  /**
   * Available arguments of this shell command.
   * Note that the order of the arguments is the same as it is expected in the command line
   */
  val arguments: List<ShellArgumentSpec>
}