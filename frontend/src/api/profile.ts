import { api, ApiError } from './client'
import type { UserProfile, ProfileTemplate, TaskMix } from '../types'

export async function getProfile(): Promise<UserProfile | null> {
  try {
    return await api.get<UserProfile>('/profile')
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null
    throw e
  }
}

export function updateProfile(data: {
  task_mix?: TaskMix
  personal_adjustment_factor?: number
}): Promise<UserProfile> {
  return api.put<UserProfile>('/profile', data)
}

export function getProfileTemplates(): Promise<ProfileTemplate[]> {
  return api.get<ProfileTemplate[]>('/profile/templates')
}
