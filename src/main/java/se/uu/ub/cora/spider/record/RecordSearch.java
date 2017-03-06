package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public interface RecordSearch {

	Collection<DataGroup> searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
			DataGroup searchData);

}
