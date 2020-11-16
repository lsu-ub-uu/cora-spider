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

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
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
	 * If a searchId does not exist, a {@link DataMissingException} MUST be thrown.
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

	/**
	 * getCombinedIdsUsingRecordId returns a list of combined ids for a specified recordId based on
	 * the recordType the recordTypeHandler currently handles. The returned list will contain two
	 * ids if the recordType has an abstract parent and only one if the recordType does not have a
	 * parent.<br>
	 * <br>
	 * The ids are a combination of the current recordTypes id and the entered recordId, for
	 * example:<br>
	 * the recordTypeHandler is currently used to handle the recordType metadataGroup and an id
	 * "someGroupId" is entered, the generated ids will be:<br>
	 * metadataGroup_someGroupId<br>
	 * metadata_someGroupId
	 * 
	 * @param recordId
	 *            A String with the recordId to get a list of combined ids for
	 * @return A List of combined recordIds using the format recordTypeId_recordId
	 */
	List<String> getCombinedIdsUsingRecordId(String recordId);

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
	 * hasRecordPartCreateConstraint is used to check if the record has constraints on its
	 * recordParts on CREATE action. If a user has read constraints on a recordPart is it implied
	 * that the user also has write constraint on that part.
	 * 
	 * @return A boolean, true if the record has at least one recordPart with write constraint, else
	 *         false
	 */
	boolean hasRecordPartCreateConstraint();

	/**
	 * getRecordPartReadConstraints returns a Set with all the read constraints for the recordType.
	 * Read constraints internally have the value "readWrite" as a read constraint also implies a
	 * write constraint. The constraints are identified by nameInData, where nameInData is the name
	 * in data for the child in the top level dataGroup that is limited by the constraint.
	 * 
	 * @return A Set filled with read constraints, more precisly nameInData and possibly attributes
	 *         for children to the top level dataGroup that has read constraints. If there are no
	 *         read constraints an empty set SHOULD be returned.
	 */
	Set<Constraint> getRecordPartReadConstraints();

	/**
	 * getRecordPartWriteConstraints returns a Set with all the write constraints that are specified
	 * in the dataGroup specified by metadataId in the recordType. Write constraints have the value
	 * "write", or "readWrite" as a read constraint also implies a write constraint. The constraints
	 * are identified by nameInData, where nameInData is the name in data for the child in the top
	 * level dataGroup that is limited by the constraint.
	 * <p>
	 * This method is similar to {@link #getRecordPartCreateWriteConstraints()}, but this one
	 * returnes constraints active when updating data.
	 * 
	 * @return A Set filled with write constraints, more precisly nameInData and possibly attributes
	 *         for children to the top level dataGroup that has write constraints. If there are no
	 *         write constraints an empty set SHOULD be returned.
	 */
	Set<Constraint> getRecordPartWriteConstraints();

	/**
	 * getRecordPartCreateWriteConstraints returns a Set with all the write constraints that are
	 * specified in the dataGroup specified by newMetadataId in the recordType. Write constraints
	 * have the value "write", or "readWrite" as a read constraint also implies a write constraint.
	 * The constraints are identified by nameInData, where nameInData is the name in data for the
	 * child in the top level dataGroup that is limited by the constraint.
	 * <p>
	 * This method is similar to {@link #getRecordPartCreateWriteConstraints()}, but this one
	 * returnes constraints active when creating data.
	 * 
	 * @return A Set filled with write constraints, more precisly nameInData and possibly attributes
	 *         for children to the top level dataGroup that has write constraints. If there are no
	 *         write constraints an empty set SHOULD be returned.
	 */
	Set<Constraint> getRecordPartCreateWriteConstraints();

	/**
	 * getImplementingRecordTypeHandlers should return a List of {@link RecordTypeHandler} for all
	 * recordTypes that has the recordType that this recordTypeHandler handles as its parent.<br>
	 * If this recordType is an implementing type or a type without a parent should an empty list be
	 * returned. If this recordType is abstract but no implementing recordTypes exist should an
	 * empty list be returned. If this recordType is abstract and implementing recordTypes exists,
	 * should a list of the recordTypeHandlers for the implementing recordTypes be returned.
	 * 
	 * @return a list with RecordTypeHandlers representing recordTypes that implements the current
	 *         recordType
	 */
	List<RecordTypeHandler> getImplementingRecordTypeHandlers();

	/**
	 * getRecordTypeId returns the id of the recordType handled by the RecordTypeHandler
	 * 
	 * @return recordTypeId as a String
	 */
	String getRecordTypeId();

}