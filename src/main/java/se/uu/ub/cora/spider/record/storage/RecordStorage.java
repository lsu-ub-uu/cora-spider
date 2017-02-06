/*
 * Copyright 2015 Uppsala University Library
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

package se.uu.ub.cora.spider.record.storage;

import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public interface RecordStorage {

	DataGroup read(String type, String id);

	void create(String type, String id, DataGroup record, DataGroup linkList, String dataDivider);

	void deleteByTypeAndId(String type, String id);

	boolean linksExistForRecord(String type, String id);

	void update(String type, String id, DataGroup record, DataGroup linkList, String dataDivider);

	Collection<DataGroup> readList(String type);

	DataGroup readLinkList(String type, String id);

	Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id);

	boolean recordsExistForRecordTypeOrAbstract(String type);

	boolean recordExistsForRecordTypeOrAbstractAndRecordId(String type, String id);
}
