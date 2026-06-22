import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildRunDetailStats,
  buildRecentRunRecords,
  filterRecentRunRecords,
  resolveRunTaskId
} from './runDetail.js'

test('buildRecentRunRecords flattens runs and preserves task names', function () {
  const records = buildRecentRunRecords({
    tasks: [
      { id: 1, taskName: 'Orders Sync' },
      { id: 2, taskName: 'Users Sync' }
    ],
    runsByTaskId: {
      1: [
        { taskId: 1, runId: 'run-1', runStatus: 'SUCCESS', startedAt: 2000, totalTableCount: 3, completedTableCount: 3, syncedRowCount: 120 },
        { taskId: 1, runId: 'run-0', runStatus: 'FAILED', startedAt: 1000, totalTableCount: 3, completedTableCount: 1, syncedRowCount: 20 }
      ],
      2: [
        { taskId: 2, runId: 'run-2', runStatus: 'RUNNING', startedAt: 3000, totalTableCount: 5, completedTableCount: 2, syncedRowCount: 45 }
      ]
    },
    limitPerTask: 2
  })

  assert.equal(records.length, 3)
  assert.equal(records[0].runId, 'run-2')
  assert.equal(records[0].taskName, 'Users Sync')
  assert.equal(records[1].runId, 'run-1')
  assert.equal(records[2].runId, 'run-0')
})

test('buildRunDetailStats counts today running success and failed runs', function () {
  const now = new Date('2026-06-22T10:00:00+08:00').getTime()
  const day = 24 * 60 * 60 * 1000
  const stats = buildRunDetailStats([
    { startedAt: now - 3600000, runStatus: 'SUCCESS' },
    { startedAt: now - 7200000, runStatus: 'FAILED' },
    { startedAt: now - 10800000, runStatus: 'RUNNING' },
    { startedAt: now - day, runStatus: 'SUCCESS' }
  ], now)

  assert.equal(stats.todayCount, 3)
  assert.equal(stats.runningCount, 1)
  assert.equal(stats.successCount, 1)
  assert.equal(stats.failedCount, 1)
})

test('filterRecentRunRecords filters by keyword, status and time range', function () {
  const now = new Date('2026-06-22T10:00:00+08:00').getTime()
  const records = [
    { runId: 'run-1', taskName: 'Orders Sync', runStatus: 'SUCCESS', startedAt: now - 1000 },
    { runId: 'run-2', taskName: 'Users Sync', runStatus: 'FAILED', startedAt: now - 2000 },
    { runId: 'run-3', taskName: 'Orders Sync', runStatus: 'RUNNING', startedAt: now - 86400000 }
  ]

  const filtered = filterRecentRunRecords(records, {
    keyword: 'orders',
    status: 'SUCCESS',
    startTime: String(now - 20000),
    endTime: String(now)
  })

  assert.equal(filtered.length, 1)
  assert.equal(filtered[0].runId, 'run-1')
})

test('resolveRunTaskId matches the latest record for a run id', function () {
  const taskId = resolveRunTaskId('run-2', [
    { runId: 'run-1', taskId: 1 },
    { runId: 'run-2', taskId: 2 },
    { runId: 'run-2', taskId: 3 }
  ])

  assert.equal(taskId, 2)
})
