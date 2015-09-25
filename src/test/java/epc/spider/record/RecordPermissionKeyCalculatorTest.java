package epc.spider.record;

import java.util.Optional;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.metadataformat.data.DataGroup;

public class RecordPermissionKeyCalculatorTest {
	@Test
	public void testGeneratePermissionKey() {
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");

		Set<String> keys = keyCalculator.calculateKeys("READ", "recordType", recordInfo);
		Optional<String> key = keys.stream().findFirst();
		Assert.assertEquals(key.get(), "READ:RECORDTYPE:SYSTEM:*",
				"Key should be calculated to match example");
	}

	@Test
	public void testCalculateKeyForList() {
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		Set<String> keys = keyCalculator.calculateKeysForList("READ", "recordType");
		Optional<String> key = keys.stream().findFirst();
		Assert.assertEquals(key.get(), "READ:RECORDTYPE:SYSTEM:*",
				"Key should be calculated to match example");

	}
}
