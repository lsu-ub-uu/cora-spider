package epc.spider.record.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RecordIdGeneratorTest {
	@Test
	public void testGenerateId() {
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		String keyType = idGenerator.getIdForType("type");
		String keyType2 = idGenerator.getIdForType("type2");
		Assert.assertNotEquals(keyType, keyType2,
				"The generated keys should not be equal for two different types");
	}
}
