package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderRecordList;

/**
 * TODO: Class description
 *
 * @author <a href="mailto:madeleine.kennback@ub.uu.se">Madeleine Kennbäck</a>
 * @version $Revision$, $Date$, $Author$
 */
public interface SpiderRecordListReader {
    SpiderRecordList readRecordList(String userId, String type);
}
