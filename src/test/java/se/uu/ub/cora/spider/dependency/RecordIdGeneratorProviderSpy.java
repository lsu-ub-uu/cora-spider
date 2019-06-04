package se.uu.ub.cora.spider.dependency;

import java.util.Map;

import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordIdGeneratorProvider;

public class RecordIdGeneratorProviderSpy implements RecordIdGeneratorProvider {
	public RecordIdGenerator recordIdGenerator = new RecordIdGeneratorSpy();

	@Override
	public int getOrderToSelectImplementionsBy() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void startUsingInitInfo(Map<String, String> initInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public RecordIdGenerator getRecordIdGenerator() {
		return recordIdGenerator;
	}

}
