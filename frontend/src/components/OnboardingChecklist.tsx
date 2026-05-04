export function OnboardingChecklist() {
  return (
    <div className="max-w-md mx-auto mt-16 text-center">
      <h2 className="text-2xl font-bold font-serif" style={{ color: '#c8dfc0' }}>Welcome to rippl</h2>
      <p className="mt-2" style={{ color: '#6a9a5a' }}>Let's get you set up</p>
      <div className="mt-8 space-y-4 text-left">
        <Step number={1} title="Connect a data source" description="Install the Chrome extension to start tracking your AI usage." />
        <Step number={2} title="Use AI for a day" description="Browse your favorite AI tools. We'll capture your sessions automatically." />
        <Step number={3} title="See your insights" description="Come back here to explore trends, time saved, and mirror moments." />
      </div>
      <a
        href="/settings"
        className="inline-block mt-8 px-6 py-2 rounded-xl text-sm"
        style={{ background: '#5C7A52', color: '#0f1a0f' }}
      >
        Go to Settings to connect
      </a>
    </div>
  )
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0"
        style={{ background: 'rgba(92,122,82,0.15)', color: '#8fb87a' }}>
        {number}
      </div>
      <div>
        <p className="font-medium" style={{ color: '#c8dfc0' }}>{title}</p>
        <p className="text-sm" style={{ color: '#6a9a5a' }}>{description}</p>
      </div>
    </div>
  )
}
