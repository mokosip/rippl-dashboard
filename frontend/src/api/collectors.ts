import { api } from './client'
import type { CollectorInfo } from '../types'

export function getCollectors(): Promise<CollectorInfo[]> {
  return api.get('/collectors')
}

export function addCollector(type: string): Promise<CollectorInfo> {
  return api.post('/collectors', { type })
}

export function removeCollector(id: string): Promise<void> {
  return api.delete(`/collectors/${id}`)
}
