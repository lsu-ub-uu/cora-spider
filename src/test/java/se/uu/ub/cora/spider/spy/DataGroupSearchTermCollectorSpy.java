package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.searchtermcollector.DataGroupTermCollector;

public class DataGroupSearchTermCollectorSpy implements DataGroupTermCollector {
	public boolean collectSearchTermsWasCalled = false;
	public String metadataId = null;

	public DataGroup collectedSearchTerms = DataGroup.withNameInData("collectedDataLinks");

	@Override
	public DataGroup collectTerms(String metadataId, DataGroup dataGroup) {
		this.metadataId = metadataId;
		collectSearchTermsWasCalled = true;
		return collectedSearchTerms;
	}
}
