package org.imgscalr;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ScalrApplyTest.class, ScalrCropTest.class, ScalrPadTest.class,
		ScalrResizeTest.class, ScalrRotateTest.class })
public class AllTests {
	// no-op
}