import { useEffect, useRef } from 'react'

interface WaveBarData {
  name: string
  value: number
  color: string
}

export function WaveBarChart({ data }: { data: WaveBarData[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const barH = 28
  const rowH = barH + 14
  const canvasH = data.length * rowH + 10

  useEffect(() => {
    const canvas = canvasRef.current
    const container = containerRef.current
    if (!canvas || !container || data.length === 0) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const dpr = window.devicePixelRatio || 1
    const displayW = container.clientWidth
    const displayH = canvasH

    canvas.width = displayW * dpr
    canvas.height = displayH * dpr
    canvas.style.width = displayW + 'px'
    canvas.style.height = displayH + 'px'
    ctx.scale(dpr, dpr)

    const W = displayW
    const maxVal = Math.max(...data.map(d => d.value))
    const labelW = 90
    const valueW = 60
    const chartW = W - labelW - valueW
    let time = 0
    let animId = 0

    function hexToRgb(hex: string) {
      const r = parseInt(hex.slice(1, 3), 16)
      const g = parseInt(hex.slice(3, 5), 16)
      const b = parseInt(hex.slice(5, 7), 16)
      return { r, g, b }
    }

    function drawBar(y: number, width: number, color: string, offset: number) {
      if (width < 2) return
      const rgb = hexToRgb(color)
      const x0 = labelW

      ctx!.save()
      ctx!.beginPath()
      ctx!.roundRect(x0, y, width, barH, [0, barH / 2, barH / 2, 0])
      ctx!.clip()

      ctx!.beginPath()
      const waveY = y + barH / 2
      for (let px = x0; px < x0 + width; px += 2) {
        const progress = (px - x0) / width
        const amp = 3 + progress * 2
        const wy = waveY + Math.sin(time * 0.03 + px * 0.04 + offset) * amp
        if (px === x0) ctx!.moveTo(px, wy)
        else ctx!.lineTo(px, wy)
      }
      ctx!.strokeStyle = `rgba(${rgb.r},${rgb.g},${rgb.b},0.7)`
      ctx!.lineWidth = 1.5
      ctx!.stroke()

      ctx!.beginPath()
      for (let px = x0; px < x0 + width; px += 2) {
        const progress = (px - x0) / width
        const amp = 2 + progress * 1.5
        const wy = waveY + Math.sin(time * 0.025 + px * 0.05 + offset + 2) * amp
        if (px === x0) ctx!.moveTo(px, wy)
        else ctx!.lineTo(px, wy)
      }
      ctx!.strokeStyle = `rgba(${rgb.r},${rgb.g},${rgb.b},0.35)`
      ctx!.lineWidth = 1
      ctx!.stroke()

      ctx!.restore()

      ctx!.beginPath()
      ctx!.roundRect(x0, y, width, barH, [0, barH / 2, barH / 2, 0])
      ctx!.strokeStyle = `rgba(${rgb.r},${rgb.g},${rgb.b},0.25)`
      ctx!.lineWidth = 1
      ctx!.stroke()
    }

    function animate() {
      time++
      ctx!.clearRect(0, 0, W, canvasH)

      data.forEach((d, i) => {
        const y = 5 + i * rowH
        const barWidth = maxVal > 0 ? (d.value / maxVal) * chartW : 0

        ctx!.fillStyle = '#6a9a5a'
        ctx!.font = '12px Inter, system-ui, sans-serif'
        ctx!.textAlign = 'right'
        ctx!.textBaseline = 'middle'
        ctx!.fillText(d.name, labelW - 12, y + barH / 2)

        drawBar(y, barWidth, d.color, i * 3)

        ctx!.fillStyle = '#5C7A52'
        ctx!.font = '11px Inter, system-ui, sans-serif'
        ctx!.textAlign = 'left'
        ctx!.fillText(`${d.value} min`, labelW + barWidth + 8, y + barH / 2)
      })

      animId = requestAnimationFrame(animate)
    }
    animate()

    return () => cancelAnimationFrame(animId)
  }, [data])

  if (data.length === 0) return null

  return (
    <div className="pond-card">
      <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
        Time saved by tool
      </p>
      <div ref={containerRef} style={{ height: canvasH }}>
        <canvas ref={canvasRef} />
      </div>
    </div>
  )
}
