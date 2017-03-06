package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class RecordSearchSpy implements RecordSearch {

	List<List<String>> listOfLists = new ArrayList<>();
	List<DataGroup> listOfSearchData = new ArrayList<>();

	@Override
	public Collection<DataGroup> searchUsingListOfRecordTypesToSearchInAndSearchData(
			List<String> list, DataGroup searchData) {
		listOfLists.add(list);
		listOfSearchData.add(searchData);
		return null;
	}

}
