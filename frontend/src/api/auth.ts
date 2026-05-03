import { api } from './client'
import type { User } from '../types'

export function sendMagicLink(email: string): Promise<{ sent: boolean }> {
  return api.post('/auth/magic-link', { email })
}

export function getMe(): Promise<User> {
  return api.get<User>('/auth/me')
}

export function logout(): Promise<void> {
  return api.post('/auth/logout')
}
