package com.dbsyncstudio.model.sync.vo;

import com.dbsyncstudio.model.sync.entity.SyncRunDO;
import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;
import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;

import java.util.ArrayList;
import java.util.List;

public class SyncRunDetailVO {

    private SyncRunDO run;
    private List<SyncTableRunDO> tableRuns = new ArrayList<SyncTableRunDO>();
    private List<SyncRunLogEntryDO> logs = new ArrayList<SyncRunLogEntryDO>();

    public SyncRunDO getRun() {
        return run;
    }

    public void setRun(SyncRunDO run) {
        this.run = run;
    }

    public List<SyncTableRunDO> getTableRuns() {
        return tableRuns;
    }

    public void setTableRuns(List<SyncTableRunDO> tableRuns) {
        this.tableRuns = tableRuns;
    }

    public List<SyncRunLogEntryDO> getLogs() {
        return logs;
    }

    public void setLogs(List<SyncRunLogEntryDO> logs) {
        this.logs = logs;
    }
}
