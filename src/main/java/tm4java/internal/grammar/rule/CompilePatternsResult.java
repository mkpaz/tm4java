/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

/**
 * Represents the result of resolving and compiling rule patterns.
 *
 * <p>Aggregates the array of resolved rule identifiers alongside a status flag
 * indicating whether any referenced patterns could not be found during resolution.
 *
 * @param patterns           the array of rule IDs associated with the compiled patterns
 * @param hasMissingPatterns {@code true} if one or more referenced patterns failed to resolve; {@code false} otherwise
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L91">
 * vscode-textmate/src/rule.ts#L91</a>
 */
record CompilePatternsResult(RuleId[] patterns, boolean hasMissingPatterns) { }