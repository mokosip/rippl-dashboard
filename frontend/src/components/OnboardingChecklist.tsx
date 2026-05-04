export function OnboardingChecklist() {
  return (
    <div className="max-w-md mx-auto mt-16 text-center">
      <h2 className="text-2xl font-bold font-serif text-fg">Welcome to rippl</h2>
      <p className="mt-2 text-fg-secondary">Let's get you set up</p>
      <div className="mt-8 space-y-4 text-left">
        <Step number={1} title="Connect a data source" description="Install the Chrome extension to start tracking your AI usage." />
        <Step number={2} title="Use AI for a day" description="Browse your favorite AI tools. We'll capture your sessions automatically." />
        <Step number={3} title="See your insights" description="Come back here to explore trends, time saved, and mirror moments." />
      </div>
      <a
        href="/settings"
        className="inline-block mt-8 px-6 py-2 rounded-xl text-sm bg-primary text-fg-on-primary"
      >
        Go to Settings to connect
      </a>
    </div>
  )
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0 bg-accent text-fg-accent">
        {number}
      </div>
      <div>
        <p className="font-medium text-fg">{title}</p>
        <p className="text-sm text-fg-secondary">{description}</p>
      </div>
    </div>
  )
}
