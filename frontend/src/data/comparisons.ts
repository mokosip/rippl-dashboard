interface Comparison {
  threshold: number
  text: string
}

const TIME_COMPARISONS: Comparison[] = [
  { threshold: 5, text: 'enough to brew and enjoy a cup of coffee' },
  { threshold: 15, text: 'enough to take a proper walk around the block' },
  { threshold: 30, text: 'enough to cook a meal from scratch' },
  { threshold: 60, text: 'enough to watch an episode of your favorite show' },
  { threshold: 120, text: 'enough to watch a full movie' },
  { threshold: 300, text: 'enough to run a half marathon' },
  { threshold: 480, text: 'enough to read a novel cover to cover' },
  { threshold: 1200, text: 'enough to binge an entire season of a TV show' },
  { threshold: 2400, text: 'enough to read War and Peace' },
  { threshold: 4800, text: 'enough to run a marathon... twice' },
]

export function getComparison(minutes: number): string {
  let best = TIME_COMPARISONS[0]
  for (const c of TIME_COMPARISONS) {
    if (minutes >= c.threshold) best = c
    else break
  }
  return best.text
}
