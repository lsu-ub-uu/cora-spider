/*
 * Copyright 2021 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

/**
 * RecordListIndexer is used to index multiple records from a specified recordType using some index
 * settings possibly containing an index filter used to only index part of the records for the
 * specified recordType.
 */
public interface RecordListIndexer {
	/**
	 * indexRecordList indexes records for the specified recordType using settings found in
	 * indexSettings.
	 * <p>
	 * Implementations MUST ensure that the supplied authToken has been issued to a user that is
	 * still loggedin and active (has not timed out). This can be done using the
	 * {@link Authenticator#getUserForToken(String)} method or similar. An
	 * {@link AuthenticationException} MUST be throw if the provided authToken is not issues to any
	 * user, or if the user has been deactivated in storage, or if the user has logged out, or if
	 * the user is no longer active. If the user is active or not is determined by the part of the
	 * system that provided the authToken to the User.
	 * <p>
	 * TODO: does both authenticator and authorizator ensure that the user is active in storage, if
	 * so twice is a bit much?
	 * <p>
	 * Implementations MUST ensure that the user represented by the authToken, is allowed to perform
	 * the action "index" on the specified recordType based on the rules the user is associated
	 * with. This can be done using the
	 * {@link SpiderAuthorizator#checkUserIsAuthorizedForActionOnRecordType(User, String, String)}
	 * method or similar. An {@link AuthorizationException} MUST be thrown if the user is not
	 * authorized to use the "index" method on the specified recordType or if the user is not active
	 * in storage.
	 * <p>
	 * Implementations MUST ensure that the supplied indexSettings dataGroup is validated so that it
	 * contains only valid input according to the metadataGroup specified as "indexSettings" by the
	 * given recordType. This can be done using the
	 * {@link DataValidator#validateIndexSettings(String, DataGroup)} method or similar. If the
	 * validation fails SHOULD an error be thrown and as much information as possible be returned as
	 * part of the error.
	 * <p>
	 * If the specified recordType does not have a indexSettings specified a DataValidationException
	 * MUST be thrown to indicate this.
	 * 
	 * @param authToken
	 *            a String with an authToken representing a user
	 * @param recordType
	 *            a String with the recordType for which records should be indexed
	 * @param indexSettings
	 *            a DataGroup with indexSettings
	 * @return
	 */
	DataRecord indexRecordList(String authToken, String recordType, DataGroup indexSettings);
	// private Response handleError(String authToken, Exception error) {
	//
	// if (error instanceof RecordConflictException) {
	// return buildResponseIncludingMessage(error, Response.Status.CONFLICT);
	// }
	//
	// if (error instanceof MisuseException) {
	// return buildResponseIncludingMessage(error, Response.Status.METHOD_NOT_ALLOWED);
	// }
	//
	// if (errorIsCausedByDataProblem(error)) {
	// return buildResponseIncludingMessage(error, Response.Status.BAD_REQUEST);
	// }
	//
	// if (error instanceof RecordNotFoundException) {
	// return buildResponseIncludingMessage(error, Response.Status.NOT_FOUND);
	// }
	//
	// if (error instanceof URISyntaxException) {
	// return buildResponse(Response.Status.BAD_REQUEST);
	// }
	//
	// if (error instanceof AuthorizationException) {
	// return handleAuthorizationException(authToken);
	// }
	//
	// if (error instanceof AuthenticationException) {
	// return buildResponse(Response.Status.UNAUTHORIZED);
	// }
	// log.logErrorUsingMessageAndException("Error handling request: " + error.getMessage(),
	// error);
	// return buildResponseIncludingMessage(error, Response.Status.INTERNAL_SERVER_ERROR);
	// }
	//
	// private boolean errorIsCausedByDataProblem(Exception error) {
	// return error instanceof JsonParseException || error instanceof DataException
	// || error instanceof ConverterException || error instanceof DataMissingException;
	// }
}
