package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordSearchSpy implements RecordSearch {

	List<List<String>> listOfLists = new ArrayList<>();
	List<DataGroup> listOfSearchData = new ArrayList<>();

	@Override
	public SpiderSearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			DataGroup searchData) {
		listOfLists.add(list);
		listOfSearchData.add(searchData);

		SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
		spiderSearchResult.listOfDataGroups = new ArrayList<>();

		SpiderDataGroup place44 = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("place44",
						"placeId", "place", "systemOne", "someUserId");
		spiderSearchResult.listOfDataGroups.add(place44.toDataGroup());
		return spiderSearchResult;
	}

}
