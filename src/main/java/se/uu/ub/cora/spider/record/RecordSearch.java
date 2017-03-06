package se.uu.ub.cora.spider.record;

import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderSearchResult;

public interface RecordSearch {

	SpiderSearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			DataGroup searchData);

}
