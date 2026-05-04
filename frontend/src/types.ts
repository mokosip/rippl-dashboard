export interface User {
  id: string
  email: string
}

export interface WeeklyTrend {
  week: string
  domain: string
  totalSeconds: number
  totalSaved: number
}

export interface MonthlyTrend {
  month: string
  domain: string
  totalSeconds: number
  totalSaved: number
}

export interface TimeSaved {
  total: number
  byDomain: Record<string, number>
  byActivity: Record<string, number>
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
