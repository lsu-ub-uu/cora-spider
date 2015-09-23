package epc.spider.record;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class MisuseExceptionTest {
	@Test
	public void testInit() {
		MisuseException exception = new MisuseException("message");
		assertEquals(exception.getMessage(), "message");
	}

	@Test
	public void testInitWithError() {
		Exception exception = new Exception();
		MisuseException misuseException = new MisuseException("message", exception);
		assertEquals(misuseException.getMessage(), "message");
		assertEquals(misuseException.getCause(), exception);
	}
}
