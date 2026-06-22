<template>
  <div class="page-section schema-diff-workbench">
    <div class="page-header schema-diff-workbench__header">
      <div>
        <h1>表结构比较器</h1>
        <p>选择源表和目标表，查看字段、类型、长度、主键和索引差异，并生成修复 SQL。</p>
      </div>
      <el-space wrap>
        <el-button round @click="loadDatasources">刷新数据源</el-button>
        <el-tooltip :disabled="canCompare" :content="compareDisabledReason" placement="bottom">
          <span>
            <el-button type="primary" round :loading="comparing" :disabled="!canCompare" @click="runCompare">
              开始比较
            </el-button>
          </span>
        </el-tooltip>
      </el-space>
    </div>

    <div class="schema-diff-workbench__overview page-overview page-overview--four">
      <div class="page-overview__item">
        <div class="page-overview__label">比较状态</div>
        <div class="page-overview__value">{{ compareStatusLabel }}</div>
        <div class="page-overview__hint">{{ compareStatusHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">差异项</div>
        <div class="page-overview__value">{{ diffCount }}</div>
        <div class="page-overview__hint">{{ diffHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">SQL 预览</div>
        <div class="page-overview__value">{{ suggestedSqlList.length }} 条</div>
        <div class="page-overview__hint">{{ sqlPreviewHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">风险提示</div>
        <div class="page-overview__value">{{ riskBadgeLabel }}</div>
        <div class="page-overview__hint">{{ riskBadgeHint }}</div>
      </div>
    </div>

    <div class="schema-diff-workbench__context-grid">
      <div class="panel-card glass-panel schema-context-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>源表上下文</h2>
            <el-tag :type="sourceReady ? 'success' : 'info'" effect="light">
              {{ sourceReady ? '已选择' : '未配置' }}
            </el-tag>
          </div>
        </div>
        <div class="schema-context-card__body">
          <div class="schema-context-row">
            <span class="schema-context-row__label">数据源</span>
            <span class="schema-context-row__value">{{ sourceDatasourceName || '—' }}</span>
          </div>
          <div class="schema-context-row">
            <span class="schema-context-row__label">数据库类型</span>
            <span class="schema-context-row__value">{{ sourceDatasourceType || '—' }}</span>
          </div>
          <div class="schema-context-row">
            <span class="schema-context-row__label">Schema / Table</span>
            <span class="schema-context-row__value">{{ sourceTablePath }}</span>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel schema-context-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>目标表上下文</h2>
            <el-tag :type="targetReady ? 'success' : 'info'" effect="light">
              {{ targetReady ? '已选择' : '未配置' }}
            </el-tag>
          </div>
        </div>
        <div class="schema-context-card__body">
          <div class="schema-context-row">
            <span class="schema-context-row__label">数据源</span>
            <span class="schema-context-row__value">{{ targetDatasourceName || '—' }}</span>
          </div>
          <div class="schema-context-row">
            <span class="schema-context-row__label">数据库类型</span>
            <span class="schema-context-row__value">{{ targetDatasourceType || '—' }}</span>
          </div>
          <div class="schema-context-row">
            <span class="schema-context-row__label">Schema / Table</span>
            <span class="schema-context-row__value">{{ targetTablePath }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact schema-diff-workbench__workspace">
      <div class="panel-card glass-panel schema-config-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>表结构比较工作台</h2>
            <el-tag :type="compareStatusTagType" effect="light">{{ compareStatusChip }}</el-tag>
          </div>
        </div>

        <div class="schema-config-card__body">
          <div class="schema-config-block">
            <div class="schema-config-block__title">源表</div>
            <el-form label-width="100px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="源数据源">
                    <el-select v-model="form.sourceDatasourceId" style="width: 100%;" @change="handleSourceDatasourceChange">
                      <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="源 schema">
                    <CreatableSelect v-model="form.sourceSchemaName" :options="sourceSchemaOptions" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="源表名">
                    <CreatableSelect v-model="form.sourceTableName" :options="sourceTableOptions" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="schema-config-block">
            <div class="schema-config-block__title">目标表</div>
            <el-form label-width="100px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="目标数据源">
                    <el-select v-model="form.targetDatasourceId" style="width: 100%;" @change="handleTargetDatasourceChange">
                      <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="目标 schema">
                    <CreatableSelect v-model="form.targetSchemaName" :options="targetSchemaOptions" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="目标表名">
                    <CreatableSelect v-model="form.targetTableName" :options="targetTableOptions" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="schema-config-footer">
            <div class="schema-config-footer__hint">
              {{ compareDisabledReason }}
            </div>
            <el-space wrap>
              <el-tooltip :disabled="hasSqlPreview" :content="sqlPreviewDisabledReason" placement="top">
                <span>
                  <el-button :disabled="!hasSqlPreview" @click="copySql">复制 SQL</el-button>
                </span>
              </el-tooltip>
              <el-tooltip :disabled="hasSqlPreview" :content="sqlPreviewDisabledReason" placement="top">
                <span>
                  <el-button :disabled="!hasSqlPreview" :loading="executing" type="warning" @click="executeSql">
                    确认执行
                  </el-button>
                </span>
              </el-tooltip>
              <el-tooltip :disabled="canCompare" :content="compareDisabledReason" placement="top">
                <span>
                  <el-button type="primary" :loading="comparing" :disabled="!canCompare" @click="runCompare">
                    开始比较
                  </el-button>
                </span>
              </el-tooltip>
            </el-space>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel schema-result-card">
        <div class="section-title schema-result-card__title">
          <div class="section-title__left">
            <h2>差异结果</h2>
            <el-tag :type="resultTagType" effect="light">{{ resultTagLabel }}</el-tag>
          </div>
        </div>

        <div class="schema-result-card__body">
          <div class="schema-result-summary" :class="comparisonSummaryClass">
            <template v-if="comparisonMode === 'waiting'">
              <div class="schema-result-summary__header">
                <div class="schema-result-summary__eyebrow">比较摘要 / 操作引导</div>
                <div class="schema-result-summary__title">请选择源表和目标表后开始比较</div>
                <div class="schema-result-summary__hint">
                  当前仅比较表结构，不比较数据内容。
                </div>
              </div>
              <div class="schema-summary-chip-grid">
                <div class="schema-summary-chip" :class="{ 'is-muted': !sourceReady }">
                  <el-icon class="schema-summary-chip__icon">
                    <Document />
                  </el-icon>
                  <div class="schema-summary-chip__content">
                    <div class="schema-summary-chip__label">源表</div>
                    <div class="schema-summary-chip__value">{{ sourceTableChipLabel }}</div>
                  </div>
                </div>
                <div class="schema-summary-chip" :class="{ 'is-muted': !targetReady }">
                  <el-icon class="schema-summary-chip__icon">
                    <Document />
                  </el-icon>
                  <div class="schema-summary-chip__content">
                    <div class="schema-summary-chip__label">目标表</div>
                    <div class="schema-summary-chip__value">{{ targetTableChipLabel }}</div>
                  </div>
                </div>
                <div class="schema-summary-chip">
                  <el-icon class="schema-summary-chip__icon">
                    <Operation />
                  </el-icon>
                  <div class="schema-summary-chip__content">
                    <div class="schema-summary-chip__label">比较模式</div>
                    <div class="schema-summary-chip__value">表结构比较</div>
                  </div>
                </div>
                <div class="schema-summary-chip schema-summary-chip--strategy">
                  <el-icon class="schema-summary-chip__icon">
                    <InfoFilled />
                  </el-icon>
                  <div class="schema-summary-chip__content">
                    <div class="schema-summary-chip__label">SQL 策略</div>
                    <div class="schema-summary-chip__value">仅生成 SQL，执行前必须确认</div>
                  </div>
                </div>
              </div>
              <div class="schema-result-summary__guide">
                请选择源表和目标表后开始比较。
              </div>
            </template>

            <template v-else-if="comparisonMode === 'comparing'">
              <div class="schema-result-summary__header">
                <div class="schema-result-summary__eyebrow">比较中</div>
                <div class="schema-result-summary__title">正在比较表结构</div>
                <div class="schema-result-summary__hint">
                  当前步骤：{{ currentComparisonStepLabel }}
                </div>
              </div>
              <div class="schema-result-summary__loading">
                <el-icon class="schema-result-summary__loading-icon is-spinning">
                  <Loading />
                </el-icon>
                <div class="schema-result-summary__loading-copy">
                  <div class="schema-result-summary__loading-title">正在读取和分析结构信息</div>
                  <div class="schema-result-summary__loading-hint">比较中不会改动目标库。</div>
                </div>
              </div>
              <div class="schema-result-summary__progress">
                <el-progress :percentage="comparisonProgress" :stroke-width="8" :show-text="false" />
                <div class="schema-result-summary__progress-hint">
                  {{ currentComparisonStepLabel }} · {{ comparisonProgress }}%
                </div>
              </div>
              <div class="schema-summary-step-grid">
                <div
                  v-for="step in comparisonSteps"
                  :key="step.label"
                  class="schema-summary-step"
                  :class="step.stateClass"
                >
                  <el-icon class="schema-summary-step__icon">
                    <Finished v-if="step.state === 'done'" />
                    <Loading v-else-if="step.state === 'active'" />
                    <Select v-else />
                  </el-icon>
                  <div class="schema-summary-step__label">{{ step.label }}</div>
                </div>
              </div>
            </template>

            <template v-else-if="comparisonMode === 'success'">
              <div class="schema-result-summary__header is-success">
                <div class="schema-result-summary__eyebrow">比较完成</div>
                <div class="schema-result-summary__title">结构一致，未发现字段差异</div>
                <div class="schema-result-summary__hint">SQL 风险为 0，当前结构可以直接保留。</div>
              </div>
              <div class="schema-summary-metric-grid schema-summary-metric-grid--success">
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">源表字段数</div>
                  <div class="schema-summary-metric-card__value">{{ sourceColumnCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">目标表字段数</div>
                  <div class="schema-summary-metric-card__value">{{ targetColumnCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">比较时间</div>
                  <div class="schema-summary-metric-card__value">{{ compareFinishedText }}</div>
                </div>
                <div class="schema-summary-metric-card is-risk">
                  <div class="schema-summary-metric-card__label">SQL 风险</div>
                  <div class="schema-summary-metric-card__value">0</div>
                </div>
              </div>
              <div class="schema-result-summary__actions">
                <el-space wrap>
                  <el-button round @click="runCompare">重新比较</el-button>
                  <el-tooltip :disabled="hasSqlPreview" :content="sqlPreviewDisabledReason" placement="top">
                    <span>
                      <el-button round :disabled="!hasSqlPreview" @click="generateSqlPreview">生成 SQL</el-button>
                    </span>
                  </el-tooltip>
                  <el-button round @click="exportComparisonResult">导出结果</el-button>
                </el-space>
              </div>
            </template>

            <template v-else-if="comparisonMode === 'error'">
              <div class="schema-result-summary__header is-error">
                <div class="schema-result-summary__eyebrow">比较失败</div>
                <div class="schema-result-summary__title">{{ errorMessage || '比较过程中发生错误' }}</div>
                <div class="schema-result-summary__hint">{{ errorHint }}</div>
              </div>
              <div class="schema-result-summary__actions schema-result-summary__actions--start">
                <el-space wrap>
                  <el-button type="primary" round :loading="comparing" @click="runCompare">重新比较</el-button>
                  <el-button round @click="activeResultTab = 'fields'">查看差异</el-button>
                </el-space>
              </div>
            </template>

            <template v-else>
              <div class="schema-result-summary__header is-warning">
                <div class="schema-result-summary__eyebrow">比较完成</div>
                <div class="schema-result-summary__title">发现 {{ diffCount }} 项结构差异</div>
                <div class="schema-result-summary__hint">仅生成 SQL，执行前必须确认。</div>
              </div>
              <div class="schema-summary-metric-grid">
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">总差异</div>
                  <div class="schema-summary-metric-card__value">{{ diffCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">缺失字段</div>
                  <div class="schema-summary-metric-card__value">{{ missingColumnDiffCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">类型不一致</div>
                  <div class="schema-summary-metric-card__value">{{ typeDiffCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">长度/精度差异</div>
                  <div class="schema-summary-metric-card__value">{{ lengthDiffCount }}</div>
                </div>
                <div class="schema-summary-metric-card">
                  <div class="schema-summary-metric-card__label">默认值差异</div>
                  <div class="schema-summary-metric-card__value">{{ defaultDiffCount }}</div>
                </div>
                <div class="schema-summary-metric-card is-risk">
                  <div class="schema-summary-metric-card__label">SQL 风险</div>
                  <div class="schema-summary-metric-card__value">{{ sqlRiskCount }}</div>
                </div>
              </div>
              <div class="schema-result-summary__actions">
                <el-space wrap>
                  <el-button round @click="runCompare">重新比较</el-button>
                  <el-tooltip :disabled="hasSqlPreview" :content="sqlPreviewDisabledReason" placement="top">
                    <span>
                      <el-button round :disabled="!hasSqlPreview" @click="generateSqlPreview">生成 SQL</el-button>
                    </span>
                  </el-tooltip>
                  <el-button round @click="exportComparisonResult">导出结果</el-button>
                </el-space>
              </div>
            </template>
          </div>

          <div class="schema-result-tabs">
            <el-tabs v-model="activeResultTab" class="sql-result-tabs">
              <el-tab-pane label="字段差异" name="fields">
                <div class="schema-result-pane">
                  <div v-if="comparisonMode === 'waiting'" class="schema-tab-empty">
                    <el-icon class="schema-tab-empty__icon">
                      <InfoFilled />
                    </el-icon>
                    <div class="schema-tab-empty__title">暂无字段差异</div>
                    <div class="schema-tab-empty__body">
                      选择源表和目标表并开始比较后，这里会展示字段级差异。
                    </div>
                  </div>
                  <div v-else-if="comparisonMode === 'comparing'" class="schema-tab-empty schema-tab-empty--loading">
                    <el-icon class="schema-tab-empty__icon is-spinning">
                      <Loading />
                    </el-icon>
                    <div class="schema-tab-empty__title">正在比较中</div>
                    <div class="schema-tab-empty__body">
                      读取结构、分析差异和生成 SQL 的结果会在这里更新。
                    </div>
                  </div>
                  <div v-else-if="comparisonMode === 'error'" class="schema-tab-empty schema-tab-empty--warning">
                    <el-icon class="schema-tab-empty__icon">
                      <WarningFilled />
                    </el-icon>
                    <div class="schema-tab-empty__title">比较失败</div>
                    <div class="schema-tab-empty__body">
                      请切换到“执行历史”或查看上方错误提示。
                    </div>
                  </div>
                  <div v-else-if="comparisonMode === 'success'" class="schema-tab-empty schema-tab-empty--success">
                    <el-icon class="schema-tab-empty__icon">
                      <CircleCheck />
                    </el-icon>
                    <div class="schema-tab-empty__title">结构一致，未发现字段差异</div>
                    <div class="schema-tab-empty__body">
                      源表字段数 {{ sourceColumnCount }}，目标表字段数 {{ targetColumnCount }}。
                    </div>
                  </div>
                  <template v-else>
                    <div class="schema-diff-tags" v-if="fieldDiffEntries.length">
                      <el-tag type="warning" effect="light">字段差异 {{ fieldDiffEntries.length }}</el-tag>
                    </div>
                    <div class="table-shell schema-diff-table-shell">
                      <el-table :data="fieldDiffEntries" border stripe v-loading="comparing">
                        <el-table-column prop="diffType" label="类型" width="150">
                          <template #default="{ row }">
                            <el-tag :type="diffTagType(row.diffType)" effect="light">
                              {{ diffTypeLabel(row.diffType) }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="description" label="描述" min-width="260" />
                        <el-table-column prop="sourceColumnName" label="源字段" min-width="150" />
                        <el-table-column prop="targetColumnName" label="目标字段" min-width="150" />
                        <el-table-column label="SQL" min-width="360" show-overflow-tooltip>
                          <template #default="{ row }">
                            {{ row.suggestedSql || '—' }}
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </template>
                </div>
              </el-tab-pane>

              <el-tab-pane label="索引差异" name="indexes">
                <div class="schema-result-pane">
                  <div v-if="!indexDiffEntries.length" class="schema-tab-empty">
                    <el-icon class="schema-tab-empty__icon">
                      <InfoFilled />
                    </el-icon>
                    <div class="schema-tab-empty__title">暂无索引差异</div>
                    <div class="schema-tab-empty__body">比较完成后，如存在索引新增、缺失或变更，会显示在这里。</div>
                  </div>
                  <div v-else class="table-shell schema-diff-table-shell">
                    <el-table :data="indexDiffEntries" border stripe>
                      <el-table-column prop="diffType" label="类型" width="150">
                        <template #default="{ row }">
                          <el-tag :type="diffTagType(row.diffType)" effect="light">
                            {{ diffTypeLabel(row.diffType) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="描述" min-width="260" />
                      <el-table-column prop="sourceColumnName" label="源索引" min-width="180" show-overflow-tooltip />
                      <el-table-column prop="targetColumnName" label="目标索引" min-width="180" show-overflow-tooltip />
                    </el-table>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="主键差异" name="primaryKeys">
                <div class="schema-result-pane">
                  <div v-if="!primaryKeyDiffEntries.length" class="schema-tab-empty">
                    <el-icon class="schema-tab-empty__icon">
                      <Finished />
                    </el-icon>
                    <div class="schema-tab-empty__title">暂无主键差异</div>
                    <div class="schema-tab-empty__body">比较完成后，如存在主键新增、缺失或不一致，会显示在这里。</div>
                  </div>
                  <div v-else class="table-shell schema-diff-table-shell">
                    <el-table :data="primaryKeyDiffEntries" border stripe>
                      <el-table-column prop="diffType" label="类型" width="150">
                        <template #default="{ row }">
                          <el-tag :type="diffTagType(row.diffType)" effect="light">
                            {{ diffTypeLabel(row.diffType) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="描述" min-width="260" />
                      <el-table-column prop="sourceColumnName" label="源主键" min-width="180" show-overflow-tooltip />
                      <el-table-column prop="targetColumnName" label="目标主键" min-width="180" show-overflow-tooltip />
                    </el-table>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="SQL 预览" name="sqlPreview">
                <div class="schema-result-pane">
                  <div v-if="!hasSqlPreview" class="schema-tab-empty">
                    <el-icon class="schema-tab-empty__icon">
                      <Document />
                    </el-icon>
                    <div class="schema-tab-empty__title">暂无 SQL 预览</div>
                    <div class="schema-tab-empty__body">完成比较并生成 SQL 后，这里会显示修复语句。</div>
                  </div>
                  <div v-else class="schema-sql-preview">
                    <div class="schema-sql-preview__meta">
                      <span>共 {{ suggestedSqlList.length }} 条 SQL</span>
                      <el-space>
                        <el-button size="small" @click="copySql">复制 SQL</el-button>
                        <el-button size="small" type="warning" @click="executeSql">确认执行</el-button>
                      </el-space>
                    </div>
                    <el-input
                      :model-value="suggestedSqlList.join(';\n')"
                      type="textarea"
                      :rows="14"
                      readonly
                      class="schema-sql-preview__textarea"
                    />
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="执行历史" name="history">
                <div class="schema-result-pane">
                  <div class="schema-history-panel">
                    <div class="schema-history-panel__toolbar">
                      <div class="schema-history-panel__hint">
                        最近 20 条比较记录
                      </div>
                      <div class="schema-history-panel__count">
                        {{ historyEntries.length }} 条
                      </div>
                    </div>
                    <div v-if="!historyLoading && !historyEntries.length" class="schema-tab-empty schema-tab-empty--history">
                      <div class="schema-tab-empty__title">暂无比较历史</div>
                      <div class="schema-tab-empty__body">最近 20 条比较记录会显示在这里。</div>
                    </div>
                    <div v-else class="table-shell schema-history-table-shell">
                      <el-table :data="historyEntries" border stripe v-loading="historyLoading">
                        <el-table-column label="时间" width="200">
                          <template #default="{ row }">
                            {{ formatTime(row.createdAt) }}
                          </template>
                        </el-table-column>
                        <el-table-column label="源表" min-width="220">
                          <template #default="{ row }">
                            {{ formatQualifiedTable(row.sourceSchemaName, row.sourceTableName) }}
                          </template>
                        </el-table-column>
                        <el-table-column label="目标表" min-width="220">
                          <template #default="{ row }">
                            {{ formatQualifiedTable(row.targetSchemaName, row.targetTableName) }}
                          </template>
                        </el-table-column>
                        <el-table-column label="差异摘要" min-width="180" show-overflow-tooltip>
                          <template #default="{ row }">
                            {{ formatHistorySummary(row.diffSummary) }}
                          </template>
                        </el-table-column>
                        <el-table-column label="SQL 条数" width="110">
                          <template #default="{ row }">
                            {{ row.sqlCount != null ? row.sqlCount : '—' }}
                          </template>
                        </el-table-column>
                        <el-table-column label="状态" width="120">
                          <template #default>
                            <el-tag type="success" effect="light">已记录</el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, Document, Finished, InfoFilled, Loading, Operation, Select, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  compareSchema,
  executeSchemaSql,
  listDatasources,
  listSchemaComparisonHistory,
  scanMetadata,
  previewSchemaSql
} from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

const datasources = ref([])
const comparing = ref(false)
const executing = ref(false)
const historyLoading = ref(false)
const previewMessage = ref('')
const historyEntries = ref([])
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const activeResultTab = ref('fields')
const errorMessage = ref('')
const lastComparisonAt = ref(null)
const lastComparedSignature = ref('')
const result = reactive({
  diffEntries: [],
  suggestedSqlList: [],
  sourceTable: null,
  targetTable: null
})
const form = reactive({
  sourceDatasourceId: null,
  targetDatasourceId: null,
  sourceSchemaName: '',
  targetSchemaName: '',
  sourceTableName: '',
  targetTableName: ''
})

const selectedSourceDatasource = computed(function () {
  return datasources.value.find(function (item) {
    return item.id === form.sourceDatasourceId
  }) || null
})

const selectedTargetDatasource = computed(function () {
  return datasources.value.find(function (item) {
    return item.id === form.targetDatasourceId
  }) || null
})

const sourceReady = computed(function () {
  return !!form.sourceDatasourceId && !!form.sourceTableName
})

const targetReady = computed(function () {
  return !!form.targetDatasourceId && !!form.targetTableName
})

const canCompare = computed(function () {
  return sourceReady.value && targetReady.value && !comparing.value
})

const compareDisabledReason = computed(function () {
  if (comparing.value) {
    return '正在比较中'
  }
  if (!form.sourceDatasourceId) {
    return '请先选择源数据源'
  }
  if (!form.sourceTableName) {
    return '请先选择源 schema / table'
  }
  if (!form.targetDatasourceId) {
    return '请先选择目标数据源'
  }
  if (!form.targetTableName) {
    return '请先选择目标 schema / table'
  }
  return '源表与目标表已就绪，可以开始比较'
})

const sqlPreviewDisabledReason = computed(function () {
  return '请先完成比较并生成 SQL'
})

const comparisonSignature = computed(function () {
  return [
    form.sourceDatasourceId || '',
    form.sourceSchemaName || '',
    form.sourceTableName || '',
    form.targetDatasourceId || '',
    form.targetSchemaName || '',
    form.targetTableName || ''
  ].join('|')
})

const comparisonMode = computed(function () {
  if (comparing.value) {
    return 'comparing'
  }
  if (errorMessage.value) {
    return 'error'
  }
  if (!lastComparisonAt.value || lastComparedSignature.value !== comparisonSignature.value) {
    return 'waiting'
  }
  if (diffCount.value === 0) {
    return 'success'
  }
  return 'diff'
})

const sourceTableChipLabel = computed(function () {
  if (!form.sourceDatasourceId || !form.sourceTableName) {
    return '未选择'
  }
  return sourceTablePath.value
})

const targetTableChipLabel = computed(function () {
  if (!form.targetDatasourceId || !form.targetTableName) {
    return '未选择'
  }
  return targetTablePath.value
})

const sourceColumnCount = computed(function () {
  return countTableColumns(result.sourceTable)
})

const targetColumnCount = computed(function () {
  return countTableColumns(result.targetTable)
})

const comparisonProgress = computed(function () {
  return comparing.value ? 72 : 100
})

const comparisonSteps = computed(function () {
  const activeStep = currentComparisonStepLabel.value
  return [
    { label: '读取源表结构', state: stepStateForLabel('读取源表结构', activeStep) },
    { label: '读取目标表结构', state: stepStateForLabel('读取目标表结构', activeStep) },
    { label: '分析差异', state: stepStateForLabel('分析差异', activeStep) },
    { label: '生成 SQL', state: stepStateForLabel('生成 SQL', activeStep) }
  ].map(function (step) {
    return {
      label: step.label,
      state: step.state,
      stateClass: 'is-' + step.state
    }
  })
})

const currentComparisonStepLabel = computed(function () {
  if (!comparing.value) {
    return '等待开始'
  }
  if (!result.sourceTable && !result.targetTable) {
    return '读取源表结构'
  }
  if (!result.targetTable) {
    return '读取目标表结构'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return '生成 SQL'
  }
  return '分析差异'
})

const missingColumnDiffCount = computed(function () {
  return diffTypeCountsByKey.value.MISSING_COLUMN || 0
})

const typeDiffCount = computed(function () {
  return diffTypeCountsByKey.value.TYPE_DIFF || 0
})

const lengthDiffCount = computed(function () {
  return diffTypeCountsByKey.value.LENGTH_DIFF || 0
})

const defaultDiffCount = computed(function () {
  return diffTypeCountsByKey.value.DEFAULT_DIFF || 0
})

const sqlRiskCount = computed(function () {
  return diffCount.value > 0 ? 1 : 0
})

const compareFinishedText = computed(function () {
  return lastComparisonAt.value ? formatTime(lastComparisonAt.value) : '—'
})

const comparisonSummaryClass = computed(function () {
  return {
    'is-waiting': comparisonMode.value === 'waiting',
    'is-comparing': comparisonMode.value === 'comparing',
    'is-success': comparisonMode.value === 'success',
    'is-warning': comparisonMode.value === 'diff',
    'is-error': comparisonMode.value === 'error'
  }
})

const diffCount = computed(function () {
  return result.diffEntries.length
})

const hasSqlPreview = computed(function () {
  return result.suggestedSqlList.length > 0
})

const sourceDatasourceName = computed(function () {
  return selectedSourceDatasource.value ? selectedSourceDatasource.value.name : ''
})

const targetDatasourceName = computed(function () {
  return selectedTargetDatasource.value ? selectedTargetDatasource.value.name : ''
})

const sourceDatasourceType = computed(function () {
  return selectedSourceDatasource.value ? (selectedSourceDatasource.value.type || selectedSourceDatasource.value.dbType || '—') : ''
})

const targetDatasourceType = computed(function () {
  return selectedTargetDatasource.value ? (selectedTargetDatasource.value.type || selectedTargetDatasource.value.dbType || '—') : ''
})

const sourceTablePath = computed(function () {
  return formatQualifiedTable(form.sourceSchemaName, form.sourceTableName)
})

const targetTablePath = computed(function () {
  return formatQualifiedTable(form.targetSchemaName, form.targetTableName)
})

const compareStatusLabel = computed(function () {
  if (comparing.value) {
    return '比较中'
  }
  if (errorMessage.value) {
    return '比较失败'
  }
  if (!sourceReady.value || !targetReady.value) {
    return '未配置'
  }
  if (!lastComparisonAt.value || lastComparedSignature.value !== comparisonSignature.value) {
    return '等待比较'
  }
  if (diffCount.value === 0) {
    return '无差异'
  }
  return '比较完成'
})

const compareStatusHint = computed(function () {
  if (comparing.value) {
    return '正在扫描两侧表结构并生成修复建议'
  }
  if (errorMessage.value) {
    return '请查看错误 Tab 或修正表配置'
  }
  if (!sourceReady.value || !targetReady.value) {
    return '先完成源表和目标表配置'
  }
  if (!lastComparisonAt.value || lastComparedSignature.value !== comparisonSignature.value) {
    return '点击开始比较后查看差异与 SQL 预览'
  }
  if (diffCount.value === 0) {
    return '当前源表与目标表没有明显结构差异'
  }
  return '已生成差异分类、修复 SQL 和比较历史'
})

const diffHint = computed(function () {
  if (diffCount.value === 0) {
    return '结构一致或尚未比较'
  }
  return '字段、索引和主键差异已展开'
})

const sqlPreviewHint = computed(function () {
  if (!hasSqlPreview.value) {
    return '未生成 SQL 时这里保持简洁'
  }
  return '仅展示建议，执行前必须确认风险'
})

const riskBadgeLabel = computed(function () {
  if (!hasSqlPreview.value) {
    return '待确认'
  }
  return 'DDL 风险'
})

const riskBadgeHint = computed(function () {
  if (!hasSqlPreview.value) {
    return '比较后可查看 SQL 风险'
  }
  return '执行前建议备份目标库'
})

const compareStatusChip = computed(function () {
  return compareStatusLabel.value
})

const compareStatusTagType = computed(function () {
  if (comparing.value) {
    return 'warning'
  }
  if (errorMessage.value) {
    return 'danger'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return 'success'
  }
  if (diffCount.value > 0) {
    return 'warning'
  }
  return 'info'
})

const resultTagType = computed(function () {
  if (errorMessage.value) {
    return 'danger'
  }
  if (comparing.value) {
    return 'warning'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return 'success'
  }
  if (diffCount.value > 0) {
    return 'warning'
  }
  return 'info'
})

const resultTagLabel = computed(function () {
  if (errorMessage.value) {
    return '比较失败'
  }
  if (comparing.value) {
    return '比较中'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return '无差异'
  }
  if (diffCount.value > 0) {
    return '有差异'
  }
  return '等待比较'
})

const resultSummaryTitle = computed(function () {
  if (errorMessage.value) {
    return '比较失败'
  }
  if (comparing.value) {
    return '正在比较表结构'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return '无差异'
  }
  if (diffCount.value > 0) {
    return '有差异'
  }
  return '还没有开始比较'
})

const resultSummaryMeta = computed(function () {
  if (errorMessage.value) {
    return errorMessage.value
  }
  if (comparing.value) {
    return '正在分析字段、索引、主键和默认值差异'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return '源表与目标表结构一致，当前没有可生成的修复 SQL'
  }
  if (diffCount.value > 0) {
    return '检测到 ' + diffCount.value + ' 项差异，已生成 ' + result.suggestedSqlList.length + ' 条 SQL'
  }
  return '请选择源表和目标表后开始比较'
})

const resultSummaryHint = computed(function () {
  if (errorMessage.value) {
    return errorHint.value
  }
  if (comparing.value) {
    return '请稍候，比较中不会改动目标库'
  }
  if (diffCount.value === 0 && hasSqlPreview.value) {
    return '执行前仍建议复核源表和目标表是否选择正确'
  }
  if (diffCount.value > 0) {
    return '仅生成 SQL，执行前必须确认风险'
  }
  return '比较后这里会展示差异摘要与 SQL 风险提示'
})

const resultSummaryClass = computed(function () {
  return {
    'is-success': diffCount.value === 0 && hasSqlPreview.value && !errorMessage.value,
    'is-warning': diffCount.value > 0,
    'is-error': !!errorMessage.value
  }
})

const diffTypeCounts = computed(function () {
  const counts = {}
  const order = [
    'MISSING_COLUMN',
    'EXTRA_COLUMN',
    'TYPE_DIFF',
    'LENGTH_DIFF',
    'NULLABLE_DIFF',
    'DEFAULT_DIFF',
    'PRIMARY_KEY_DIFF',
    'INDEX_DIFF'
  ]
  result.diffEntries.forEach(function (entry) {
    const key = entry && entry.diffType ? entry.diffType : 'UNKNOWN'
    counts[key] = (counts[key] || 0) + 1
  })
  return order
    .filter(function (key) {
      return counts[key]
    })
    .map(function (key) {
      return {
        label: diffTypeLabel(key),
        count: counts[key],
        tagType: diffTagType(key)
      }
    })
})

const fieldDiffEntries = computed(function () {
  return result.diffEntries.filter(function (entry) {
    return entry && entry.diffType !== 'INDEX_DIFF' && entry.diffType !== 'PRIMARY_KEY_DIFF'
  })
})

const indexDiffEntries = computed(function () {
  return result.diffEntries.filter(function (entry) {
    return entry && entry.diffType === 'INDEX_DIFF'
  })
})

const primaryKeyDiffEntries = computed(function () {
  return result.diffEntries.filter(function (entry) {
    return entry && entry.diffType === 'PRIMARY_KEY_DIFF'
  })
})

const diffTypeCountsByKey = computed(function () {
  return result.diffEntries.reduce(function (acc, entry) {
    const key = entry && entry.diffType ? entry.diffType : 'UNKNOWN'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
})

const suggestedSqlList = computed(function () {
  return result.suggestedSqlList
})

const errorHint = computed(function () {
  if (!errorMessage.value) {
    return '暂无错误信息'
  }
  return '可能原因：数据源连接失败、schema/table 不存在、权限不足、比较引擎异常。'
})

const dialectLabel = computed(function () {
  const type = (selectedTargetDatasource.value && (selectedTargetDatasource.value.type || selectedTargetDatasource.value.dbType)) || ''
  const normalized = String(type).toUpperCase()
  if (normalized.indexOf('POST') >= 0) {
    return 'PostgreSQL'
  }
  if (normalized.indexOf('DM') >= 0) {
    return 'DM'
  }
  return 'MySQL'
})

onMounted(function () {
  loadDatasources()
  loadComparisonHistory()
})

async function loadDatasources() {
  try {
    datasources.value = await listDatasources()
    if (datasources.value.length > 0 && !form.sourceDatasourceId) {
      form.sourceDatasourceId = datasources.value[0].id
      await handleSourceDatasourceChange(form.sourceDatasourceId)
    }
    if (datasources.value.length > 1 && !form.targetDatasourceId) {
      form.targetDatasourceId = datasources.value[1].id
      await handleTargetDatasourceChange(form.targetDatasourceId)
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据源失败')
  }
}

async function handleSourceDatasourceChange(value) {
  form.sourceDatasourceId = value
  sourceSchemaOptions.value = []
  sourceTableOptions.value = []
  await loadMetadataOptions('source')
}

async function handleTargetDatasourceChange(value) {
  form.targetDatasourceId = value
  targetSchemaOptions.value = []
  targetTableOptions.value = []
  await loadMetadataOptions('target')
}

async function loadComparisonHistory() {
  historyLoading.value = true
  try {
    historyEntries.value = await listSchemaComparisonHistory(20)
  } catch (error) {
    ElMessage.error(error.message || '加载比较历史失败')
  } finally {
    historyLoading.value = false
  }
}

async function loadMetadataOptions(side) {
  const datasourceId = side === 'target' ? form.targetDatasourceId : form.sourceDatasourceId
  if (!datasourceId) {
    return
  }
  try {
    const schemas = await scanMetadata(datasourceId)
    const schemaNames = (schemas || []).map(function (schema) {
      return schema.schemaName
    })
    const tableNames = (schemas || []).reduce(function (acc, schema) {
      const tables = schema.tables || []
      for (let i = 0; i < tables.length; i += 1) {
        acc.push(tables[i].tableName)
      }
      return acc
    }, [])
    if (side === 'target') {
      targetSchemaOptions.value = schemaNames
      targetTableOptions.value = tableNames
    } else {
      sourceSchemaOptions.value = schemaNames
      sourceTableOptions.value = tableNames
    }
  } catch (error) {
    if (side === 'target') {
      targetSchemaOptions.value = []
      targetTableOptions.value = []
    } else {
      sourceSchemaOptions.value = []
      sourceTableOptions.value = []
    }
    ElMessage.error(error.message || '加载结构失败')
  }
}

async function runCompare() {
  if (!canCompare.value) {
    ElMessage.warning(compareDisabledReason.value)
    return
  }
  comparing.value = true
  errorMessage.value = ''
  activeResultTab.value = 'fields'
  try {
    const request = {
      sourceDatasource: selectedSourceDatasource.value,
      targetDatasource: selectedTargetDatasource.value,
      sourceSchemaName: form.sourceSchemaName,
      sourceTableName: form.sourceTableName,
      targetSchemaName: form.targetSchemaName,
      targetTableName: form.targetTableName
    }
    const response = await compareSchema(request)
    result.diffEntries = response.diffEntries || []
    result.suggestedSqlList = response.suggestedSqlList || []
    result.sourceTable = response.sourceTable || null
    result.targetTable = response.targetTable || null
    lastComparisonAt.value = Date.now()
    lastComparedSignature.value = comparisonSignature.value
    await loadComparisonHistory()
    const targetDatasource = findTargetDatasource()
    if (targetDatasource) {
      const preview = await previewSchemaSql({
        datasource: targetDatasource,
        sql: result.suggestedSqlList.join(';\n'),
        allowDangerousSql: true
      })
      previewMessage.value = preview.message || '已生成 SQL 预览'
    } else {
      previewMessage.value = '请先选择目标数据源'
    }
    ElMessage.success('比较完成')
  } catch (error) {
    errorMessage.value = error.message || '比较失败'
    previewMessage.value = ''
    lastComparisonAt.value = null
    ElMessage.error(errorMessage.value)
  } finally {
    comparing.value = false
  }
}

function findTargetDatasource() {
  return selectedTargetDatasource.value
}

async function copySql() {
  try {
    await navigator.clipboard.writeText(suggestedSqlList.value.join(';\n'))
    ElMessage.success('已复制 SQL')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

async function executeSql() {
  if (!hasSqlPreview.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认执行这组 DDL SQL 吗？', '执行确认', {
      type: 'warning'
    })
  } catch (error) {
    return
  }
  const targetDatasource = findTargetDatasource()
  if (!targetDatasource) {
    ElMessage.warning('请选择目标数据源')
    return
  }
  executing.value = true
  try {
    await executeSchemaSql({
      datasource: targetDatasource,
      sql: suggestedSqlList.value.join(';\n'),
      allowDangerousSql: true
    })
    ElMessage.success('DDL 执行成功')
  } catch (error) {
    ElMessage.error(error.message || 'DDL 执行失败')
  } finally {
    executing.value = false
  }
}

function diffTagType(diffType) {
  if (diffType === 'MISSING_COLUMN') {
    return 'warning'
  }
  if (diffType === 'EXTRA_COLUMN') {
    return 'info'
  }
  if (diffType === 'INDEX_DIFF') {
    return 'primary'
  }
  return 'danger'
}

function diffTypeLabel(diffType) {
  if (diffType === 'MISSING_COLUMN') {
    return '新增字段'
  }
  if (diffType === 'EXTRA_COLUMN') {
    return '缺失字段'
  }
  if (diffType === 'TYPE_DIFF') {
    return '类型不一致'
  }
  if (diffType === 'LENGTH_DIFF') {
    return '长度不一致'
  }
  if (diffType === 'NULLABLE_DIFF') {
    return 'nullable 差异'
  }
  if (diffType === 'DEFAULT_DIFF') {
    return '默认值差异'
  }
  if (diffType === 'PRIMARY_KEY_DIFF') {
    return '主键差异'
  }
  if (diffType === 'INDEX_DIFF') {
    return '索引差异'
  }
  return diffType || '-'
}

function formatQualifiedTable(schemaName, tableName) {
  const prefix = schemaName ? schemaName + '.' : ''
  return prefix + (tableName || '—')
}

function formatHistorySummary(diffSummary) {
  if (diffSummary === null || diffSummary === undefined || diffSummary === '') {
    return '—'
  }
  return diffSummary + ' 项差异'
}

function formatTime(value) {
  if (!value) {
    return '—'
  }
  const time = Number(value)
  if (!Number.isFinite(time)) {
    return '—'
  }
  return new Date(time).toLocaleString()
}

function countTableColumns(table) {
  if (!table || !table.columns || !table.columns.length) {
    return 0
  }
  return table.columns.length
}

function stepStateForLabel(label, activeLabel) {
  if (!comparing.value) {
    return 'pending'
  }
  const steps = ['读取源表结构', '读取目标表结构', '分析差异', '生成 SQL']
  const currentIndex = steps.indexOf(activeLabel)
  const stepIndex = steps.indexOf(label)
  if (stepIndex < 0) {
    return 'pending'
  }
  if (stepIndex < currentIndex) {
    return 'done'
  }
  if (stepIndex === currentIndex) {
    return 'active'
  }
  return 'pending'
}

async function generateSqlPreview() {
  if (!hasSqlPreview.value) {
    ElMessage.warning('暂无可生成的 SQL')
    return
  }
  activeResultTab.value = 'sqlPreview'
  ElMessage.success('已切换到 SQL 预览')
}

async function exportComparisonResult() {
  const payload = {
    sourceTable: sourceTablePath.value,
    targetTable: targetTablePath.value,
    diffCount: diffCount.value,
    sqlCount: suggestedSqlList.value.length,
    comparisonTime: lastComparisonAt.value
  }
  try {
    await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
    ElMessage.success('已复制比较结果摘要')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}
</script>
