import { useMemo } from 'react'
import { useTheme } from '../context/ThemeContext'

interface ActivityPondProps {
  data?: number[][]
}

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

const MOCK_DATA = [
  [0,0,0,0,0,0,0,0.2,0.5,0.8,0.9,0.7,0.4,0.6,0.8,0.9,0.7,0.5,0.3,0.1,0,0,0,0],
  [0,0,0,0,0,0,0,0.3,0.6,0.7,0.8,0.9,0.5,0.7,0.9,1.0,0.8,0.6,0.4,0.2,0,0,0,0],
  [0,0,0,0,0,0,0.1,0.4,0.7,0.9,1.0,1.0,0.6,0.8,1.0,1.0,0.9,0.7,0.5,0.3,0.1,0,0,0],
  [0,0,0,0,0,0,0,0.3,0.5,0.7,0.8,0.6,0.3,0.5,0.7,0.8,0.6,0.4,0.2,0.1,0,0,0,0],
  [0,0,0,0,0,0,0,0.2,0.4,0.6,0.7,0.5,0.3,0.4,0.6,0.5,0.3,0.2,0.1,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0.1,0.2,0.3,0.2,0.1,0.2,0.1,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0.1,0.1,0,0,0.1,0,0,0,0,0,0,0,0,0],
]

export function ActivityPond({ data }: ActivityPondProps) {
  const grid = data ?? MOCK_DATA
  const maxVal = Math.max(1, ...grid.flat())
  const normalized = grid.map(row => row.map(v => v / maxVal))

  const { resolved } = useTheme()
  const { heatBase, glowBase, opacityScale } = useMemo(() => {
    const styles = getComputedStyle(document.documentElement)
    return {
      heatBase: styles.getPropertyValue('--heatmap-base').trim() || '92,122,82',
      glowBase: styles.getPropertyValue('--heatmap-glow-base').trim() || '143,184,122',
      opacityScale: parseFloat(styles.getPropertyValue('--heatmap-opacity-scale').trim() || '0.8'),
    }
  }, [resolved])

  return (
    <div className="pond-card">
      <p className="text-xs uppercase tracking-widest mb-4 text-fg-muted" style={{ letterSpacing: '1px' }}>
        Time Saved by Day & Hour
      </p>
      <div className="flex">
        <div className="flex flex-col justify-between mr-2" style={{ gap: '3px' }}>
          {DAYS.map(d => (
            <div key={d} className="text-[10px] flex items-center text-fg-muted" style={{ height: '100%' }}>{d}</div>
          ))}
        </div>
        <div className="flex-1">
          <div className="grid gap-[3px]" style={{ gridTemplateColumns: 'repeat(24, 1fr)' }}>
            {normalized.flatMap((dayRow, day) =>
              dayRow.map((val, hour) => (
                <div
                  key={`${day}-${hour}`}
                  className="heatmap-cell"
                  title={`${DAYS[day]} ${hour}:00 — ${Math.round(val * 60)} min`}
                  style={{
                    backgroundColor: `rgba(${heatBase},${val * opacityScale})`,
                    boxShadow: val > 0.7 ? `0 0 ${val * 8}px rgba(${glowBase},${val * 0.4})` : 'none',
                  }}
                />
              ))
            )}
          </div>
          <div className="flex justify-between mt-2">
            {['0:00', '6:00', '12:00', '18:00', '23:00'].map(t => (
              <span key={t} className="text-[10px] text-fg-muted">{t}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
