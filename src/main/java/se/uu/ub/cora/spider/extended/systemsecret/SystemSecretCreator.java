/*
 * Copyright 2024 Uppsala University Library
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

package se.uu.ub.cora.spider.extended.systemsecret;

import se.uu.ub.cora.password.texthasher.TextHasher;

public interface SystemSecretCreator {

	/**
	 * createAndStoreSystemSecretRecord creates a systemSecret data group and stores it into
	 * storage. The data group contains a secret that is obtain from hashing the String secret using
	 * {@link TextHasher} and using tha dataDivider sent to the method.
	 * 
	 * @param secret
	 *            String to be hashed and stored into the systemSecret
	 * @param dataDivider
	 *            A String defining the dataDivider that the systemSecret belong
	 * @return A String with the record id of the newly created systemSecret.
	 */
	String createAndStoreSystemSecretRecord(String secret, String dataDivider);

}