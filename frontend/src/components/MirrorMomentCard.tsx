import { motion } from 'framer-motion'
import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '📈',
  top_tool: '🎯',
  busiest_day: '📅',
}

export function MirrorMomentCard({ moment, index = 0 }: { moment: MirrorMoment; index?: number }) {
  return (
    <motion.div
      className="pond-card overflow-hidden"
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.6, delay: index * 0.15, ease: [0.4, 0, 0.2, 1] }}
    >
      <div className="absolute inset-0 pointer-events-none">
        <motion.div
          className="absolute rounded-full"
          style={{
            top: '50%', left: '50%',
            background: 'radial-gradient(circle, rgba(92,122,82,0.15), transparent 70%)',
          }}
          initial={{ width: 0, height: 0, x: 0, y: 0 }}
          animate={{ width: 400, height: 400, x: -200, y: -200 }}
          transition={{ duration: 1.5, delay: index * 0.15, ease: 'easeOut' }}
        />
      </div>
      <span className="text-xl relative z-10">{TYPE_ICONS[moment.type] ?? '💡'}</span>
      <p className="text-sm mt-2 leading-relaxed relative z-10 text-fg">
        {moment.message}
      </p>
    </motion.div>
  )
}
