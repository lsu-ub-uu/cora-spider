package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

import java.util.HashSet;
import java.util.Set;

public class SpiderDataRecordLink extends SpiderDataGroup {

    private Set<Action> actions = new HashSet<>();

    private SpiderDataRecordLink(String nameInData) {
        super(nameInData);
    }

    private SpiderDataRecordLink(DataGroup dataRecordLink){
        super(dataRecordLink);
    }

    public static SpiderDataRecordLink withNameInData(String nameInData) {
        return new SpiderDataRecordLink(nameInData);
    }

    public static SpiderDataRecordLink fromDataRecordLink(DataGroup dataRecordLink) {
        return new SpiderDataRecordLink(dataRecordLink);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public Set<Action> getActions() {
        return actions;
    }
}
