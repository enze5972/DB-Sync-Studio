package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.model.sync.IncrementalSyncRequest;
import com.dbsyncstudio.model.sync.IncrementalSyncResult;

public interface IncrementalSyncEngine {

    IncrementalSyncResult sync(IncrementalSyncRequest request);
}

