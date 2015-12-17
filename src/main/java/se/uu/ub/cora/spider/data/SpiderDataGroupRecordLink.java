package se.uu.ub.cora.spider.data;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class SpiderDataGroupRecordLink extends SpiderDataGroup {

    private Set<Action> actions = new HashSet<>();;

    private SpiderDataGroupRecordLink(String nameInData) {
        super(nameInData);
    }

    public static SpiderDataGroupRecordLink withNameInData(String nameInData) {
        return new SpiderDataGroupRecordLink(nameInData);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public Set<Action> getActions() {
        return actions;
    }
}
