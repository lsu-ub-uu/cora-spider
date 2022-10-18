/*
 * Copyright 2015, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testspies.DataResourceLinkSpy;

public class RecordLinkTestsDataCreator {

	private static final String DATA_WITH_LINKS = "dataWithLinks";

	public static DataGroup createDataDataGroupWithLink() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);

		DataLink recordLink = createDataLink();
		dataGroup.addChild(recordLink);
		return dataGroup;
	}

	private static DataRecordLink createDataLink() {
		return DataCreator2.createLinkWithLinkedId("link", "toRecordType", "toRecordId");
	}

	public static DataGroup createDataDataGroupWithLinkNotAuthorized() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);

		DataRecordLink recordLink = DataCreator2.createLinkWithLinkedId("link", "toRecordType",
				"recordLinkNotAuthorized");
		dataGroup.addChild(recordLink);
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithLinkOneLevelDown() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);
		DataGroup oneLevelDown = new DataGroupOldSpy("oneLevelDown");
		dataGroup.addChild(oneLevelDown);

		DataRecordLink recordLink = createDataLink();
		oneLevelDown.addChild(recordLink);
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithLinkOneLevelDownTargetDoesNotExist() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);
		DataGroup oneLevelDown = new DataGroupOldSpy("oneLevelDownTargetDoesNotExist");
		dataGroup.addChild(oneLevelDown);

		DataRecordLink recordLink = DataCreator2.createLinkWithLinkedId("link", "toRecordType",
				"nonExistingRecordId");
		oneLevelDown.addChild(recordLink);
		return dataGroup;
	}

	public static DataGroup createDataGroupWithRecordInfoAndLink() {
		DataGroup dataGroup = createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneLinkTopLevel", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataGroupWithRecordInfoAndTwoLinks() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "towLinksTopLevel", "cora"));

		DataRecordLinkSpy recordLink = (DataRecordLinkSpy) createDataLink();
		recordLink.setRepeatId("one");
		dataGroup.addChild(recordLink);

		DataRecordLinkSpy recordLink2 = (DataRecordLinkSpy) createDataLink();
		recordLink2.setRepeatId("two");
		dataGroup.addChild(recordLink2);
		return dataGroup;

	}

	public static DataGroup createDataGroupWithRecordInfoAndLinkNotAuthorized() {
		DataGroup dataGroup = createDataDataGroupWithLinkNotAuthorized();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneLinkTopLevelNotAuthorized", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataGroupWithRecordInfoAndMissingLink() {
		DataGroup dataGroup = createDataDataGroupWithLinkNotAuthorized();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"missingLink", "oneLinkTopLevelMissingLink", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithRecordInfoAndLinkOneLevelDown() {
		DataGroup dataGroup = createDataDataGroupWithLinkOneLevelDown();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneLinkOneLevelDown", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithRecordInfoAndLinkOneLevelDownTargetDoesNotExist() {
		DataGroup dataGroup = createDataDataGroupWithLinkOneLevelDownTargetDoesNotExist();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneLinkOneLevelDownTargetDoesNotExist", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataGroupWithRecordInfoAndResourceLink() {
		DataGroup dataGroup = createDataDataGroupWithResourceLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneResourceLinkTopLevel", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithRecordInfoAndResourceLinkOneLevelDown() {
		DataGroup dataGroup = createDataDataGroupWithResourceLinkOneLevelDown();
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				DATA_WITH_LINKS, "oneResourceLinkOneLevelDown", "cora"));
		return dataGroup;
	}

	public static DataGroup createDataDataGroupWithResourceLink() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);
		dataGroup.addChild(createDataResourceLink());
		return dataGroup;
	}

	private static DataLink createDataResourceLink() {
		DataResourceLinkSpy resourceLink = new DataResourceLinkSpy("link");

		resourceLink.addChild(new DataAtomicSpy("streamId", "someStreamId"));
		resourceLink.addChild(new DataAtomicSpy("filename", "aFileName"));
		resourceLink.addChild(new DataAtomicSpy("filesize", "12345"));
		resourceLink.addChild(new DataAtomicSpy("mimeType", "application/pdf"));
		return resourceLink;
	}

	public static DataGroup createDataDataGroupWithResourceLinkOneLevelDown() {
		DataGroup dataGroup = new DataGroupOldSpy(DATA_WITH_LINKS);
		DataGroup oneLevelDown = new DataGroupOldSpy("oneLevelDown");
		dataGroup.addChild(oneLevelDown);

		oneLevelDown.addChild(createDataResourceLink());

		return dataGroup;
	}

	public static DataGroup createLinkChildAsDataRecordDataGroup() {
		DataGroup dataGroup = new DataGroupOldSpy("toRecordType");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "recordLinkNotAuthorized"));

		DataGroup type = new DataGroupOldSpy("type");
		type.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		type.addChild(new DataAtomicSpy("linkedRecordId", "toRecordType"));
		recordInfo.addChild(type);

		dataGroup.addChild(recordInfo);
		return dataGroup;
	}
}
