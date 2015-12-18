package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

import java.util.HashSet;
import java.util.Set;

public class SpiderDataGroupRecordLink extends SpiderDataGroup {

    private Set<Action> actions = new HashSet<>();

    private SpiderDataGroupRecordLink(String nameInData) {
        super(nameInData);
    }

    private SpiderDataGroupRecordLink(DataGroup dataRecordLink){
        super(dataRecordLink);
    }

    public static SpiderDataGroupRecordLink withNameInData(String nameInData) {
        return new SpiderDataGroupRecordLink(nameInData);
    }

    public static SpiderDataGroupRecordLink fromDataRecordLink(DataGroup dataRecordLink) {
        return new SpiderDataGroupRecordLink(dataRecordLink);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public Set<Action> getActions() {
        return actions;
    }
}
