/**
 * The Ted chatbot: a command-line task list that remembers what it is told.
 * <p>
 * The classes here are the moving parts of one conversation, each with a
 * single job:
 * <ul>
 *   <li>{@link ted.Ted} wires the others together and runs the command loop</li>
 *   <li>{@link ted.Ui} does all the talking and listening</li>
 *   <li>{@link ted.Parser} reads a typed line as a command</li>
 *   <li>{@link ted.Storage} loads and saves the task list</li>
 *   <li>{@link ted.TedException} carries a problem the user can recover from</li>
 * </ul>
 * The tasks themselves live in {@link ted.task}, and the commands that act on
 * them in {@link ted.command}.
 */
package ted;
