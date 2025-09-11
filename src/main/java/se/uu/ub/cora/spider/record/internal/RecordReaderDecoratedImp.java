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

import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.RecordDecorator;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordReaderDecorated;

public class RecordReaderDecoratedImp implements RecordReaderDecorated {

	public static RecordReaderDecoratedImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new RecordReaderDecoratedImp(dependencyProvider);
	}

	private SpiderDependencyProvider dependencyProvider;

	private RecordReaderDecoratedImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public DataRecord readDecoratedRecord(String authToken, String type, String id) {
		RecordReader recordReader = SpiderInstanceProvider.getRecordReader();
		DataRecord recordToDecorate = recordReader.readRecord(authToken, type, id);
		RecordDecorator recordDecorator = dependencyProvider.getRecordDecorator();
		recordDecorator.decorateRecord(recordToDecorate, authToken);
		return recordToDecorate;
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
