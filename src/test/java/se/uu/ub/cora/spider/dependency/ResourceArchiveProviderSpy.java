package se.uu.ub.cora.spider.dependency;

import java.util.Map;

import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.RecordArchiveProvider;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class ResourceArchiveProviderSpy implements RecordArchiveProvider {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public ResourceArchiveProviderSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getOrderToSelectImplementionsBy", () -> 0);
		MRV.setDefaultReturnValuesSupplier("getRecordArchive", ResourceArchiveSpy::new);
	}

	@Override
	public int getOrderToSelectImplementionsBy() {
		return (int) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public void startUsingInitInfo(Map<String, String> initInfo) {
		MCR.addCall("initInfo", initInfo);
	}

	@Override
	public RecordArchive getRecordArchive() {
		return (RecordArchive) MCR.addCallAndReturnFromMRV();
	}

}
