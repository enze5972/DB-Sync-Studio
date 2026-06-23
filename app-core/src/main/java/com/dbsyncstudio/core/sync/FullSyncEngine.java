package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.model.sync.dto.FullSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.FullSyncResultVO;

public interface FullSyncEngine {

    FullSyncResultVO sync(FullSyncRequestDTO request);
}

