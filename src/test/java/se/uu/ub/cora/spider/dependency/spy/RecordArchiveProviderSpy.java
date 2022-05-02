package se.uu.ub.cora.spider.dependency.spy;

import java.util.Map;

import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.RecordArchiveProvider;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordArchiveProviderSpy implements RecordArchiveProvider {

	MethodCallRecorder MCR = new MethodCallRecorder();
	int order = 0;

	@Override
	public int getOrderToSelectImplementionsBy() {
		MCR.addCall();

		MCR.addReturned(order);
		return order;
	}

	@Override
	public void startUsingInitInfo(Map<String, String> initInfo) {
		MCR.addCall("initInfo", initInfo);
	}

	@Override
	public RecordArchive getRecordArchive() {

		return null;
	}

}
