import { useEffect, useRef, useState } from 'react'
import type { WeeklyTrend } from '../types'
import { getDomain } from '../data/domains'
import { useTheme } from '../context/ThemeContext'

interface Ripple {
  x: number; y: number; r: number; opacity: number
}

interface TooltipState {
  visible: boolean
  x: number; y: number
  week: string; total: number
  breakdown: { domain: string; minutes: number }[]
}

export function PondChart({ data }: { data: WeeklyTrend[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [tooltip, setTooltip] = useState<TooltipState>({ visible: false, x: 0, y: 0, week: '', total: 0, breakdown: [] })
  const { resolved } = useTheme()

  const domains = [...new Set(data.map(d => d.domain))]
  const byWeek: Record<string, Record<string, number>> = {}
  for (const d of data) {
    if (!byWeek[d.week]) byWeek[d.week] = {}
    byWeek[d.week][d.domain] = Math.round(d.totalSeconds / 60)
  }
  const weeks = Object.keys(byWeek).sort()
  const maxVal = Math.max(20, ...weeks.flatMap(w => domains.map(d => byWeek[w][d] ?? 0))) * 1.2

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || weeks.length === 0) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const styles = getComputedStyle(document.documentElement)
    const gridColor = styles.getPropertyValue('--chart-grid').trim()
    const rippleColor = styles.getPropertyValue('--chart-ripple').trim()

    const W = canvas.width
    const H = canvas.height
    let time = 0
    let animId = 0
    let lastHoveredWeek = -1
    const activeRipples: Ripple[] = []

    function getY(val: number) { return H - (val / maxVal) * (H - 40) - 20 }
    function getX(i: number) { return 40 + (i / Math.max(weeks.length - 1, 1)) * (W - 80) }

    function drawWave(values: number[], color: string, fillColor: string, offset: number) {
      const points = values.map((v, i) => ({
        x: getX(i),
        y: getY(v) + Math.sin(time * 0.02 + i * 0.8 + offset) * 3,
      }))

      ctx!.beginPath()
      ctx!.moveTo(points[0].x, points[0].y)
      for (let i = 1; i < points.length; i++) {
        const cpx = (points[i-1].x + points[i].x) / 2
        ctx!.bezierCurveTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
      }
      ctx!.lineTo(points[points.length-1].x, H)
      ctx!.lineTo(points[0].x, H)
      ctx!.closePath()
      ctx!.fillStyle = fillColor
      ctx!.fill()

      ctx!.beginPath()
      ctx!.moveTo(points[0].x, points[0].y)
      for (let i = 1; i < points.length; i++) {
        const cpx = (points[i-1].x + points[i].x) / 2
        ctx!.bezierCurveTo(cpx, points[i-1].y, cpx, points[i].y, points[i].x, points[i].y)
      }
      ctx!.strokeStyle = color
      ctx!.lineWidth = 2
      ctx!.stroke()

      for (const p of points) {
        ctx!.beginPath()
        ctx!.arc(p.x, p.y, 3, 0, Math.PI * 2)
        ctx!.fillStyle = color
        ctx!.fill()
      }

      return points
    }

    const colorPairs = [
      { stroke: styles.getPropertyValue('--chart-wave-1').trim(), fill: styles.getPropertyValue('--chart-fill-1').trim() },
      { stroke: styles.getPropertyValue('--chart-wave-2').trim(), fill: styles.getPropertyValue('--chart-fill-2').trim() },
      { stroke: styles.getPropertyValue('--chart-wave-3').trim(), fill: styles.getPropertyValue('--chart-fill-3').trim() },
      { stroke: styles.getPropertyValue('--chart-wave-2').trim(), fill: styles.getPropertyValue('--chart-fill-2').trim() },
    ]
    const domainColors: Record<string, { stroke: string; fill: string }> = {}
    domains.forEach((d, i) => { domainColors[d] = colorPairs[i % colorPairs.length] })

    function animate() {
      time++
      ctx!.clearRect(0, 0, W, H)

      ctx!.strokeStyle = gridColor
      ctx!.lineWidth = 0.5
      for (let i = 0; i <= 4; i++) {
        const y = 20 + i * (H - 40) / 4
        ctx!.beginPath(); ctx!.moveTo(40, y); ctx!.lineTo(W - 40, y); ctx!.stroke()
      }

      for (let di = domains.length - 1; di >= 0; di--) {
        const domain = domains[di]
        const values = weeks.map(w => byWeek[w][domain] ?? 0)
        const colors = domainColors[domain]
        drawWave(values, colors.stroke, colors.fill, di * 2)
      }

      for (let i = activeRipples.length - 1; i >= 0; i--) {
        const rip = activeRipples[i]
        rip.r += 0.8
        rip.opacity -= 0.008
        if (rip.opacity <= 0) { activeRipples.splice(i, 1); continue }
        ctx!.beginPath()
        ctx!.arc(rip.x, rip.y, rip.r, 0, Math.PI * 2)
        ctx!.globalAlpha = rip.opacity
        ctx!.strokeStyle = rippleColor
        ctx!.lineWidth = 1
        ctx!.stroke()
        ctx!.globalAlpha = 1
      }

      animId = requestAnimationFrame(animate)
    }

    function handleMouseMove(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const mx = (e.clientX - rect.left) * (W / rect.width)

      let closest = 0
      let closestDist = Infinity
      for (let i = 0; i < weeks.length; i++) {
        const d = Math.abs(getX(i) - mx)
        if (d < closestDist) { closestDist = d; closest = i }
      }

      if (closestDist < 40) {
        const week = weeks[closest]
        const breakdown = domains.map(d => ({ domain: getDomain(d).name, minutes: byWeek[week][d] ?? 0 })).filter(b => b.minutes > 0)
        const total = breakdown.reduce((s, b) => s + b.minutes, 0)
        const rawX = e.clientX - rect.left + 16
        const clampedX = Math.min(rawX, rect.width - 160)
        setTooltip({
          visible: true,
          x: clampedX,
          y: e.clientY - rect.top - 60,
          week: new Date(week).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
          total,
          breakdown,
        })

        if (closest !== lastHoveredWeek) {
          lastHoveredWeek = closest
          const primaryValues = weeks.map(w => byWeek[w][domains[0]] ?? 0)
          activeRipples.push({ x: getX(closest), y: getY(primaryValues[closest]), r: 3, opacity: 0.35 })
        }
      } else {
        setTooltip(prev => ({ ...prev, visible: false }))
      }
    }

    function handleMouseLeave() {
      setTooltip(prev => ({ ...prev, visible: false }))
      lastHoveredWeek = -1
    }

    function handleClick(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const cx = (e.clientX - rect.left) * (W / rect.width)
      const cy = (e.clientY - rect.top) * (H / rect.height)
      for (let i = 0; i < 3; i++) {
        activeRipples.push({ x: cx, y: cy, r: i * 8, opacity: 0.5 - i * 0.1 })
      }
    }

    canvas.addEventListener('mousemove', handleMouseMove)
    canvas.addEventListener('mouseleave', handleMouseLeave)
    canvas.addEventListener('click', handleClick)
    animate()

    return () => {
      cancelAnimationFrame(animId)
      canvas.removeEventListener('mousemove', handleMouseMove)
      canvas.removeEventListener('mouseleave', handleMouseLeave)
      canvas.removeEventListener('click', handleClick)
    }
  }, [data, resolved])

  if (weeks.length === 0) return null

  const weekLabels = weeks.filter((_, i) => i % Math.max(1, Math.floor(weeks.length / 5)) === 0)

  return (
    <div className="pond-card" ref={containerRef} style={{ overflow: 'visible' }}>
      <p className="text-xs uppercase tracking-widest mb-4 text-fg-muted" style={{ letterSpacing: '1px' }}>
        AI Usage (minutes/week)
      </p>
      <div className="pond-surface">
        <canvas ref={canvasRef} width={936} height={280} />
        <div className={`pond-tooltip ${tooltip.visible ? 'visible' : ''}`}
          style={{ left: tooltip.x, top: tooltip.y }}>
          <div className="text-xs mb-1 text-fg-muted">Week of {tooltip.week}</div>
          <div className="text-lg font-semibold text-fg-accent">{tooltip.total} min</div>
          {tooltip.breakdown.map(b => (
            <div key={b.domain} className="text-xs mt-0.5 text-fg-secondary">
              {b.domain}: {b.minutes}
            </div>
          ))}
        </div>
      </div>
      <div className="flex justify-between pt-2">
        {weekLabels.map(w => (
          <span key={w} className="text-xs text-fg-muted">
            {new Date(w).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
          </span>
        ))}
      </div>
      <div className="flex gap-4 pt-3 justify-center flex-wrap">
        {domains.map((domain, i) => (
          <div key={domain} className="flex items-center gap-1.5">
            <div className="w-2.5 h-2.5 rounded-full" style={{
              backgroundColor: `var(--chart-wave-${(i % 3) + 1})`
            }} />
            <span className="text-xs text-fg-secondary">{getDomain(domain).name}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
