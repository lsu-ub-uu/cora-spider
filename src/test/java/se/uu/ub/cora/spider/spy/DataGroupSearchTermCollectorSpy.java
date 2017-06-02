package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.searchtermcollector.DataGroupSearchTermCollector;

public class DataGroupSearchTermCollectorSpy implements DataGroupSearchTermCollector {
	public boolean collectSearchTermsWasCalled = false;
	public String metadataId = null;

	public DataGroup collectedSearchTerms = DataGroup.withNameInData("collectedDataLinks");

	@Override
	public DataGroup collectSearchTerms(String metadataId, DataGroup dataGroup) {
		this.metadataId = metadataId;
		collectSearchTermsWasCalled = true;
		return collectedSearchTerms;
	}
}
