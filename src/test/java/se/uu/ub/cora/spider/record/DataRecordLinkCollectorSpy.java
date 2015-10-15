package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.linkcollector.DataRecordLinkCollector;

public class DataRecordLinkCollectorSpy implements DataRecordLinkCollector {

	public boolean collectLinksWasCalled = false;

	@Override
	public DataGroup collectLinks(String metadataId, DataGroup dataGroup, String fromRecordType,
			String fromRecordId) {
		collectLinksWasCalled = true;
		DataGroup collectedDataLinks = DataGroup.withNameInData("collectedDataLinks");
		return collectedDataLinks;
	}

}
