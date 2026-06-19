package com.dbsyncstudio.model.schema;

import java.util.List;

public interface SchemaComparisonHistoryRepository {

    long save(SchemaComparisonHistoryEntry entry);

    List<SchemaComparisonHistoryEntry> findRecent(int limit);
}
