import { api } from './client'

export function deleteAccount(): Promise<void> {
  return api.delete('/account')
}
