export interface User {
  id: string
  email: string
}

export interface WeeklyTrend {
  week: string
  domain: string
  totalSeconds: number
  totalSaved: number
  confidence: 'low' | 'medium' | 'high'
}

export interface MonthlyTrend {
  month: string
  domain: string
  totalSeconds: number
  totalSaved: number
  confidence: 'low' | 'medium' | 'high'
}

export interface TimeSaved {
  total: number
  confidence: 'low' | 'medium' | 'high'
  byDomain: Record<string, number>
  byTaskMix: Record<string, number>
}

export interface MirrorMoment {
  type: string
  message: string
}

export interface CollectorInfo {
  id: string
  type: string
  enabled: boolean
  linkedAt: string
  lastSyncAt?: string
  token?: string
}

export interface TaskMix {
  writing: number
  coding: number
  research: number
  planning: number
  communication: number
  other: number
}

export interface UserProfile {
  task_mix: TaskMix
  personal_adjustment_factor: number
  onboarded: boolean
}

export interface ProfileTemplate {
  name: string
  task_mix: TaskMix
}
