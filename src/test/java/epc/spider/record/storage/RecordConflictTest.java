package epc.spider.record.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RecordConflictTest {
	@Test
	public void testInit() {
		RecordConflictException conflict = new RecordConflictException("message");
		
		Assert.assertEquals(conflict.getMessage(), "message");
	}
}
