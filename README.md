# imgscalr - Java Image-Scaling Library

http://www.thebuzzmedia.com/software/imgscalr-java-image-scaling-library/

See imgscalr in action at [imgscalr.com](http://imgscalr.com)!


Description
-----------
A class implementing performant (hardware accelerated), good-looking and 
intelligent image-scaling algorithms in pure Java 2D. This class implements the 
Java2D "best practices" when it comes to scaling images as well as Chris 
Campbell's incremental scaling algorithm proposed as the best method for 
down-sizes images for use as thumbnails (along with some additional minor 
optimizations).

imgscalr also provides support for applying arbitrary BufferedImageOps against
resultant images directly in the library.

TIP: imgscalr provides a default "anti-aliasing" Op that will very lightly soften
an image; this was a common request. Check `Scalr.OP_ANTIALIAS`!

TIP: All resizing operations maintain the original images proportions.

TIP: You can ask imgscalr to fit an image to a specific width or height regardless 
of its orientation using a Mode argument.

This class attempts to make scaling images in Java as simple as possible by providing
a handful of approaches tuned for scaling as fast as possible or as best-looking 
as possible and the ability to let the algorithm choose for you to optionally create 
the best-looking scaled image as fast as possible without boring you with the details 
if you don't want them.


Example
-------
In the simplest use-case where an image needs to be scaled to proportionally fit 
a specific width (say 150px for a thumbnail) and the class is left to decide which 
method will look the best, the code would look like this:

    BufferedImage srcImage = ImageIO.read(...); // Load image
    BufferedImage scaledImage = Scalr.resize(srcImage, 150); // Scale image

You could even flatten that out further if you simply wanted to scale the image 
and write out the scaled result immediately to a single line:

    ImageIO.write(Scalr.resize(ImageIO.read(...), 150));


Working with GIFs
-----------------
Java's support for writing GIF is... terrible. In Java 5 is was patent-encumbered
which made it mostly totally broken. In Java 6 the quantizer used to downsample
colors to the most accurate 256 colors was fast but inaccurate, yielding 
poor-looking results. The handling of an alpha channel (transparency) while writing
out GIF files (e.g. ImageIO.write(...)) was non-existent in Java 5 and in Java 6
would remove the alpha channel completely and replace it with solid BLACK.

In Java 7, support for writing out the alpha channel was added but unfortunately
many of the remaining image operations (like ConvoleOp) still corrupt the
resulting image when written out as a GIF.

NOTE: Support for scaling animated GIFs don't work at all in any version.

My recommendation for working with GIFs is as follows in order of preference:

1. Save the resulting BufferedImage from imgscalr as a PNG; it looks
better as no quantizer needs to be used to cull down the color space and 
transparency is maintained. 

2. If you mostly need GIF, check the resulting BufferedImage.getType() to see
if it is TYPE_INT_RGB (no transparency) or TYPE_INT_ARGB (transparency); if the
type is ARGB, then save the image as a PNG to maintain the alpha channel, if not,
you can safely save it as a GIF.

3. If you MUST have GIF, upgrade your runtime to Java 7 and save your images as
GIF. If you run Java 6, any GIF using transparency will have the transparent
channel replaced with BLACK and in Java 5 I think the images will most all be
corrupt/invalid.

REMINDER: Even in Java 7, applying some BufferedImageOps (like ConvolveOp) to
the scaled GIF before saving it totally corrupts it; so you would need to avoid
that if you didn't want to save it as a PNG. If you decide to save as a PNG, you
can apply any Ops you want.


License
-------
This library is released under the Apache 2 License. See `LICENSE`.
 

Troubleshooting
---------------
Image-manipulation in Java can take more memory than the size of the source image
because the image has to be "decoded" into raw ARGB bytes when loaded into the
BufferedImage instance; fortunately on most platforms this is a hardware-accelerated
operation by the video card.

If you are running into OutOfMemoryExceptions when using this library (e.g. if you
dealing with 10+ MB source images from an ultra-high-MP DSLR) try and up the 
heap size using the "-Xmx" command line argument to your Java process.

An example of how to do this looks like:

    java -Xmx128m com.site.MyApp
  

Reference
---------
Chris Campbell Incremental Scaling - http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html


Related Projects
----------------
ExifTool for Java - http://www.thebuzzmedia.com/software/exiftool-enhanced-java-integration-for-exiftool/


Contact
-------
If you have questions, comments or bug reports for this software please contact
us at: software@thebuzzmedia.com
