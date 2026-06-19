package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.model.sync.FullSyncRequest;
import com.dbsyncstudio.model.sync.FullSyncResult;

public interface FullSyncEngine {

    FullSyncResult sync(FullSyncRequest request);
}

