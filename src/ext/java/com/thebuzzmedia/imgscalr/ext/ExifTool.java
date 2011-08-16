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
 * TODO: Add a few static checkExifTool methods to this class that this class
 * can both use and external processes (e.g. imgscalr Rotation.AUTO) can use to
 * verify the installation of ExifTool before trying to use this library and 
 * having something explode.
 */

/**
 * Class used to provide a Java-like wrapper to Phil Harvey's excellent <a
 * href="http://www.sno.phy.queensu.ca/~phil/exiftool">ExifTool</a>.
 * <p/>
 * Instances of this class are <strong>not</strong> Thread-safe. Both the
 * instance of this class and external ExifTool process maintain state specific
 * to the current operation. Use of instances of this class need to be
 * synchronized using an external mechanism or in a highly threaded environment
 * (e.g. web application), instances of this class can be used along with
 * {@link ThreadLocal}s to ensure Thread-safe, highly parallel use.
 * <p/>
 * There are a number of other basic wrappers to ExifTool available online, but
 * most of them only abstract out the actual Java-external-process execution
 * logic and do no additional work to make integration with the external
 * ExifTool any easier or intuitive.
 * <p/>
 * This class was written in order to make integration with ExifTool inside of a
 * Java application seamless, performant and easy whether you use the imgscalr
 * library or not.
 * <h3>Usage</h3>
 * Assuming ExifTool is installed on the host system correctly and either in the
 * system path or pointed at correctly by {@link #EXIF_TOOL_PATH}, using this
 * class to communicate with ExifTool is as simple as creating an instance (
 * <code>ExifTool tool = new ExifTool()</code>) and then making calls to
 * {@link #getImageMeta(File, Tag...)} with the list of {@link Tag} values you
 * wish to pull from the source image.
 * <p/>
 * In this default mode, calls to {@link #getImageMeta(File, Tag...)} will
 * automatically start an external ExifTool process to handle the request. After
 * ExifTool has parsed the tag values from the file, the external process exits
 * and this class parses the result.
 * <p/>
 * Results from calls to {@link #getImageMeta(File, Tag...)} are returned in a
 * {@link Map} with the {@link Tag} values as the keys and {@link String} values
 * for every tag that had a value in the image file. {@link Tag}s with no value
 * found in the image are omitted from the result map.
 * <p/>
 * It is up to the caller to decide how best to parse and convert the resulting
 * {@link String} values (e.g. convert to numbers, enums, etc.).
 * <p/>
 * In order to make use of the new <code>-stay_open</code> daemon mode supported
 * by ExifTool 8.36 and later, simply pass <code>true</code> to the constructor
 * of this class when creating an instance:
 * <code>ExifTool tool = new ExifTool(true)</code> - this will cause a daemon
 * version of ExifTool to be spun up and reused for all subsequent calls to
 * {@link #getImageMeta(File, Tag...)}.
 * <p/>
 * <strong>REMINDER</strong>: When you use this mode, you must be sure to
 * explicitly call {@link #close()} when you are done using ExifTool so this
 * class has a chance to stop the external ExifTool process and cleanup the
 * read/write streams used to communicate with it.
 * <p/>
 * Forgetting to do this will not only leak resources inside the VM, but leak
 * <code>exiftool</code> processes in the host OS.
 * <h3>ExifTool -stay_open Support</h3>
 * ExifTool <a href=
 * "http://u88.n24.queensu.ca/exiftool/forum/index.php/topic,1402.msg12933.html#msg12933"
 * >8.36</a> added a new persistent-process feature that allows ExifTool to stay
 * running in a daemon mode and continue accepting commands via a file or stdin.
 * <p/>
 * This new mode is controlled via the <code>-stay_open True/False</code>
 * command line argument and in a busy system can offer speed improvements of up
 * to <strong>40-60x</strong> (yes, really that much).
 * <p/>
 * This feature was added to ExifTool shortly after user <a
 * href="http://www.christian-etter.de/?p=458">Christian Etter discovered</a>
 * the overhead for starting up a new Perl interpreter each time ExifTool is
 * loaded accounts for roughly <a href=
 * "http://u88.n24.queensu.ca/exiftool/forum/index.php/topic,1402.msg6121.html#msg6121"
 * >98.4% of the total runtime</a>.
 * <p/>
 * If <code>-stay_open</code> support is enabled in this class by passing
 * <code>true</code> to the constructor, but the installed copy of ExifTool is
 * older than 8.36 on the host system, in order to avoid a potential lockup,
 * this class will actually make an attempt at verifying the version of ExifTool
 * used before trying to run in daemon mode. Once the version of the installed
 * ExifTool has been verified, the check will not be performed again for the
 * life of the instance of this class.
 * <p/>
 * If the version is confirmed to be lower than the required ExifTool version to
 * support that feature, a {@link RuntimeException} is thrown to notify the
 * caller of the situation.
 * <p/>
 * At that point the developer can either upgrade ExifTool or not use it in
 * daemon mode to work around the exception.
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
	 * If ExifTool is on your system path and running the command "exiftool"
	 * successfully executes it, leaving this value unchanged will work fine on
	 * any platform. If the ExifTool executable is named something else or not
	 * in the system path, then this property will need to be set to point at it
	 * before using this class.
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
	 * Default value is "<code>exiftool</code>".
	 * <h3>Relative Paths</h3>
	 * Relative path values (e.g. "bin/tools/exiftool") are executed with
	 * relation to the base directory the VM process was started in. Essentially
	 * the directory that <code>new File(".").getAbsolutePath()</code> points at
	 * during runtime.
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

	/**
	 * Compiled {@link Pattern} of ": " used to split compact output from
	 * ExifTool evenly into name/value pairs.
	 */
	protected static final Pattern TAG_VALUE_PATTERN = Pattern.compile(": ");

	/*
	 * TODO: Would be really nice to implement some flexible tag-building system
	 * that can be extended by anyone using the library that needs a tag value
	 * but there isn't an enum for it.
	 */

	/**
	 * Enum used to pre-define a convenience list of tags that can be easily
	 * extracted from images using this class with an external install of
	 * ExifTool.
	 * <p/>
	 * Each tag defined also includes a type hint for the parsed value
	 * associated with it. All replies from ExifTool are parsed as
	 * {@link String}s and using the type hint can easily be converted to the
	 * correct data format inside of Java by the caller using the provided
	 * {@link Tag#parseValue(Tag, String)} method. This class does not make an
	 * attempt at converting the value automatically in case there is a problem
	 * with a past or future version of ExifTool that the caller will need to
	 * manually handle.
	 * <p/>
	 * The types are merely a hint based on the <a
	 * href="http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/index.html"
	 * >ExifTool tag guide</a> by Phil Harvey, the caller is free to parse or
	 * process the returned {@link String} values any way they wish.
	 * <h3>Tag Support</h3>
	 * ExifTool is capable of parsing almost every tag known to man (1000+), but
	 * this class makes an attempt at pre-defining a convenient list of the most
	 * common tags for use. This list was determined by looking at the common
	 * metadata tag values written to images by popular mobile devices like the
	 * iPhone and Android phones as well as cameras like a Canon Point and Shoot
	 * as well as a DSLR.
	 * <p/>
	 * Please email or <a
	 * href="https://github.com/thebuzzmedia/imgscalr/issues">file an issue</a>
	 * if you think this list is missing a commonly used tag that should be
	 * added to it.
	 * 
	 * @author Riyad Kalla (software@thebuzzmedia.com)
	 * @since 4.0
	 */
	public enum Tag {
		IMAGE_WIDTH("ImageWidth", Integer.class), IMAGE_HEIGHT("ImageHeight",
				Integer.class), ISO("ISO", Integer.class), APERTURE(
				"ApertureValue", Double.class), WHITE_BALANCE("WhiteBalance",
				Integer.class), SHUTTER_SPEED("ShutterSpeedValue", Double.class), FLASH(
				"Flash", Integer.class), METERING_MODE("MeteringMode",
				Integer.class), FOCAL_LENGTH("FocalLength", Double.class), EXPOSURE_TIME(
				"ExposureTime", Double.class), EXPOSURE_COMPENSATION(
				"ExposureCompensation", Double.class), EXPOSURE_PROGRAM(
				"ExposureProgram", Integer.class), ORIENTATION("Orientation",
				Integer.class), COLOR_SPACE("ColorSpace", Integer.class), MAKE(
				"Make", String.class), MODEL("Model", String.class), LENS_MAKE(
				"LensMake", String.class), LENS_MODEL("LensModel", String.class), OWNER_NAME(
				"OwnerName", String.class), TITLE("XPTitle", String.class), AUTHOR(
				"XPAuthor", String.class), SUBJECT("XPSubject", String.class), KEYWORDS(
				"XPKeywords", String.class), COMMENT("XPComment", String.class), RATING(
				"Rating", Integer.class), RATING_PERCENT("RatingPercent",
				Integer.class), DATE_TIME_ORIGINAL("DateTimeOriginal",
				String.class), GPS_LATITUDE("GPSLatitude", Double.class), GPS_LATITUDE_REF(
				"GPSLatitudeRef", String.class), GPS_LONGITUDE("GPSLongitude",
				Double.class), GPS_LONGITUDE_REF("GPSLongitudeRef",
				String.class), GPS_ALTITUDE("GPSAltitude", Double.class), GPS_ALTITUDE_REF(
				"GPSAltitudeRef", Integer.class), GPS_SPEED("GPSSpeed",
				Double.class), GPS_SPEED_REF("GPSSpeedRef", String.class), GPS_PROCESS_METHOD(
				"GPSProcessingMethod", String.class), GPS_BEARING(
				"GPSDestBearing", Double.class), GPS_BEARING_REF(
				"GPSDestBearingRef", String.class), GPS_TIMESTAMP(
				"GPSTimeStamp", String.class), EXIF_VERSION("ExifVersion",
				String.class);

		private static final Map<String, Tag> TAG_LOOKUP_MAP;

		/**
		 * Initializer used to init the <code>static final</code> tag/name
		 * lookup map used by all instances of this class.
		 */
		static {
			Tag[] values = Tag.values();
			TAG_LOOKUP_MAP = new HashMap<String, ExifTool.Tag>(
					values.length * 3);

			for (int i = 0; i < values.length; i++) {
				Tag tag = values[i];
				TAG_LOOKUP_MAP.put(tag.name, tag);
			}
		}

		/**
		 * Used to get the {@link Tag} identified by the given, case-sensitive,
		 * tag name.
		 * 
		 * @param name
		 *            The case-sensitive name of the tag that will be searched
		 *            for.
		 * 
		 * @return the {@link Tag} identified by the given, case-sensitive, tag
		 *         name or <code>null</code> if one couldn't be found.
		 */
		public static Tag forName(String name) {
			return TAG_LOOKUP_MAP.get(name);
		}

		/**
		 * Convenience method used to convert the given string Tag value
		 * (returned from the external ExifTool process) into the type described
		 * by the associated {@link Tag}.
		 * 
		 * @param <T>
		 *            The type of the returned value.
		 * @param tag
		 *            The {@link Tag} whose value this is. The tag's type hint
		 *            will be queried to determine how to convert this string
		 *            value.
		 * @param value
		 *            The {@link String} representation of the tag's value as
		 *            parsed from the image.
		 * 
		 * @return the given string value converted to a native Java type (e.g.
		 *         Integer, Double, etc.).
		 * 
		 * @throws IllegalArgumentException
		 *             if <code>tag</code> is <code>null</code>.
		 */
		@SuppressWarnings("unchecked")
		public static <T> T parseValue(Tag tag, String value)
				throws IllegalArgumentException {
			if (tag == null)
				throw new IllegalArgumentException("tag cannot be null");

			T result = null;

			// Check that there is work to do first.
			if (value != null) {
				Class<?> type = tag.type;

				try {
					if (Boolean.class.isAssignableFrom(type))
						result = (T) Boolean.valueOf(value);
					else if (Byte.class.isAssignableFrom(type))
						result = (T) Byte.valueOf(Byte.parseByte(value));
					else if (Integer.class.isAssignableFrom(type))
						result = (T) Integer.valueOf(Integer.parseInt(value));
					else if (Short.class.isAssignableFrom(type))
						result = (T) Short.valueOf(Short.parseShort(value));
					else if (Long.class.isAssignableFrom(type))
						result = (T) Long.valueOf(Long.parseLong(value));
					else if (Float.class.isAssignableFrom(type))
						result = (T) Float.valueOf(Float.parseFloat(value));
					else if (Double.class.isAssignableFrom(type))
						result = (T) Double.valueOf(Double.parseDouble(value));
					else if (Character.class.isAssignableFrom(type))
						result = (T) Character.valueOf(value.charAt(0));
					else if (String.class.isAssignableFrom(type))
						result = (T) value;
				} catch (Exception e) {
					// no-op, we just return null if it cannot be parsed.
				}
			}

			return result;
		}

		/**
		 * Used to get the name of the tag (e.g. "Orientation", "ISO", etc.).
		 * 
		 * @return the name of the tag (e.g. "Orientation", "ISO", etc.).
		 */
		public String getName() {
			return name;
		}

		/**
		 * Used to get a hint for the native type of this tag's value as
		 * specified by Phil Harvey's <a href=
		 * "http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/index.html"
		 * >ExifTool Tag Guide</a>.
		 * 
		 * @return a hint for the native type of this tag's value.
		 */
		public Class<?> getType() {
			return type;
		}

		private String name;
		private Class<?> type;

		private Tag(String name, Class<?> type) {
			this.name = name;
			this.type = type;
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

	/*
	 * TODO: Change stayOpen to a Feature enum that can be passed in.
	 */
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

	/**
	 * TODO: This should be static and run ONCE for every instance of this class type
	 * because the EXIF_TOOL_PATH used to execute points at the same version of the executable every
	 * time, no need to keep testing the same executable.
	 * 
	 * @throws RuntimeException
	 */
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