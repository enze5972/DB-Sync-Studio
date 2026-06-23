package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.schema.entity.SchemaComparisonHistoryEntryDO;

import java.util.List;

public interface SchemaComparisonHistoryRepository {

    long save(SchemaComparisonHistoryEntryDO entry);

    List<SchemaComparisonHistoryEntryDO> findRecent(int limit);
}
