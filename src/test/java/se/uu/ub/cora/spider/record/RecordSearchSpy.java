package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordSearchSpy implements RecordSearch {

	List<List<String>> listOfLists = new ArrayList<>();
	List<SpiderDataGroup> listOfSearchData = new ArrayList<>();

	@Override
	public SpiderSearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			SpiderDataGroup searchData) {
		listOfLists.add(list);
		listOfSearchData.add(searchData);

		SpiderSearchResult spiderSearchResult = new SpiderSearchResult();
		spiderSearchResult.listOfDataGroups = new ArrayList<>();

		SpiderDataGroup place44 = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("place",
						"place44", "place", "systemOne", "someUserId");
		spiderSearchResult.listOfDataGroups.add(place44.toDataGroup());

		if (list.contains("image")) {
			SpiderDataGroup image44 = DataCreator
					.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("image",
							"image44", "image", "systemOne", "someUserId");
			spiderSearchResult.listOfDataGroups.add(image44.toDataGroup());

			SpiderDataGroup image45 = DataCreator
					.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("image",
							"image45", "image", "systemOne", "someUserId");
			spiderSearchResult.listOfDataGroups.add(image45.toDataGroup());
		}
		return spiderSearchResult;
	}

}
