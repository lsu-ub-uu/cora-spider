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

package se.uu.ub.cora.spider.getmetadata.testdata;

import se.uu.ub.cora.bookkeeper.metadata.MetadataChildReference;
import se.uu.ub.cora.bookkeeper.metadata.MetadataGroup;
import se.uu.ub.cora.bookkeeper.metadata.MetadataHolder;
import se.uu.ub.cora.bookkeeper.metadata.MetadataHolderImp;
import se.uu.ub.cora.bookkeeper.metadata.TextVariable;

public class TestDataAuthority {
	public static MetadataHolder createTestAuthorityMetadataHolder() {

		MetadataHolder metadataHolder = new MetadataHolderImp();

		// otherVariable
		String regularExpression = "((^(([0-1][0-9])|([2][0-3])):[0-5][0-9]$|^$){1}";
		TextVariable otherVariable = TextVariable
				.withIdAndNameInDataAndTextIdAndDefTextIdAndRegularExpression("otherId", "otherNameInData",
						"otherTextId", "otherDefTextId", regularExpression);
		metadataHolder.addMetadataElement(otherVariable);

		// authorityGroup
		MetadataGroup authorityGroup = MetadataGroup.withIdAndNameInDataAndTextIdAndDefTextId(
				"authority", "authorityNameInData", "authorityTextId", "authorityDefTextId");
		metadataHolder.addMetadataElement(authorityGroup);
		MetadataChildReference otherReference = MetadataChildReference
				.withLinkedRecordTypeAndLinkedRecordIdAndRepeatMinAndRepeatMax("metadataTextVariable", "otherTextId", 1,
						MetadataChildReference.UNLIMITED);
		authorityGroup.addChildReference(otherReference);

		return metadataHolder;
	}

}
