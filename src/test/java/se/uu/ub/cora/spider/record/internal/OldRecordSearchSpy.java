/// *
// * Copyright 2017, 2018 Uppsala University Library
// *
// * This file is part of Cora.
// *
// * Cora is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Cora is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Cora. If not, see <http://www.gnu.org/licenses/>.
// */
// package se.uu.ub.cora.spider.record.internal;
//
// import java.util.ArrayList;
// import java.util.List;
//
// import se.uu.ub.cora.data.DataGroup;
// import se.uu.ub.cora.search.RecordSearch;
// import se.uu.ub.cora.search.SearchResult;
// import se.uu.ub.cora.spider.testdata.DataCreator2;
// import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
//
// public class OldRecordSearchSpy implements RecordSearch {
//
// List<List<String>> listOfLists = new ArrayList<>();
// List<DataGroup> listOfSearchData = new ArrayList<>();
// public DataGroup place44 = DataCreator2
// .createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("place", "place44",
// "place", "systemOne", "someUserId");
// public long totalNumberOfMatches = 1;
//
// public MethodCallRecorder MCR = new MethodCallRecorder();
//
// @Override
// public SearchResult searchUsingListOfRecordTypesToSearchInAndSearchData(List<String> list,
// DataGroup searchData) {
// MCR.addCall("list", list, "searchData", searchData);
// listOfLists.add(list);
// listOfSearchData.add(searchData);
//
// SearchResult spiderSearchResult = new SearchResult();
// spiderSearchResult.listOfDataGroups = new ArrayList<>();
// spiderSearchResult.listOfDataGroups.add(place44);
// spiderSearchResult.totalNumberOfMatches = totalNumberOfMatches;
//
// if (list.contains("image")) {
// DataGroup image44 = DataCreator2
// .createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("image",
// "image44", "image", "systemOne", "someUserId");
// spiderSearchResult.listOfDataGroups.add(image44);
//
// DataGroup image45 = DataCreator2
// .createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("binary",
// "binary45", "binary", "systemOne", "someUserId");
// spiderSearchResult.listOfDataGroups.add(image45);
// }
// MCR.addReturned(spiderSearchResult);
// return spiderSearchResult;
// }
//
// }
