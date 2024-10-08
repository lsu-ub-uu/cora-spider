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
package se.uu.ub.cora.spider.extended.apptoken;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

/**
 * AppTokenClearTextExtendedFuncionality set the field with nameInData "appTokenClearText", just
 * before returning the record, for all apptokens with this information added to the dataSharer by
 * the {@link AppTokenHandlerExtendedFunctionality}.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class AppTokenClearTextExtendedFuncionality implements ExtendedFunctionality {
	private static final String APP_TOKEN_CLEAR_TEXT_NAME_IN_DATA = "appTokenClearText";
	private Map<String, String> efSystemSecretIdAndClearTextToken;

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		Map<String, Object> dataSharer = data.dataSharer;
		efSystemSecretIdAndClearTextToken = (Map<String, String>) dataSharer
				.getOrDefault("AppTokenHandlerExtendedFunctionality", Collections.emptyMap());

		possiblyAddClearTextsToAppTokens(data.dataRecordGroup);
	}

	private void possiblyAddClearTextsToAppTokens(DataRecordGroup dataRecordGroup) {
		if (clearTextsExists()) {
			addClearTextsToAppTokens(dataRecordGroup);
		}
	}

	private boolean clearTextsExists() {
		return !efSystemSecretIdAndClearTextToken.isEmpty();
	}

	private void addClearTextsToAppTokens(DataRecordGroup dataRecordGroup) {
		List<DataGroup> appTokenGroups = getListOfAppTokenGroups(dataRecordGroup);
		for (DataGroup appTokenGroup : appTokenGroups) {
			possiblySetClearTextInAppTokenGroup(appTokenGroup);
		}
	}

	private List<DataGroup> getListOfAppTokenGroups(DataRecordGroup dataRecordGroup) {
		DataGroup appTokensGroup = dataRecordGroup.getFirstGroupWithNameInData("appTokens");
		return appTokensGroup.getChildrenOfTypeAndName(DataGroup.class, "appToken");
	}

	private void possiblySetClearTextInAppTokenGroup(DataGroup appTokenGroup) {
		String systemSecretId = getSystemSecretId(appTokenGroup);
		if (efSystemSecretIdAndClearTextToken.containsKey(systemSecretId)) {
			String clearText = efSystemSecretIdAndClearTextToken.get(systemSecretId);
			setClearTextInAppTokenGroup(appTokenGroup, clearText);
		}
	}

	private String getSystemSecretId(DataGroup appTokenGroup) {
		DataRecordLink link = appTokenGroup.getFirstChildOfTypeAndName(DataRecordLink.class,
				"appTokenLink");
		return link.getLinkedRecordId();
	}

	private void setClearTextInAppTokenGroup(DataGroup appTokenGroup, String clearText) {
		DataAtomic clearTextAtomic = DataProvider
				.createAtomicUsingNameInDataAndValue(APP_TOKEN_CLEAR_TEXT_NAME_IN_DATA, clearText);
		appTokenGroup.addChild(clearTextAtomic);
	}

}
