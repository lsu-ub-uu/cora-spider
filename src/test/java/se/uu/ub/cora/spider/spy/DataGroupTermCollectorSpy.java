package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;

public class DataGroupTermCollectorSpy implements DataGroupTermCollector {
	public boolean collectTermsWasCalled = false;
	public String metadataId = null;
	public DataGroup dataGroup;

	public DataGroup collectedTerms = DataGroup.withNameInData("collectedData");

	public List<DataGroup> dataGroups = new ArrayList<>();

	@Override
	public DataGroup collectTerms(String metadataId, DataGroup dataGroup) {
		this.metadataId = metadataId;
		this.dataGroup = dataGroup;
		collectTermsWasCalled = true;

		dataGroups.add(dataGroup);

		return collectedTerms;
	}
}
