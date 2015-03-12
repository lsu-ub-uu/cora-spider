package epc.spider.record;

import java.util.Optional;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.spider.data.SpiderDataGroup;

public class PermissionKeyCalculatorTest {
	@Test
	public void testGeneratePermissionKey() {
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");

		Set<String> keys = keyCalculator.calculateKeys("READ", "recordType", recordInfo);
		Optional<String> key = keys.stream().findFirst();
		Assert.assertEquals(key.get(), "READ:RECORDTYPE:SYSTEM:*",
				"Key should be calculated to match example");

	}
}
