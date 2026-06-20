const STORAGE_KEY = 'db-sync-studio:first-launch-completed'

function isBrowserStorageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

export function getFirstLaunchState(value) {
  if (value === true) {
    return true
  }
  if (value === false || value === null || value === undefined) {
    return false
  }
  const normalized = String(value).trim().toLowerCase()
  return normalized === 'true' || normalized === '1' || normalized === 'yes'
}

export function shouldRedirectToWelcome(firstLaunchCompleted, currentPath) {
  return !getFirstLaunchState(firstLaunchCompleted) && !isFirstLaunchRouteAllowed(currentPath)
}

export function isFirstLaunchRouteAllowed(pathname) {
  const path = String(pathname || '')
  return path === '/welcome' || path === '/help'
}

export async function readFirstLaunchCompleted() {
  if (!isBrowserStorageAvailable()) {
    return false
  }
  return getFirstLaunchState(window.localStorage.getItem(STORAGE_KEY))
}

export async function markFirstLaunchCompleted() {
  if (!isBrowserStorageAvailable()) {
    return false
  }
  window.localStorage.setItem(STORAGE_KEY, 'true')
  return true
}

export async function resetFirstLaunchCompleted() {
  if (!isBrowserStorageAvailable()) {
    return false
  }
  window.localStorage.removeItem(STORAGE_KEY)
  return true
}
