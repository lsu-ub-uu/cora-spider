package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;

public class DataGroupTermCollectorSpy implements DataGroupTermCollector {
	public boolean collectTermsWasCalled = false;
	public String metadataId = null;
	public DataGroup dataGroup;

	public DataGroup collectedTerms = DataGroup.withNameInData("collectedData");

	@Override
	public DataGroup collectTerms(String metadataId, DataGroup dataGroup) {
		this.metadataId = metadataId;
		this.dataGroup = dataGroup;
		collectTermsWasCalled = true;
		return collectedTerms;
	}
}
