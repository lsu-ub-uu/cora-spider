/*
 * Copyright 2025 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.spider.record.internal;

import java.util.List;

import se.uu.ub.cora.bookkeeper.decorator.DataDecarator;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.DecoratedRecordReader;
import se.uu.ub.cora.spider.record.RecordReader;

public class DecoratedRecordReaderImp implements DecoratedRecordReader {

	private static final int MAX_DEPTH = 2;

	public static DecoratedRecordReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new DecoratedRecordReaderImp(dependencyProvider);
	}

	private SpiderDependencyProvider dependencyProvider;
	private RecordReader recordReader;
	private DataDecarator decorator;

	private DecoratedRecordReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public DataRecord readDecoratedRecord(String authToken, String type, String id) {
		recordReader = SpiderInstanceProvider.getRecordReader();
		decorator = dependencyProvider.getDataDecorator();
		return readDecoratedRecordRecursive(authToken, type, id, 0);
	}

	private DataRecord readDecoratedRecordRecursive(String authToken, String type, String id,
			int depth) {
		DataRecord recordToDecorate = readRecordFromStorage(authToken, type, id);
		String definitionId = getDefinitionId(type);
		decorator.decorateRecord(definitionId, recordToDecorate);
		if (maxDepthNotReached(depth)) {
			loopChildren(authToken, depth, recordToDecorate);
		}
		return recordToDecorate;
	}

	private void loopChildren(String authToken, int depth, DataRecord recordToDecorate) {
		DataRecordGroup dataRecordGroup = recordToDecorate.getDataRecordGroup();
		DataGroup dataGroup = DataProvider.createGroupFromRecordGroup(dataRecordGroup);
		loopChildrenAndAddLinkRecordIntoRecordLinks(authToken, dataGroup, depth);
	}

	private boolean maxDepthNotReached(int depth) {
		return depth < MAX_DEPTH;
	}

	private void loopChildrenAndAddLinkRecordIntoRecordLinks(String authToken, DataGroup dataGroup,
			int depth) {
		loopGroups(authToken, dataGroup, depth);
		loopLinks(authToken, dataGroup, depth);
	}

	private void loopGroups(String authToken, DataGroup dataGroup, int depth) {
		List<DataGroup> groups = dataGroup.getChildrenOfType(DataGroup.class);
		for (DataGroup group : groups) {
			loopChildrenAndSkipRecordInfoGroup(authToken, depth, group);
		}
	}

	private void loopChildrenAndSkipRecordInfoGroup(String authToken, int depth, DataGroup group) {
		if (!"recordInfo".equals(group.getNameInData())) {
			loopChildrenAndAddLinkRecordIntoRecordLinks(authToken, group, depth);
		}
	}

	private void loopLinks(String authToken, DataGroup dataGroup, int depth) {
		List<DataRecordLink> links = dataGroup.getChildrenOfType(DataRecordLink.class);
		for (DataRecordLink link : links) {
			possiblyReadLinksAndSetLinkedRecord(authToken, depth, link);
		}
	}

	private void possiblyReadLinksAndSetLinkedRecord(String authToken, int depth,
			DataRecordLink link) {
		if (link.hasReadAction()) {
			var linkedRecordAsGroup = readLinkedRecordAsDecoratedGroup(authToken, depth, link);
			link.setLinkedRecord(linkedRecordAsGroup);
		}
	}

	private DataGroup readLinkedRecordAsDecoratedGroup(String authToken, int depth,
			DataRecordLink link) {
		var linkedRecord = readDecoratedRecordRecursive(authToken, link.getLinkedRecordType(),
				link.getLinkedRecordId(), depth + 1);
		return DataProvider.createGroupFromRecordGroup(linkedRecord.getDataRecordGroup());
	}

	private DataRecord readRecordFromStorage(String authToken, String type, String id) {
		return recordReader.readRecord(authToken, type, id);
	}

	private String getDefinitionId(String type) {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(type);
		return recordTypeHandler.getDefinitionId();
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
