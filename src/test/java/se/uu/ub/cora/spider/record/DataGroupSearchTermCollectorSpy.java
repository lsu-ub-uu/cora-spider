package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.searchtermcollector.DataGroupSearchTermCollector;

public class DataGroupSearchTermCollectorSpy implements DataGroupSearchTermCollector {
    @Override
    public DataGroup collectSearchTerms(String s, DataGroup dataGroup) {
        return null;
    }
}
