import { useEffect } from 'react'

export function ClickRipple() {
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      const container = document.createElement('div')
      container.style.cssText = `position:fixed;left:${e.clientX}px;top:${e.clientY}px;pointer-events:none;z-index:9999;`
      for (let i = 0; i < 3; i++) {
        const ring = document.createElement('div')
        ring.style.cssText = `
          position:absolute;border-radius:50%;
          border:${1.5 - i * 0.4}px solid rgba(92,122,82,${0.5 - i * 0.15});
          animation:clickRippleAnim ${0.8 + i * 0.3}s ease-out forwards;
        `
        container.appendChild(ring)
      }
      document.body.appendChild(container)
      setTimeout(() => container.remove(), 1500)
    }

    document.addEventListener('click', handleClick)
    return () => document.removeEventListener('click', handleClick)
  }, [])

  return null
}
