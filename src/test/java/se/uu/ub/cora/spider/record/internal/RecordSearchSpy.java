package se.uu.ub.cora.spider.record.internal;

import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordSearchSpy implements RecordSearch {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();
	private SearchResult searchResult;

	public RecordSearchSpy() {
		searchResult = new SearchResult();
		searchResult.listOfDataGroups = Collections.emptyList();
		searchResult.start = 0;
		searchResult.totalNumberOfMatches = 0;

		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("searchUsingListOfRecordTypesToSearchInAndSearchData",
				() -> searchResult);
	}

	@Override
	public SearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> recordTypes, DataGroup searchData) {
		return (SearchResult) MCR.addCallAndReturnFromMRV("recordTypes", recordTypes, "searchData",
				searchData);
	}

}
