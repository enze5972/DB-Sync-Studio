const DAY_MILLIS = 24 * 60 * 60 * 1000

export function buildRecentRunRecords(input) {
  const tasks = Array.isArray(input && input.tasks) ? input.tasks : []
  const runsByTaskId = input && input.runsByTaskId ? input.runsByTaskId : {}
  const limitPerTask = typeof (input && input.limitPerTask) === 'number' && input.limitPerTask > 0
    ? input.limitPerTask
    : 8
  const records = []

  tasks.forEach(function (task) {
    const taskRuns = Array.isArray(runsByTaskId[task.id]) ? runsByTaskId[task.id].slice() : []
    taskRuns.sort(function (left, right) {
      return resolveRunTime(right) - resolveRunTime(left)
    })
    taskRuns.slice(0, limitPerTask).forEach(function (run) {
      records.push(Object.assign({}, run, {
        taskId: task.id,
        taskName: task.taskName || ('任务 #' + task.id)
      }))
    })
  })

  records.sort(function (left, right) {
    return resolveRunTime(right) - resolveRunTime(left)
  })

  return records
}

export function buildRunDetailStats(records, now) {
  const list = Array.isArray(records) ? records : []
  const currentTime = Number.isFinite(Number(now)) ? Number(now) : Date.now()
  const todayStart = startOfDay(currentTime)
  const todayEnd = todayStart + DAY_MILLIS
  const todayRuns = list.filter(function (record) {
    const time = resolveRunTime(record)
    return time >= todayStart && time < todayEnd
  })

  return {
    todayCount: todayRuns.length,
    runningCount: countByStatus(todayRuns, ['RUNNING']),
    successCount: countByStatus(todayRuns, ['SUCCESS', 'PARTIAL_SUCCESS']),
    failedCount: countByStatus(todayRuns, ['FAILED'])
  }
}

export function filterRecentRunRecords(records, filters) {
  const list = Array.isArray(records) ? records : []
  const keyword = normalizeKeyword(filters && filters.keyword)
  const status = normalizeStatus(filters && filters.status)
  const startTime = toTimeNumber(filters && filters.startTime)
  const endTime = toTimeNumber(filters && filters.endTime)

  return list.filter(function (record) {
    if (!record) {
      return false
    }
    if (status && status !== 'ALL' && normalizeText(record.runStatus) !== status) {
      return false
    }
    if (keyword) {
      const haystack = [
        record.runId,
        record.taskName,
        record.runStatus,
        record.progressMessage,
        record.errorMessage
      ].join(' ').toLowerCase()
      if (haystack.indexOf(keyword) < 0) {
        return false
      }
    }
    const time = resolveRunTime(record)
    if (startTime !== null && time < startTime) {
      return false
    }
    if (endTime !== null && time > endTime) {
      return false
    }
    return true
  })
}

export function resolveRunTaskId(runId, records) {
  if (!runId || !Array.isArray(records)) {
    return null
  }
  const normalized = String(runId)
  for (let index = 0; index < records.length; index += 1) {
    const record = records[index]
    if (record && String(record.runId) === normalized && record.taskId !== undefined && record.taskId !== null) {
      return record.taskId
    }
  }
  return null
}

function resolveRunTime(record) {
  return Number(record && (record.startedAt || record.endedAt || record.updatedAt || record.createdAt || 0)) || 0
}

function countByStatus(records, statuses) {
  return records.filter(function (record) {
    return statuses.indexOf(record.runStatus) >= 0
  }).length
}

function normalizeText(value) {
  return value === null || value === undefined ? '' : String(value).trim().toUpperCase()
}

function normalizeKeyword(value) {
  return value === null || value === undefined ? '' : String(value).trim().toLowerCase()
}

function normalizeStatus(value) {
  return value === null || value === undefined ? '' : String(value).trim().toUpperCase()
}

function toTimeNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

function startOfDay(time) {
  const date = new Date(time)
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}
