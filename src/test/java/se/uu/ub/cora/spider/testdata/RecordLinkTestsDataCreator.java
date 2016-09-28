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

package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.data.SpiderDataResourceLink;

public class RecordLinkTestsDataCreator {

	private static final String DATA_WITH_LINKS = "dataWithLinks";

	public static SpiderDataGroup createDataGroupWithLink() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(DATA_WITH_LINKS);
		SpiderDataRecordLink spiderRecordLink = SpiderDataRecordLink.withNameInData("link");

		SpiderDataAtomic linkedRecordType = SpiderDataAtomic
				.withNameInDataAndValue("linkedRecordType", "toRecordType");
		spiderRecordLink.addChild(linkedRecordType);

		SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				"toRecordId");
		spiderRecordLink.addChild(linkedRecordId);
		dataGroup.addChild(spiderRecordLink);
		return dataGroup;
	}

	public static SpiderDataGroup createDataGroupWithLinkOneLevelDown() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(DATA_WITH_LINKS);
		SpiderDataGroup oneLevelDown = SpiderDataGroup.withNameInData("oneLevelDown");
		dataGroup.addChild(oneLevelDown);

		SpiderDataRecordLink spiderRecordLink = SpiderDataRecordLink.withNameInData("link");

		SpiderDataAtomic linkedRecordType = SpiderDataAtomic
				.withNameInDataAndValue("linkedRecordType", "toRecordType");
		spiderRecordLink.addChild(linkedRecordType);

		SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				"toRecordId");
		spiderRecordLink.addChild(linkedRecordId);

		oneLevelDown.addChild(spiderRecordLink);
		return dataGroup;
	}

	public static SpiderDataGroup createSpiderDataGroupWithRecordInfoAndLink() {
		SpiderDataGroup dataGroup = createDataGroupWithLink();
		dataGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						DATA_WITH_LINKS, "oneLinkTopLevel", "cora"));
		return dataGroup;
	}

	public static SpiderDataGroup createDataGroupWithRecordInfoAndLinkOneLevelDown() {
		SpiderDataGroup dataGroup = createDataGroupWithLinkOneLevelDown();
		dataGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						DATA_WITH_LINKS, "oneLinkOneLevelDown", "cora"));
		return dataGroup;
	}

	public static SpiderDataGroup createSpiderDataGroupWithRecordInfoAndResourceLink() {
		SpiderDataGroup dataGroup = createDataGroupWithResourceLink();
		dataGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						DATA_WITH_LINKS, "oneResourceLinkTopLevel", "cora"));
		return dataGroup;
	}

	public static SpiderDataGroup createDataGroupWithRecordInfoAndResourceLinkOneLevelDown() {
		SpiderDataGroup dataGroup = createDataGroupWithResourceLinkOneLevelDown();
		dataGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						DATA_WITH_LINKS, "oneResourceLinkOneLevelDown", "cora"));
		return dataGroup;
	}

	public static SpiderDataGroup createDataGroupWithResourceLink() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(DATA_WITH_LINKS);
		dataGroup.addChild(createResourceLink());
		return dataGroup;
	}

	private static SpiderDataResourceLink createResourceLink() {
		SpiderDataResourceLink spiderResourceLink = SpiderDataResourceLink.withNameInData("link");

		spiderResourceLink
				.addChild(SpiderDataAtomic.withNameInDataAndValue("streamId", "someStreamId"));
		spiderResourceLink
				.addChild(SpiderDataAtomic.withNameInDataAndValue("filename", "aFileName"));
		spiderResourceLink.addChild(SpiderDataAtomic.withNameInDataAndValue("filesize", "12345"));
		spiderResourceLink
				.addChild(SpiderDataAtomic.withNameInDataAndValue("mimeType", "application/pdf"));
		return spiderResourceLink;
	}

	public static SpiderDataGroup createDataGroupWithResourceLinkOneLevelDown() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(DATA_WITH_LINKS);
		SpiderDataGroup oneLevelDown = SpiderDataGroup.withNameInData("oneLevelDown");
		dataGroup.addChild(oneLevelDown);

		oneLevelDown.addChild(createResourceLink());

		return dataGroup;
	}

}
