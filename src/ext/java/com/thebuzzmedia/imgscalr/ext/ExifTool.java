package com.thebuzzmedia.imgscalr.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/*
 * TODO: Is there a way to discover the PID of the ExifTool process this class
 * spawns so it can keep track of it in Close and issue a taskkill (win) or
 * kill -9 (linux/mac/unix) in an attempt to clean up stray processes if necessary?
 */

/*
 * TODO: Add a few static checkExifTool methods to this class that this class
 * can both use and external processes (e.g. imgscalr Rotation.AUTO) can use to
 * verify the installation of ExifTool before trying to use this library and 
 * having something explode.
 */

/**
 * Class used to provide a Java-like wrapper to Phil Harvey's excellent <a
 * href="http://www.sno.phy.queensu.ca/~phil/exiftool">ExifTool</a>.
 * <p/>
 * There are a number of other basic wrappers to ExifTool available online, but
 * most of them just abstract out the actual process execution logic, but do no
 * other work to make using ExifTool from within Java any easier or more
 * intuitive.
 * <p/>
 * This class is an attempt at making ExifTool integration into a Java app
 * seamless and feel just like using a Java library.
 * <h3>ExifTool -stay_open Support</h3>
 * ExifTool <a href=
 * "http://u88.n24.queensu.ca/exiftool/forum/index.php/topic,1402.msg12933.html#msg12933"
 * >8.36</a> added a new persistent-process feature that allows ExifTool to stay
 * running in a daemon mode and continue accepting commands via a file or stdin.
 * This new mode is controlled via the <code>-stay_open True/False</code>
 * command line argument.
 * <p/>
 * If a version of ExifTool earlier than 8.36 is used, the
 * <code>-stay_open</code> command line argument is ignored and the code path
 * inside this tool written to take advantage of it will hang the executing
 * thread waiting for a reply from an incompatible ExifTool process.
 * <p/>
 * To protect against this nasty side effect, this class, when configured to use
 * the <code>stayOpen</code> mode, will make an attempt at verifying the version
 * of ExifTool <em>before</em> the first call to the external tool. Once the
 * version has been verified, the check will not be performed again for the life
 * of the instance. If the version is confirmed to be lower than the required
 * ExifTool version to support that feature, an exception is thrown to notify
 * the caller.
 * <h3>Performance</h3>
 * Extra care is taken to ensure minimal object creation or unnecessary CPU
 * overhead while communicating with the external process.
 * <p/>
 * {@link Pattern}s used to split the responses from the process are explicitly
 * compiled and reused, string concatenation is minimized, Tag name lookup is
 * done via a <code>static final</code> {@link Map} shared by all instances and
 * so on.
 * <p/>
 * Additionally, extra care is taken to utilize the most optimal code paths when
 * initiating and using the external process, for example, the
 * {@link ProcessBuilder#command(List)} method is used to avoid the copying of
 * array elements when {@link ProcessBuilder#command(String...)} is used and
 * avoiding the (hidden) use of {@link StringTokenizer} when
 * {@link Runtime#exec(String)} is called.
 * <p/>
 * All of this effort was done to ensure that imgscalr and its supporting
 * classes continue to provide best-of-breed performance and memory utilization
 * in long running/high performance environments (e.g. web applications).
 * <h3>Why ExifTool?</h3>
 * <a href="http://www.sno.phy.queensu.ca/~phil/exiftool">ExifTool</a> is
 * written in Perl and requires an external process call from Java to make use
 * of.
 * <p/>
 * While this would normally preclude a piece of software from inclusion into
 * the imgscalr library (more complex integration), there is no other image
 * metadata piece of software available as robust, complete and well-tested as
 * ExifTool. In addition, ExifTool already runs on all major platforms
 * (including Windows), so there was not a lack of portability introduced by
 * providing an integration for it.
 * <p/>
 * Allowing it to be used from Java is a boon to any Java project that needs the
 * ability to read/write image-metadata from almost <a
 * href="http://www.sno.phy.queensu.ca/~phil/exiftool/#supported">any image or
 * video file</a> format.
 * <h3>Alternatives</h3>
 * If integration with an external Perl process is something your app cannot do
 * and you still need image metadata-extraction capability, Drew Noakes has
 * written the 2nd most robust image metadata library I have come across: <a
 * href="http://drewnoakes.com/drewnoakes.com/code/exif/">Metadata Extractor</a>
 * that you might want to look at.
 * 
 * @author Riyad Kalla (software@thebuzzmedia.com)
 * @since 4.0
 */
public class ExifTool {
	/**
	 * Flag used to indicate if debugging output has been enabled by setting the
	 * "<code>imgscalr.ext.exiftool.debug</code>" system property to
	 * <code>true</code>. This value will be <code>false</code> if the "
	 * <code>imgscalr.ext.exiftool.debug</code>" system property is undefined or
	 * set to <code>false</code>.
	 * <p/>
	 * This system property can be set on startup with:<br/>
	 * <code>
	 * -Dimgscalr.ext.exiftool.debug=true
	 * </code> or by calling {@link System#setProperty(String, String)} before
	 * this class is loaded.
	 * <p/>
	 * Default value is <code>false</code>.
	 */
	public static final Boolean DEBUG = Boolean
			.getBoolean("imgscalr.ext.exiftool.debug");

	/**
	 * The absolute path to the ExifTool executable on the host system running
	 * this class as defined by the "<code>imgscalr.ext.exiftool.path</code>"
	 * system property.
	 * <p/>
	 * If ExifTool is on your system path and named "exiftool", leaving this
	 * value unchanged will work fine. If ExifTool is named something else or
	 * not in the system path, then this property will need to be set before
	 * using this class.
	 * <p/>
	 * This system property can be set on startup with:<br/>
	 * <code>
	 * -Dimgscalr.ext.exiftool.path=/path/to/exiftool
	 * </code> or by calling {@link System#setProperty(String, String)} before
	 * this class is loaded.
	 * <p/>
	 * On Windows be sure to double-escape the path to the tool, for example:
	 * <code>
	 * -Dimgscalr.ext.exiftool.path=C:\\Tools\\exiftool.exe
	 * </code>
	 * <p/>
	 * Default value is <code>exiftool</code>.
	 */
	public static final String EXIF_TOOL_PATH = System.getProperty(
			"imgscalr.ext.exiftool.path", "exiftool");

	/**
	 * Prefix to every log message this library logs. Using a well-defined
	 * prefix helps make it easier both visually and programmatically to scan
	 * log files for messages produced by this library.
	 * <p/>
	 * The value is "<code>[imgscalr.ext.exiftool] </code>" (including the
	 * space).
	 */
	public static final String LOG_PREFIX = "[imgscalr.ext.exiftool] ";

	private static final Pattern TAG_VALUE_PATTERN = Pattern.compile(": ");

	/**
	 * Enum used to specify the different supported tags that can be extracted
	 * from an image using ExifTool.
	 * <p/>
	 * ExifTool is capable of extracting almost every image metadata tag known
	 * to man, but this class makes an attempt at simplifying the most common
	 * cases (the 80% rule) and defining them as simple Enums that can be used
	 * easily and intuitively.
	 * 
	 * @see <a
	 *      href="http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/index.html">Full
	 *      Tag List</a>
	 * 
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 3.2
	 */
	public enum Tag {
		ISO("ISO"), APERTURE("ApertureValue"), SHUTTER_SPEED(
				"ShutterSpeedValue"), FOCAL_LENGTH("FocalLength"), EXPOSURE_COMPENSATION(
				"ExposureCompensation"), EXPOSURE_PROGRAM("ExposureProgram"), ORIENTATION(
				"Orientation"), MAKE("Make"), MODEL("Model"), TITLE("XPTitle"), AUTHOR(
				"XPAuthor"), SUBJECT("XPSubject"), KEYWORDS("XPKeywords"), COMMENT(
				"XPComment"), RATING("Rating"), RATING_PERCENT("RatingPercent"), DATE_TIME_ORIGINAL(
				"DateTimeOriginal"), GPS_LATITUDE("GPSLatitude"), GPS_LATITUDE_REF(
				"GPSLatitudeRef"), GPS_LONGITUDE("GPSLongitude"), GPS_LONGITUDE_REF(
				"GPSLongitudeRef"), GPS_ALTITUDE("GPSAltitude"), GPS_ALTITUDE_REF(
				"GPSAltitudeRef"), GPS_SPEED("GPSSpeed"), GPS_SPEED_REF(
				"GPSSpeedRef"), GPS_PROCESS_METHOD("GPSProcessingMethod"), GPS_BEARING(
				"GPSDestBearing"), GPS_BEARING_REF("GPSDestBearingRef");

		private static final Map<String, Tag> LOOKUP_MAP;

		static {
			Tag[] values = Tag.values();
			LOOKUP_MAP = new HashMap<String, ExifTool.Tag>(values.length * 3);

			for (int i = 0; i < values.length; i++) {
				Tag tag = values[i];
				LOOKUP_MAP.put(tag.name, tag);
			}
		}

		public static Tag forName(String name) {
			return LOOKUP_MAP.get(name);
		}

		private String name;

		private Tag(String name) {
			this.name = name;
		}
	}

	private boolean stayOpen;
	private boolean stayOpenSupportVerified;

	private List<String> args;

	private BufferedReader reader;
	private OutputStreamWriter writer;

	public ExifTool() {
		this(false);
	}

	public ExifTool(boolean stayOpen) {
		this.stayOpen = stayOpen;
		this.stayOpenSupportVerified = false;

		args = new ArrayList<String>(64);
	}

	public boolean isOpen() {
		return stayOpen;
	}

	public void close() {
		// No-op if ExifTool never used -stay_open True in the first place.
		if (!stayOpen)
			return;

		/*
		 * If ExifTool was used in stayOpen mode but getImageMeta was never
		 * called then the streams were never initialized and there is nothing
		 * to shut down or destroy, otherwise we need to close down all the
		 * resources in use.
		 */
		if (reader == null && writer == null) {
			log("\tThis ExifTool instance was never used so no external process or streams were ever created (nothing to clean up, we can just exit).");
		} else {
			try {
				log("\tAttempting to close ExifTool daemon process, issuing '-stay_open\\nFalse\\n' command...");

				// Tell the ExifTool process to exit.
				writer.write("-stay_open\nFalse\n");
				writer.flush();

				log("\t\tSuccessful");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				closeStreams();
			}
		}

		// Update the open state.
		stayOpen = false;
		log("\tExifTool daemon process successfully terminated.");
	}

	public Map<Tag, String> getImageMeta(File image, Tag... tags)
			throws IllegalArgumentException, SecurityException, IOException {
		if (image == null)
			throw new IllegalArgumentException(
					"image cannot be null and must be a valid stream of image data.");
		if (!image.canRead())
			throw new SecurityException(
					"Unable to read the given image ["
							+ image.getAbsolutePath()
							+ "], ensure that the image exists at the given path and that the executing Java process has permissions to read it.");

		long startTime = System.currentTimeMillis();
		Map<Tag, String> resultMap = new HashMap<ExifTool.Tag, String>(
				tags.length * 3);

		if (DEBUG)
			log("Querying %d tags from image: %s", tags.length,
					image.getAbsolutePath());

		// Ensure there is work to do first.
		if (tags.length > 0) {
			// Clear process args
			args.clear();

			if (stayOpen) {
				log("\tUsing ExifTool in daemon mode (-stay_open True)...");

				/*
				 * If this is our first time calling getImageMeta with a
				 * stayOpen connection, set up the persistent process and run it
				 * so it is ready to receive commands from us.
				 */
				if (reader == null && writer == null) {
					log("\tStarting daemon ExifTool process and creating read/write streams (this only happens once)...");

					args.add(EXIF_TOOL_PATH);
					args.add("-stay_open");
					args.add("True");
					args.add("-@");
					args.add("-");

					// Begin the persistent ExifTool process.
					runProcess(args);
				}

				log("\tStreaming arguments to ExifTool process...");

				writer.write("-n\n"); // numeric output
				writer.write("-S\n"); // compact output
				writer.write("-fast\n"); // do not search to EOF

				for (int i = 0; i < tags.length; i++) {
					writer.write('-');
					writer.write(tags[i].name);
					writer.write("\n");
				}

				writer.write(image.getAbsolutePath());
				writer.write("\n");

				log("\tExecuting ExifTool...");

				// Run ExifTool on our file with all the given arguments.
				writer.write("-execute\n");
				writer.flush();
			} else {
				log("\tUsing ExifTool in non-daemon mode (-stay_open False)...");

				/*
				 * Since we are not using a stayOpen process, we need to setup
				 * the execution arguments completely each time.
				 */
				args.add(EXIF_TOOL_PATH);

				args.add("-n"); // numeric output
				args.add("-S"); // compact output
				args.add("-fast"); // do not search to EOF

				for (int i = 0; i < tags.length; i++)
					args.add("-" + tags[i].name);

				args.add(image.getAbsolutePath());

				// Run the ExifTool with our args.
				runProcess(args);
			}

			log("\tReading response back from ExifTool...");

			String line = null;

			while ((line = reader.readLine()) != null) {
				String[] pair = TAG_VALUE_PATTERN.split(line);

				if (pair != null && pair.length == 2) {
					// Determine the tag represented by this value.
					Tag tag = Tag.forName(pair[0]);

					// Store the tag and its associated value in the result map.
					resultMap.put(tag, pair[1]);
					log("\t\tRead Tag [name=%s, value=%s]", tag.name, pair[1]);
				}

				/*
				 * When using a persistent ExifTool process, it terminates its
				 * output to us with a {ready} clause, we need to look for it
				 * and break from this loop when we see it otherwise this
				 * process will hang.
				 */
				if (stayOpen && line.equals("{ready}"))
					break;
			}

			log("\tDone reading ExifTool response.");

			/*
			 * If we are not using a persistent ExifTool process, then after
			 * running the command above, the process exited in which case we
			 * need to clean our streams up since it no longer exists. If we
			 * were using a persistent ExifTool process, leave the streams open
			 * for future calls.
			 */
			if (!stayOpen)
				closeStreams();
		}

		if (DEBUG)
			log("\tImage Meta Processed in %d ms [queried %d tags and found %d values]",
					(System.currentTimeMillis() - startTime), tags.length,
					resultMap.size());

		return resultMap;
	}

	/**
	 * Helper method used to ensure a message is loggable before it is logged
	 * and then pre-pend a universal prefix to all log messages generated by
	 * this library to make the log entries easy to parse visually or
	 * programmatically.
	 * <p/>
	 * If a message cannot be logged (logging is disabled) then this method
	 * returns immediately.
	 * <p/>
	 * <strong>NOTE</strong>: Because Java will auto-box primitive arguments
	 * into Objects when building out the <code>params</code> array, care should
	 * be taken not to call this method with primitive values unless
	 * {@link #DEBUG} is <code>true</code>; otherwise the VM will be spending
	 * time performing unnecessary auto-boxing calculations.
	 * 
	 * @param message
	 *            The log message in <a href=
	 *            "http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html#syntax"
	 *            >format string syntax</a> that will be logged.
	 * @param params
	 *            The parameters that will be swapped into all the place holders
	 *            in the original messages before being logged.
	 * 
	 * @see #LOG_PREFIX
	 */
	protected static void log(String message, Object... params) {
		if (DEBUG)
			System.out.printf(LOG_PREFIX + message + '\n', params);
	}

	protected void runProcess(List<String> args) throws RuntimeException {
		/*
		 * Ensure we have verified stayOpen support with the installed ExifTool
		 * version, otherwise our code path for that mode will hang.
		 */
		if (stayOpen && !stayOpenSupportVerified)
			checkExifToolVersion();

		Process proc = null;

		try {
			log("\tAttempting to start process: %s", args);

			proc = new ProcessBuilder(args).start();

			log("\t\tSuccessful");
		} catch (Exception e) {
			throw new RuntimeException(
					"Unable to start the ExifTool process using the following execution arguments "
							+ args
							+ ". Ensure ExifTool is installed and executable using the command '"
							+ EXIF_TOOL_PATH
							+ "'. The path used to executed ExifTool can be adjusted using the 'imgscalr.ext.exiftool.path' system property.",
					e);
		}

		log("\tSetting up Read/Write streams to process...");

		// Setup read/write streams to the new process.
		reader = new BufferedReader(
				new InputStreamReader(proc.getInputStream()));
		writer = new OutputStreamWriter(proc.getOutputStream());

		log("\t\tSuccessful");
	}

	protected void checkExifToolVersion() throws RuntimeException {
		try {
			log("\tVerifiying installed ExifTool supports daemon mode (this check is only done once)...");

			Process proc = new ProcessBuilder(EXIF_TOOL_PATH, "-ver").start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			String ver = reader.readLine();
			reader.close();

			if (ver.length() >= 3 && ver.compareTo("8.36") >= 0) {
				stayOpenSupportVerified = true;

				log("\t\tRequired ExifTool version CONFIRMED, needed version 8.36 or higher and found version %s.",
						ver);
			} else {
				String message = "Required ExifTool version NOT FOUND. Needed version 8.36 or higher but found version "
						+ ver + " instead.";
				log("\t\t%s", message);
				throw new RuntimeException(message);
			}
		} catch (IOException e) {
			throw new RuntimeException(
					"Unable to start the ExifTool process in order to verify the version '"
							+ EXIF_TOOL_PATH
							+ " -ver'. Ensure ExifTool is installed and executable using the command path defined by the 'imgscalr.ext.exiftool.path' system property.",
					e);
		}
	}

	protected void closeStreams() {
		try {
			log("\tClosing Read stream...");
			reader.close();
			log("\t\tSuccessful");
		} catch (Exception e) {
			// no-op, just try to close it.
		}

		try {
			log("\tClosing Write stream...");
			writer.close();
			log("\t\tSuccessful");
		} catch (Exception e) {
			// no-op, just try to close it.
		}

		// Null the stream references.
		reader = null;
		writer = null;

		log("\tRead/Write streams successfully closed.");
	}
}