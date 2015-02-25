package epc.spider.record.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.spider.record.storage.RecordNotFoundException;

public class RecordNotFoundExceptionTest {
	@Test
	public void testInit() {
		RecordNotFoundException notFound = new RecordNotFoundException("message");
		
		Assert.assertEquals(notFound.getMessage(), "message");
	}
}
