package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.model.sync.dto.IncrementalSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.IncrementalSyncResultVO;

public interface IncrementalSyncEngine {

    IncrementalSyncResultVO sync(IncrementalSyncRequestDTO request);
}

