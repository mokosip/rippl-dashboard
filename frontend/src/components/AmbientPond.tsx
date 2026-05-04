import { useEffect, useRef } from 'react'

interface Ripple {
  x: number
  y: number
  r: number
  maxR: number
  speed: number
  opacity: number
}

export function AmbientPond() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let w = 0
    let h = 0
    const ripples: Ripple[] = []
    let animId = 0

    function resize() {
      w = canvas!.width = window.innerWidth
      h = canvas!.height = window.innerHeight
    }
    resize()
    window.addEventListener('resize', resize)

    function addRipple() {
      ripples.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: 0,
        maxR: 80 + Math.random() * 120,
        speed: 0.3 + Math.random() * 0.4,
        opacity: 0.06 + Math.random() * 0.04,
      })
    }

    const interval = setInterval(addRipple, 3000)
    for (let i = 0; i < 4; i++) addRipple()

    function draw() {
      ctx!.clearRect(0, 0, w, h)
      for (let i = ripples.length - 1; i >= 0; i--) {
        const rip = ripples[i]
        rip.r += rip.speed
        const progress = rip.r / rip.maxR
        if (progress > 1) { ripples.splice(i, 1); continue }
        const alpha = rip.opacity * (1 - progress)
        ctx!.beginPath()
        ctx!.arc(rip.x, rip.y, rip.r, 0, Math.PI * 2)
        ctx!.strokeStyle = `rgba(92, 122, 82, ${alpha})`
        ctx!.lineWidth = 1
        ctx!.stroke()
        if (rip.r > 20) {
          ctx!.beginPath()
          ctx!.arc(rip.x, rip.y, rip.r * 0.6, 0, Math.PI * 2)
          ctx!.strokeStyle = `rgba(92, 122, 82, ${alpha * 0.6})`
          ctx!.stroke()
        }
      }
      animId = requestAnimationFrame(draw)
    }
    draw()

    return () => {
      window.removeEventListener('resize', resize)
      clearInterval(interval)
      cancelAnimationFrame(animId)
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      className="fixed inset-0 z-0 pointer-events-none"
      style={{ width: '100%', height: '100%' }}
    />
  )
}
