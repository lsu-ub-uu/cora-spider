/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.dependency;

import java.util.List;

import se.uu.ub.cora.spider.record.RecordTypeHandler;

public class RecordTypeHandlerSpy implements RecordTypeHandler {

	public boolean isPublicForRead = true;
	public boolean isAbstract = false;
	public boolean recordTypeHasReadPartLimitations = false;
	public String recordPartConstraint = "";

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean shouldAutoGenerateId() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNewMetadataId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMetadataId() {
		// TODO Auto-generated method stub
		return "fakeMetadataIdFromRecordTypeHandlerSpy";
	}

	@Override
	public List<String> createListOfPossibleIdsToThisRecord(String recordId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPublicForRead() {
		return isPublicForRead;
	}

}
