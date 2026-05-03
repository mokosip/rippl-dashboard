import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '📈',
  top_tool: '🎯',
  time_saving_activity: '⏱️',
  busiest_day: '📅',
}

export function MirrorMomentCard({ moment }: { moment: MirrorMoment }) {
  return (
    <div className="bg-card rounded-card shadow-sm p-4">
      <span className="text-2xl">{TYPE_ICONS[moment.type] ?? '💡'}</span>
      <p className="text-fg-secondary mt-2 text-sm">{moment.message}</p>
    </div>
  )
}
