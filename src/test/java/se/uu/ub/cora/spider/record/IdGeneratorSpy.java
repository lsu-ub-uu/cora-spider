package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;

public class IdGeneratorSpy implements RecordIdGenerator {

	public boolean getIdForTypeWasCalled = false;

	@Override
	public String getIdForType(String type) {
		getIdForTypeWasCalled = true;
		return "1";
	}

}
