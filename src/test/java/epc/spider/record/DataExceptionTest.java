package epc.spider.record;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class DataExceptionTest {
	@Test
	public void testInit() {
		DataException exception = new DataException("message");
		assertEquals(exception.getMessage(), "message");
	}

	@Test
	public void testInitWithError() {
		Exception exception = new Exception();
		DataException dataException = new DataException("message", exception);
		assertEquals(dataException.getMessage(), "message");
		assertEquals(dataException.getCause(), exception);
	}
}
