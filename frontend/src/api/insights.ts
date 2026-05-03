import { api } from './client'
import type { MirrorMoment } from '../types'

export function getMirrorMoments(): Promise<MirrorMoment[]> {
  return api.get('/insights/mirror')
}
