package se.uu.ub.cora.spider.record.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.storage.RecordConflictException;

public class RecordConflictTest {
	@Test
	public void testInit() {
		RecordConflictException conflict = new RecordConflictException("message");
		
		Assert.assertEquals(conflict.getMessage(), "message");
	}
}
