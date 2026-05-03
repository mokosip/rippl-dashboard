import type { MirrorMoment } from '../types'

const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '\u{1F4C8}',
  top_tool: '\u{1F3AF}',
  time_saving_activity: '⏱️',
  busiest_day: '\u{1F4C5}',
}

export function MirrorMomentCard({ moment }: { moment: MirrorMoment }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <span className="text-2xl">{TYPE_ICONS[moment.type] ?? '\u{1F4A1}'}</span>
      <p className="text-gray-700 mt-2 text-sm">{moment.message}</p>
    </div>
  )
}
