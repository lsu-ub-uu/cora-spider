/*
 * Copyright 2016, 2017, 2019, 2020 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;

/**
 * RecordTypeHandler is a class that mostly handles information about a recordType, but also has
 * some extra utility methods around recordTypes.
 */
public interface RecordTypeHandler {

	/**
	 * isAbstract checks if the recordType is abstract.
	 * 
	 * @return A boolean, true if the recordType is abstract
	 */
	boolean isAbstract();

	/**
	 * hasParent returns if the recordType has a parent recordType or not.
	 * 
	 * @return A boolean, true if the recordType has a parent
	 */
	public boolean hasParent();

	/**
	 * isChildOfBinary returns if the recordType has a parent recordType that is binary (handles
	 * binary data).
	 * 
	 * @return A boolean, true if the recordType has binary as parentRecordType
	 */
	public boolean isChildOfBinary();

	/**
	 * representsTheRecordTypeDefiningSearches returns if this recordType is the one defining
	 * searches. (this recordTypes id is search)
	 * 
	 * @return A boolean, true if this recordType defines searches
	 */
	boolean representsTheRecordTypeDefiningSearches();

	/**
	 * representsTheRecordTypeDefiningRecordTypes returns if this recordType is the one defining
	 * recordTypes. (this recordTypes id is recordType)
	 * 
	 * @return A boolean, true if this recordType defines recordTypes
	 */
	boolean representsTheRecordTypeDefiningRecordTypes();

	/**
	 * hasLinkedSearch returns if the recordType has a link to a search or not.
	 * 
	 * @return A boolean, true if the recordType is recordType type
	 */
	boolean hasLinkedSearch();

	/**
	 * getSearchId returns the id of the linked search.
	 * 
	 * @return A String with the recordId of the linked search record
	 */
	String getSearchId();

	/**
	 * getParentId returns the parentId for the recordType if it has one, if this recordType does
	 * not have a parent should an exception be thrown indicating that no parentId can be found.
	 * 
	 * @return A String with this recordTypes parentId, if it has one
	 */
	public String getParentId();

	boolean shouldAutoGenerateId();

	/**
	 * getNewMetadataId returns the metadataId for the top level dataGroup to use for validating
	 * data when creating a new record, used in the specified recordType in RecordTypeHandler
	 * 
	 * @return String with metadataId
	 */
	String getNewMetadataId();

	/**
	 * getMetadataId returns the metadataId for the top level dataGroup to use for validating data
	 * when updating data, used in the specified recordType in RecordTypeHandler
	 * 
	 * @return String with metadataId
	 */
	String getMetadataId();

	/**
	 * getMetadataGroup is used to get the metadata group as a DataGroup using the implementation of
	 * {@link #getMetadataId()}.
	 * 
	 * @return DataGroup of the recordPart used in the RecordTypeHandler
	 */
	DataGroup getMetadataGroup();

	List<String> createListOfPossibleIdsToThisRecord(String recordId);

	/**
	 * isPublicForRead is used to check if the record has been marked as PublicForRead which implies
	 * that the record is totally public and exists no restrictions on the record.
	 * 
	 * @return If record is PublicForRead or not
	 */
	boolean isPublicForRead();

	/**
	 * hasRecordPartReadWriteConstraint is used to check if the record has read constraints on its
	 * recordParts.
	 * 
	 * @return A boolean, true if the record has at least one recordPart with read constraint, else
	 *         false
	 */
	boolean hasRecordPartReadConstraint();

	/**
	 * hasRecordPartWriteConstraint is used to check if the record has write constraints on its
	 * recordParts. If a user has read constraints on a recordPart is it implied that the user also
	 * has write constraint on that part.
	 * 
	 * @return A boolean, true if the record has at least one recordPart with write constraint, else
	 *         false
	 */
	boolean hasRecordPartWriteConstraint();

	/**
	 * getRecordPartReadConstraints returns a Map with all the read constraints for the recordType.
	 * Read constraints have the value "readWrite" as a read constraint also implies a write
	 * constraint. The constraints are stored in the map under the key, nameInData, where nameInData
	 * is the name in data for the child in the top level dataGroup that is limited by the
	 * constraint.
	 * 
	 * @return Map filled with read constraints key = nameInData Value = "readWrite"
	 */
	Set<String> getRecordPartReadConstraints();

	/**
	 * getRecordPartWriteConstraints returns a Map with all the write constraints for the
	 * recordType. Write constraints have the value "write", or "readWrite" as a read constraint
	 * also implies a write constraint. The constraints are stored in the map under the key,
	 * nameInData, where nameInData is the name in data for the child in the top level dataGroup
	 * that is limited by the constraint.
	 * 
	 * @return Map filled with write constraints key = nameInData Value = "write" or "readWrite"
	 */
	Set<String> getRecordPartWriteConstraints();

}