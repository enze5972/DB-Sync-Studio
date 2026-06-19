const DAY_MILLIS = 24 * 60 * 60 * 1000
const TREND_DAYS = 7

export function buildRunMonitoringSnapshot(input) {
  const now = input && input.now ? Number(input.now) : Date.now()
  const tasks = Array.isArray(input && input.tasks) ? input.tasks : []
  const datasources = Array.isArray(input && input.datasources) ? input.datasources : []
  const runsByTaskId = input && input.runsByTaskId ? input.runsByTaskId : {}
  const datasourceStatuses = input && input.datasourceStatuses ? input.datasourceStatuses : {}
  const latestRuns = []
  const allRuns = []

  tasks.forEach(function (task) {
    const taskRuns = Array.isArray(runsByTaskId[task.id]) ? runsByTaskId[task.id].slice() : []
    taskRuns.sort(function (left, right) {
      return resolveRunTime(right) - resolveRunTime(left)
    })
    if (taskRuns.length > 0) {
      const latestRun = Object.assign({}, taskRuns[0], {
        taskName: task.taskName || ('任务 #' + task.id)
      })
      latestRuns.push(latestRun)
    }
    taskRuns.forEach(function (run) {
      allRuns.push(Object.assign({}, run, {
        taskName: task.taskName || ('任务 #' + task.id)
      }))
    })
  })

  latestRuns.sort(function (left, right) {
    return resolveRunTime(right) - resolveRunTime(left)
  })
  allRuns.sort(function (left, right) {
    return resolveRunTime(right) - resolveRunTime(left)
  })

  const todayStart = startOfDay(now)
  const todayEnd = todayStart + DAY_MILLIS
  const latestTodayRuns = latestRuns.filter(function (run) {
    const time = resolveRunTime(run)
    return time >= todayStart && time < todayEnd
  })
  const speedValues = latestTodayRuns
    .map(function (run) {
      return Number(run.speedRowsPerSecond)
    })
    .filter(function (value) {
      return Number.isFinite(value) && value > 0
    })

  const todaySummary = {
    totalTaskCount: latestTodayRuns.length,
    successTaskCount: countByStatus(latestTodayRuns, ['SUCCESS', 'PARTIAL_SUCCESS']),
    failedTaskCount: countByStatus(latestTodayRuns, ['FAILED']),
    runningTaskCount: countByStatus(latestTodayRuns, ['RUNNING']),
    averageSpeed: average(speedValues),
    averageSpeedText: formatSpeed(average(speedValues))
  }

  const recentFailedTasks = latestRuns
    .filter(function (run) {
      return run.runStatus === 'FAILED'
    })
    .slice(0, 6)
    .map(function (run) {
      return {
        taskId: run.taskId,
        taskName: run.taskName,
        runId: run.runId,
        runStatus: run.runStatus,
        message: run.errorMessage || run.progressMessage || '执行失败',
        startedAt: run.startedAt,
        endedAt: run.endedAt,
        durationMillis: run.durationMillis
      }
    })

  const datasourceStates = datasources.map(function (datasource) {
    const result = datasourceStatuses[datasource.id] || null
    return {
      datasourceId: datasource.id,
      name: datasource.name || ('数据源 #' + datasource.id),
      type: datasource.type || '-',
      status: result ? (result.success ? 'SUCCESS' : 'FAILED') : 'UNKNOWN',
      message: result ? (result.message || (result.success ? '连接正常' : '连接失败')) : '未测试',
      latencyText: result && Number.isFinite(Number(result.costMillis))
        ? Number(result.costMillis) + ' ms'
        : '-'
    }
  })

  const trend = buildTrend(allRuns, now)

  return {
    todaySummary,
    recentFailedTasks,
    datasourceStates,
    trend,
    latestRuns
  }
}

function buildTrend(allRuns, now) {
  const runsByDay = {}
  allRuns.forEach(function (run) {
    const timestamp = resolveRunTime(run)
    const dayKey = formatDayKey(startOfDay(timestamp))
    if (!runsByDay[dayKey]) {
      runsByDay[dayKey] = []
    }
    runsByDay[dayKey].push(run)
  })

  const result = []
  for (let index = TREND_DAYS - 1; index >= 0; index -= 1) {
    const dayTime = startOfDay(now) - (index * DAY_MILLIS)
    const dayKey = formatDayKey(dayTime)
    const dayRuns = runsByDay[dayKey] || []
    result.push({
      dayKey: dayKey,
      label: formatDayLabel(dayTime),
      totalCount: dayRuns.length,
      successCount: countByStatus(dayRuns, ['SUCCESS', 'PARTIAL_SUCCESS']),
      failedCount: countByStatus(dayRuns, ['FAILED']),
      runningCount: countByStatus(dayRuns, ['RUNNING'])
    })
  }
  return result
}

function countByStatus(runs, statuses) {
  return runs.filter(function (run) {
    return statuses.indexOf(run.runStatus) >= 0
  }).length
}

function average(values) {
  if (!values.length) {
    return 0
  }
  const total = values.reduce(function (sum, value) {
    return sum + value
  }, 0)
  return total / values.length
}

function resolveRunTime(run) {
  return Number(run && (run.startedAt || run.endedAt || run.updatedAt || run.createdAt || 0)) || 0
}

function startOfDay(time) {
  const date = new Date(time)
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

function formatDayKey(time) {
  const date = new Date(time)
  const year = date.getFullYear()
  const month = padNumber(date.getMonth() + 1)
  const day = padNumber(date.getDate())
  return year + '-' + month + '-' + day
}

function formatDayLabel(time) {
  const date = new Date(time)
  return padNumber(date.getMonth() + 1) + '/' + padNumber(date.getDate())
}

function padNumber(value) {
  return value < 10 ? ('0' + value) : String(value)
}

function formatSpeed(value) {
  if (!value) {
    return '-'
  }
  return value.toFixed(1) + ' rows/s'
}
