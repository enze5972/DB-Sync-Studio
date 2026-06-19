package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.sync.SyncRun;
import com.dbsyncstudio.model.sync.SyncRunLogEntry;
import com.dbsyncstudio.model.sync.SyncTableRun;

import java.util.ArrayList;
import java.util.List;

public class SyncRunDetailResponse {

    private SyncRun run;
    private List<SyncTableRun> tableRuns = new ArrayList<SyncTableRun>();
    private List<SyncRunLogEntry> logs = new ArrayList<SyncRunLogEntry>();

    public SyncRun getRun() {
        return run;
    }

    public void setRun(SyncRun run) {
        this.run = run;
    }

    public List<SyncTableRun> getTableRuns() {
        return tableRuns;
    }

    public void setTableRuns(List<SyncTableRun> tableRuns) {
        this.tableRuns = tableRuns;
    }

    public List<SyncRunLogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<SyncRunLogEntry> logs) {
        this.logs = logs;
    }
}
