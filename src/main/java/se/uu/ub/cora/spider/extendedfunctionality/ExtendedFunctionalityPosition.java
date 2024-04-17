/*
 * Copyright 2020, 2021, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extendedfunctionality;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.storage.RecordStorage;

/**
 * ExtendedFunctionalityPosition is an Enum with the positions from where Spider calls
 * extendedFunctionality.<br>
 * The position is the combination of action and position within spiders execution of this action,
 * the reason they are connected as one is that not all actions have all internal positions. <br>
 * <br>
 * For create are the positions ordered as follows:<br>
 * CREATE_AFTER_AUTHORIZATION<br>
 * CREATE_AFTER_METADATA_VALIDATION<br>
 * CREATE_BEFORE_ENHANCE<br>
 * <br>
 * For read are the positions ordered as follows:<br>
 * READ_BEFORE_RETURN<br>
 * <br>
 * For update are the positions ordered as follows:<br>
 * UPDATE_AFTER_AUTHORIZATION<br>
 * UPDATE_BEFORE_METADATA_VALIDATION<br>
 * UPDATE_AFTER_METADATA_VALIDATION<br>
 * UPDATE_BEFORE_STORE<br>
 * UPDATE_AFTER_STORE<br>
 * UPDATE_BEFORE_RETURN<br>
 * <br>
 * For delete are the positions ordered as follows:<br>
 * DELETE_BEFORE<br>
 * DELETE_AFTER<br>
 */
public enum ExtendedFunctionalityPosition {
	/**
	 * CREATE_AFTER_AUTHORIZATION, as the name suggests is the functionality plugged into a create
	 * operation, right after the user is authorized for the create action on the recordType.
	 */
	CREATE_AFTER_AUTHORIZATION,

	/**
	 * CREATE_AFTER_METADATA_VALIDATION, as the name suggests is the functionality plugged into a
	 * create operation, after metadataValidation is done.
	 */
	CREATE_AFTER_METADATA_VALIDATION,

	/**
	 * CREATE_BEFORE_ENHANCE, as the name suggests is the functionality plugged into a create
	 * operation, before the method returns.
	 */
	CREATE_BEFORE_ENHANCE,

	/**
	 * READ_AFTER_AUTHORIZATION, as the name suggests is the functionality plugged into a update
	 * operation, before metadataValidation is done.
	 */
	READ_AFTER_AUTHORIZATION,

	/**
	 * READ_BEFORE_RETURN, as the name suggests is the functionality plugged into a read operation,
	 * before the method returns.
	 */
	READ_BEFORE_RETURN,

	/**
	 * UPDATE_AFTER_AUTHORIZATION, as the name suggests is the functionality plugged into a update
	 * operation, before metadataValidation is done.
	 */
	UPDATE_AFTER_AUTHORIZATION,

	/**
	 * UPDATE_BEFORE_METADATA_VALIDATION, as the name suggests is the functionality plugged into a
	 * update operation, before metadataValidation is done.
	 */
	UPDATE_BEFORE_METADATA_VALIDATION,

	/**
	 * UPDATE_AFTER_METADATA_VALIDATION, as the name suggests is the functionality plugged into a
	 * update operation, after metadataValidation is done.
	 */
	UPDATE_AFTER_METADATA_VALIDATION,

	/**
	 * UPDATE_BEFORE_STORE, as the name suggests is the functionality plugged into a update
	 * operation, right before data is updated in storage, using the
	 * {@link RecordStorage#update(String, String, DataGroup, DataGroup, DataGroup, String)} method.
	 */
	UPDATE_BEFORE_STORE,

	/**
	 * UPDATE_AFTER_STORE, as the name suggests is the functionality plugged into a update
	 * operation, right after data is updated in storage, using the
	 * {@link RecordStorage#update(String, String, DataGroup, DataGroup, DataGroup, String)} method.
	 */
	UPDATE_AFTER_STORE,

	/**
	 * UPDATE_BEFORE_RETURN, as the name suggests is the functionality plugged into a update
	 * operation, before the method returns.
	 */
	UPDATE_BEFORE_RETURN,

	/**
	 * DELETE_AFTER_AUTHORIZATION, as the name suggests is the functionality plugged into a delete
	 * operation, right after the user is authorized for the create action on the recordType.
	 */
	DELETE_AFTER_AUTHORIZATION,

	/**
	 * DELETE_BEFORE, as the name suggests is the functionality plugged into a delete operation,
	 * before it is done.
	 */
	DELETE_BEFORE,

	/**
	 * DELETE_AFTER, as the name suggests is the functionality plugged into a delete operation,
	 * after it is done.
	 */
	DELETE_AFTER
}
