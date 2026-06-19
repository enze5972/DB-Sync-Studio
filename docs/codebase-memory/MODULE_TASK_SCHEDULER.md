# MODULE_TASK_SCHEDULER

## 模块用途

本地调度、运行中保护、跳过执行处理、计划任务触发。

## 关键目录

- `app-core/src/main/java/com/dbsyncstudio/core/scheduler`
- `app-core/src/main/java/com/dbsyncstudio/core/backend`

## 关键文件

- `app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskSchedulerService.java`
- `app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskScheduleCalculator.java`
- `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`

## 核心类/组件/配置

- `TaskSchedulerService`
- `TaskScheduleCalculator`
- `schedule_enabled`
- `schedule_type`
- `schedule_cron_expression`
- `schedule_interval_seconds`
- `schedule_next_run_at`

## 什么时候需要读取

- 需要改定时触发、下一次执行时间、跳过执行逻辑时
- 需要改运行中保护、暂停恢复、手动触发兼容时
- 需要改调度相关日志和告警时

## 什么时候不需要读取

- 只改前端页面展示时
- 只改 SQLite 迁移表结构时

## 已知问题

- 调度逻辑和任务执行链路耦合较紧，任务前要先定位具体触发点
- 运行中跳过需要继续保持幂等，待确认

## 后续待办

- 继续验证跳过执行的日志和告警触发
- 继续验证 schedule_last_run_at / schedule_next_run_at 更新是否正确
