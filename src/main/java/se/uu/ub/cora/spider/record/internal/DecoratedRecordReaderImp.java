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

import se.uu.ub.cora.bookkeeper.decorator.DataDecarator;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.DecoratedRecordReader;
import se.uu.ub.cora.spider.record.RecordReader;

public class DecoratedRecordReaderImp implements DecoratedRecordReader {

	public static DecoratedRecordReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new DecoratedRecordReaderImp(dependencyProvider);
	}

	private SpiderDependencyProvider dependencyProvider;

	private DecoratedRecordReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public DataRecord readDecoratedRecord(String authToken, String type, String id) {
		DataRecord recordToDecorate = readRecordFromStorage(authToken, type, id);
		String definitionId = getDefinitionId(type);
		DataDecarator decorator = dependencyProvider.getDataDecorator();
		decorator.decorateRecord(definitionId, recordToDecorate);
		return recordToDecorate;
	}

	private DataRecord readRecordFromStorage(String authToken, String type, String id) {
		RecordReader recordReader = SpiderInstanceProvider.getRecordReader();
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
