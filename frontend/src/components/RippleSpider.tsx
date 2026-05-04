import { useEffect, useRef, useState } from 'react'

interface SpiderData {
  name: string
  value: number
}

export function RippleSpider({ data }: { data: SpiderData[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [hovered, setHovered] = useState<number>(-1)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || data.length < 2) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const W = canvas.width
    const H = canvas.height
    const cx = W / 2
    const cy = H / 2
    const maxR = Math.min(cx, cy) - 80
    const maxVal = Math.max(...data.map(d => d.value), 1)
    const n = data.length
    let time = 0
    let animId = 0

    function getAngle(i: number) {
      return (Math.PI * 2 * i) / n - Math.PI / 2
    }

    function getPoint(i: number, r: number) {
      const angle = getAngle(i)
      return {
        x: cx + Math.cos(angle) * r,
        y: cy + Math.sin(angle) * r,
      }
    }

    function drawRippleRings() {
      const levels = 4
      for (let l = 1; l <= levels; l++) {
        const r = (maxR / levels) * l
        const wobble = Math.sin(time * 0.015 + l * 1.2) * 1.5
        ctx!.beginPath()
        const steps = 60
        for (let s = 0; s <= steps; s++) {
          const angle = (Math.PI * 2 * s) / steps
          const localWobble = Math.sin(angle * 3 + time * 0.02 + l) * wobble
          const px = cx + Math.cos(angle) * (r + localWobble)
          const py = cy + Math.sin(angle) * (r + localWobble)
          if (s === 0) ctx!.moveTo(px, py)
          else ctx!.lineTo(px, py)
        }
        ctx!.closePath()
        ctx!.strokeStyle = `rgba(92,122,82,${0.06 + l * 0.02})`
        ctx!.lineWidth = 1
        ctx!.stroke()
      }
    }

    function drawAxes() {
      for (let i = 0; i < n; i++) {
        const p = getPoint(i, maxR)
        ctx!.beginPath()
        ctx!.moveTo(cx, cy)
        ctx!.lineTo(p.x, p.y)
        ctx!.strokeStyle = 'rgba(92,122,82,0.1)'
        ctx!.lineWidth = 0.5
        ctx!.stroke()

        const labelR = maxR + 20
        const lp = getPoint(i, labelR)
        const angle = getAngle(i)
        const cosA = Math.cos(angle)

        let align: CanvasTextAlign = 'center'
        let offsetX = 0
        if (cosA > 0.3) { align = 'left'; offsetX = 4 }
        else if (cosA < -0.3) { align = 'right'; offsetX = -4 }

        ctx!.fillStyle = i === hovered ? '#8fb87a' : '#6a9a5a'
        ctx!.font = `${i === hovered ? '600 ' : ''}12px Inter, system-ui, sans-serif`
        ctx!.textAlign = align
        ctx!.textBaseline = 'middle'

        const label = i === hovered ? `${data[i].name} — ${data[i].value} min` : data[i].name
        ctx!.fillText(label, lp.x + offsetX, lp.y)
      }
    }

    function drawDataArea() {
      ctx!.beginPath()
      for (let i = 0; i < n; i++) {
        const r = (data[i].value / maxVal) * maxR
        const wobble = Math.sin(time * 0.02 + i * 1.5) * 2
        const p = getPoint(i, r + wobble)
        if (i === 0) ctx!.moveTo(p.x, p.y)
        else ctx!.lineTo(p.x, p.y)
      }
      ctx!.closePath()

      const grad = ctx!.createRadialGradient(cx, cy, 0, cx, cy, maxR)
      grad.addColorStop(0, 'rgba(92,122,82,0.2)')
      grad.addColorStop(1, 'rgba(92,122,82,0.05)')
      ctx!.fillStyle = grad
      ctx!.fill()

      ctx!.strokeStyle = 'rgba(143,184,122,0.6)'
      ctx!.lineWidth = 2
      ctx!.stroke()

      for (let i = 0; i < n; i++) {
        const r = (data[i].value / maxVal) * maxR
        const wobble = Math.sin(time * 0.02 + i * 1.5) * 2
        const p = getPoint(i, r + wobble)
        ctx!.beginPath()
        ctx!.arc(p.x, p.y, i === hovered ? 5 : 3.5, 0, Math.PI * 2)
        ctx!.fillStyle = i === hovered ? '#8fb87a' : 'rgba(143,184,122,0.9)'
        ctx!.fill()

        if (i === hovered) {
          ctx!.beginPath()
          ctx!.arc(p.x, p.y, 10, 0, Math.PI * 2)
          ctx!.strokeStyle = 'rgba(143,184,122,0.3)'
          ctx!.lineWidth = 1
          ctx!.stroke()
        }
      }
    }

    function animate() {
      time++
      ctx!.clearRect(0, 0, W, H)
      drawRippleRings()
      drawAxes()
      drawDataArea()
      animId = requestAnimationFrame(animate)
    }
    animate()

    return () => cancelAnimationFrame(animId)
  }, [data, hovered])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || data.length < 2) return

    const W = canvas.width
    const H = canvas.height
    const cx = W / 2
    const cy = H / 2
    const maxR = Math.min(cx, cy) - 80
    const maxVal = Math.max(...data.map(d => d.value), 1)
    const n = data.length

    function handleMouseMove(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const mx = (e.clientX - rect.left) * (W / rect.width)
      const my = (e.clientY - rect.top) * (H / rect.height)

      let closest = -1
      let closestDist = 30

      for (let i = 0; i < n; i++) {
        const angle = (Math.PI * 2 * i) / n - Math.PI / 2
        const r = (data[i].value / maxVal) * maxR
        const px = cx + Math.cos(angle) * r
        const py = cy + Math.sin(angle) * r
        const dist = Math.sqrt((mx - px) ** 2 + (my - py) ** 2)
        if (dist < closestDist) {
          closestDist = dist
          closest = i
        }
      }
      setHovered(closest)
    }

    function handleMouseLeave() {
      setHovered(-1)
    }

    canvas.addEventListener('mousemove', handleMouseMove)
    canvas.addEventListener('mouseleave', handleMouseLeave)
    return () => {
      canvas.removeEventListener('mousemove', handleMouseMove)
      canvas.removeEventListener('mouseleave', handleMouseLeave)
    }
  }, [data])

  if (data.length < 2) return null

  return (
    <div className="pond-card">
      <p className="text-xs uppercase tracking-widest mb-4" style={{ color: '#5C7A52', letterSpacing: '1px' }}>
        What you use AI for
      </p>
      <div className="flex justify-center">
        <canvas ref={canvasRef} width={500} height={400} style={{ width: '100%', maxWidth: 500, height: 'auto', aspectRatio: '500 / 400' }} />
      </div>
    </div>
  )
}
