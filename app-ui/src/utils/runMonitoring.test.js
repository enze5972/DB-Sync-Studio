import test from 'node:test'
import assert from 'node:assert/strict'

import { buildRunMonitoringSnapshot } from './runMonitoring.js'

test('buildRunMonitoringSnapshot aggregates metrics, failures, trends and datasource states', function () {
  const now = new Date('2026-06-19T10:00:00+08:00').getTime()
  const hour = 60 * 60 * 1000
  const day = 24 * hour
  const tasks = [
    {
      id: 1,
      taskName: 'Orders Sync'
    },
    {
      id: 2,
      taskName: 'Users Sync'
    }
  ]
  const datasources = [
    {
      id: 11,
      name: 'Source MySQL',
      type: 'MYSQL'
    },
    {
      id: 12,
      name: 'Target PG',
      type: 'POSTGRESQL'
    }
  ]
  const runsByTaskId = {
    1: [
      {
        taskId: 1,
        runId: 'run-today-failed',
        runStatus: 'FAILED',
        speedRowsPerSecond: 120.5,
        startedAt: now - hour,
        endedAt: now - hour + 180000,
        durationMillis: 180000,
        progressMessage: 'target write failed',
        errorMessage: 'duplicate key'
      },
      {
        taskId: 1,
        runId: 'run-yesterday-success',
        runStatus: 'SUCCESS',
        speedRowsPerSecond: 160.0,
        startedAt: now - day,
        endedAt: now - day + 240000,
        durationMillis: 240000
      }
    ],
    2: [
      {
        taskId: 2,
        runId: 'run-today-running',
        runStatus: 'RUNNING',
        speedRowsPerSecond: 88.0,
        startedAt: now - (2 * hour),
        durationMillis: 600000,
        progressMessage: 'copying rows'
      }
    ]
  }
  const datasourceStatuses = {
    11: {
      success: true,
      message: 'ok',
      costMillis: 35
    },
    12: {
      success: false,
      message: 'timeout',
      costMillis: 120
    }
  }

  const snapshot = buildRunMonitoringSnapshot({
    tasks,
    datasources,
    runsByTaskId,
    datasourceStatuses,
    now
  })

  assert.equal(snapshot.todaySummary.totalTaskCount, 2)
  assert.equal(snapshot.todaySummary.successTaskCount, 0)
  assert.equal(snapshot.todaySummary.failedTaskCount, 1)
  assert.equal(snapshot.todaySummary.runningTaskCount, 1)
  assert.equal(snapshot.todaySummary.averageSpeedText, '104.3 rows/s')

  assert.equal(snapshot.recentFailedTasks.length, 1)
  assert.equal(snapshot.recentFailedTasks[0].taskId, 1)
  assert.equal(snapshot.recentFailedTasks[0].runId, 'run-today-failed')

  assert.equal(snapshot.datasourceStates.length, 2)
  assert.equal(snapshot.datasourceStates[0].status, 'SUCCESS')
  assert.equal(snapshot.datasourceStates[1].status, 'FAILED')

  assert.equal(snapshot.trend.length, 7)
  assert.equal(snapshot.trend[snapshot.trend.length - 1].totalCount, 2)
  assert.equal(snapshot.trend[snapshot.trend.length - 1].failedCount, 1)
  assert.equal(snapshot.trend[snapshot.trend.length - 2].successCount, 1)
})
