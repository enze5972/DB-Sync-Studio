import assert from 'node:assert/strict'
import {
  getFirstLaunchState,
  isFirstLaunchRouteAllowed,
  shouldRedirectToWelcome
} from '../src/services/onboardingState.js'

assert.equal(getFirstLaunchState(null), false)
assert.equal(getFirstLaunchState(undefined), false)
assert.equal(getFirstLaunchState('true'), true)
assert.equal(getFirstLaunchState('1'), true)
assert.equal(getFirstLaunchState('yes'), true)
assert.equal(getFirstLaunchState('false'), false)
assert.equal(shouldRedirectToWelcome(false, '/'), true)
assert.equal(shouldRedirectToWelcome(false, '/welcome'), false)
assert.equal(shouldRedirectToWelcome(true, '/'), false)
assert.equal(isFirstLaunchRouteAllowed('/welcome'), true)
assert.equal(isFirstLaunchRouteAllowed('/'), false)

console.log('onboarding state checks passed')
