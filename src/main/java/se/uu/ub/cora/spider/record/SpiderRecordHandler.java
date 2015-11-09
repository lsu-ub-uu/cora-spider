package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;

public class SpiderRecordHandler {
	protected void addReadActionToDataRecordLinks(SpiderDataGroup spiderDataGroup) {
		for (SpiderDataElement spiderDataChild : spiderDataGroup.getChildren()) {
			addReadActionToDataRecordLink(spiderDataChild);
		}

	}

	private void addReadActionToDataRecordLink(SpiderDataElement spiderDataChild) {
		if (isLink(spiderDataChild)) {
			((SpiderDataRecordLink) spiderDataChild).addAction(Action.READ);
		}
		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink;
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}
}
