import { useEffect, useState } from 'react'
import { getProfileTemplates, updateProfile } from '../api/profile'
import type { ProfileTemplate, TaskMix } from '../types'

const TASK_LABELS: Record<keyof TaskMix, string> = {
  writing: 'Writing',
  coding: 'Coding',
  research: 'Research',
  planning: 'Planning',
  communication: 'Communication',
  other: 'Other',
}

const TASK_KEYS = Object.keys(TASK_LABELS) as (keyof TaskMix)[]

const DEFAULT_MIX: TaskMix = {
  writing: 0.15, coding: 0.15, research: 0.2,
  planning: 0.15, communication: 0.2, other: 0.15,
}

interface Props {
  onComplete: () => void
}

export function ProfileOnboarding({ onComplete }: Props) {
  const [templates, setTemplates] = useState<ProfileTemplate[]>([])
  const [mix, setMix] = useState<TaskMix>(DEFAULT_MIX)
  const [mode, setMode] = useState<'pick' | 'custom'>('pick')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getProfileTemplates().then(setTemplates)
  }, [])

  const selectTemplate = (template: ProfileTemplate) => {
    setMix(template.task_mix)
    setMode('custom')
  }

  const adjustSlider = (key: keyof TaskMix, value: number) => {
    const clamped = Math.max(0, Math.min(1, value))
    const others = TASK_KEYS.filter(k => k !== key)
    const otherSum = others.reduce((s, k) => s + mix[k], 0)
    const remaining = 1 - clamped

    const updated = { ...mix, [key]: clamped }
    if (otherSum > 0) {
      const scale = remaining / otherSum
      others.forEach(k => { updated[k] = Math.max(0, mix[k] * scale) })
    } else {
      const even = remaining / others.length
      others.forEach(k => { updated[k] = even })
    }
    setMix(updated)
  }

  const save = async () => {
    setSaving(true)
    try {
      await updateProfile({ task_mix: mix })
      onComplete()
    } finally {
      setSaving(false)
    }
  }

  const skip = async () => {
    setSaving(true)
    try {
      await updateProfile({ task_mix: DEFAULT_MIX })
      onComplete()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto mt-12 px-4">
      <h2 className="text-2xl font-bold font-serif text-fg text-center">How do you use AI?</h2>
      <p className="mt-2 text-fg-secondary text-center text-sm">
        This helps us personalize your time-saved estimates.
      </p>

      {mode === 'pick' && (
        <div className="mt-8 space-y-3">
          {templates.map(t => (
            <button
              key={t.name}
              onClick={() => selectTemplate(t)}
              className="w-full p-4 rounded-2xl text-left border border-default bg-card hover:border-ring transition-colors"
            >
              <p className="font-medium text-fg capitalize">{t.name}</p>
              <p className="text-xs text-fg-muted mt-1">
                {TASK_KEYS.filter(k => t.task_mix[k] > 0)
                  .map(k => `${TASK_LABELS[k]} ${Math.round(t.task_mix[k] * 100)}%`)
                  .join(' · ')}
              </p>
            </button>
          ))}
          <button
            onClick={() => setMode('custom')}
            className="w-full p-4 rounded-2xl text-left border border-dashed border-default text-fg-muted hover:text-fg-active hover:border-ring transition-colors"
          >
            <p className="font-medium">Custom mix</p>
            <p className="text-xs mt-1">Set your own weights</p>
          </button>
        </div>
      )}

      {mode === 'custom' && (
        <div className="mt-8 space-y-4">
          {TASK_KEYS.map(key => (
            <div key={key} className="flex items-center gap-3">
              <span className="w-28 text-sm text-fg-secondary">{TASK_LABELS[key]}</span>
              <input
                type="range"
                min={0}
                max={100}
                value={Math.round(mix[key] * 100)}
                onChange={e => adjustSlider(key, parseInt(e.target.value) / 100)}
                className="flex-1 accent-primary"
              />
              <span className="w-10 text-right text-sm text-fg-muted">
                {Math.round(mix[key] * 100)}%
              </span>
            </div>
          ))}
          <div className="flex gap-3 mt-6">
            <button
              onClick={save}
              disabled={saving}
              className="flex-1 py-3 bg-primary text-fg-on-primary rounded-2xl hover:bg-primary-hover disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save profile'}
            </button>
            <button
              onClick={() => setMode('pick')}
              className="px-4 py-3 text-fg-muted hover:text-fg-active text-sm"
            >
              Back
            </button>
          </div>
        </div>
      )}

      <button
        onClick={skip}
        disabled={saving}
        className="w-full mt-4 text-center text-sm text-fg-muted hover:text-fg-secondary"
      >
        Skip for now
      </button>
    </div>
  )
}
