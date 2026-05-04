import { api } from './client'
import type { WeeklyTrend, MonthlyTrend, TimeSaved } from '../types'

export function getWeeklyTrends(from?: string, to?: string): Promise<WeeklyTrend[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const q = params.toString()
  return api.get(`/trends/weekly${q ? '?' + q : ''}`)
}

export function getMonthlyTrends(from?: string, to?: string): Promise<MonthlyTrend[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const q = params.toString()
  return api.get(`/trends/monthly${q ? '?' + q : ''}`)
}

export function getTimeSaved(): Promise<TimeSaved> {
  return api.get('/trends/time-saved')
}

export function getActivityHeatmap(): Promise<number[][]> {
  return api.get('/trends/activity-heatmap')
}
