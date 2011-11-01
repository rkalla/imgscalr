package org.imgscalr.util;

import org.imgscalr.Scalr;

public class LogUtil {
	/**
	 * Used to write out a useful and well-formatted log message by any piece of
	 * code inside of the imgscalr library.
	 * <p/>
	 * If a message cannot be logged (logging is disabled) then this method
	 * returns immediately.
	 * <p/>
	 * <strong>NOTE</strong>: Because Java will auto-box primitive arguments
	 * into Objects when building out the <code>params</code> array, care should
	 * be taken not to call this method with primitive values unless
	 * {@link Scalr#DEBUG} is <code>true</code>; otherwise the VM will be
	 * spending time performing unnecessary auto-boxing calculations.
	 * 
	 * @param depth
	 *            The indentation level of the log message.
	 * @param src
	 *            The instance that is the source of the log message, this
	 *            allows more easily identifiable information to be written to
	 *            the log. If <code>null</code> then no identifying class
	 *            information is written in the log.
	 * @param message
	 *            The log message in <a href=
	 *            "http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html#syntax"
	 *            >format string syntax</a> that will be logged.
	 * @param params
	 *            The parameters that will be swapped into all the place holders
	 *            in the original messages before being logged.
	 * 
	 * @see Scalr#LOG_PREFIX
	 * @see Scalr#LOG_PREFIX_PROPERTY_NAME
	 */
	public static void log(int depth, Object src, String message,
			Object... params) {
		if (Scalr.DEBUG) {
			System.out.print(Scalr.LOG_PREFIX);

			if (src != null) {
				System.out.print(" ");
				System.out.print(src.getClass().getSimpleName());
				System.out.print(": ");
			}

			for (int i = 0; i < depth; i++)
				System.out.print("\t");

			System.out.printf(message, params);
			System.out.println();
		}
	}
}