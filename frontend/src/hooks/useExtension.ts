import { useEffect, useState } from 'react'

const EXT_ID = import.meta.env.VITE_EXTENSION_ID as string | undefined

declare global {
  interface Window {
    chrome?: {
      runtime?: {
        sendMessage(
          extensionId: string,
          message: unknown,
          callback: (response?: { ok?: boolean }) => void
        ): void
      }
    }
  }
}

export type ExtensionStatus = 'checking' | 'installed' | 'not_installed' | 'unavailable'

export function useExtension() {
  const [status, setStatus] = useState<ExtensionStatus>('checking')

  useEffect(() => {
    if (!EXT_ID || !window.chrome?.runtime?.sendMessage) {
      setStatus('unavailable')
      return
    }
    try {
      window.chrome.runtime.sendMessage(EXT_ID, { type: 'rippl-ping' }, (response) => {
        setStatus(response?.ok === true ? 'installed' : 'not_installed')
      })
    } catch {
      setStatus('not_installed')
    }
  }, [])

  const sendToken = (token: string): Promise<boolean> => {
    if (!EXT_ID || !window.chrome?.runtime?.sendMessage) return Promise.resolve(false)
    return new Promise((resolve) => {
      try {
        window.chrome!.runtime!.sendMessage(EXT_ID, { type: 'rippl-auth', token }, (response) => {
          resolve(response?.ok === true)
        })
      } catch {
        resolve(false)
      }
    })
  }

  return { status, sendToken, extensionId: EXT_ID }
}
