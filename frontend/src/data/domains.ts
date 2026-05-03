export interface DomainInfo {
  name: string
  icon: string
  color: string
}

export const DOMAINS: Record<string, DomainInfo> = {
  'claude.ai': { name: 'Claude', icon: '\u{1F916}', color: '#D4A574' },
  'chatgpt.com': { name: 'ChatGPT', icon: '\u{1F4AC}', color: '#10A37F' },
  'chat.openai.com': { name: 'ChatGPT', icon: '\u{1F4AC}', color: '#10A37F' },
  'gemini.google.com': { name: 'Gemini', icon: '✨', color: '#4285F4' },
  'copilot.microsoft.com': { name: 'Copilot', icon: '\u{1F535}', color: '#0078D4' },
  'perplexity.ai': { name: 'Perplexity', icon: '\u{1F50D}', color: '#20808D' },
  'you.com': { name: 'You.com', icon: '\u{1F7E3}', color: '#7B61FF' },
  'poe.com': { name: 'Poe', icon: '⚡', color: '#5856D6' },
  'phind.com': { name: 'Phind', icon: '\u{1F50E}', color: '#3B82F6' },
  'huggingface.co': { name: 'Hugging Face', icon: '\u{1F917}', color: '#FFD21E' },
  'pi.ai': { name: 'Pi', icon: '\u{1F967}', color: '#E85D04' },
  'meta.ai': { name: 'Meta AI', icon: '\u{1F537}', color: '#0668E1' },
  'mistral.ai': { name: 'Mistral', icon: '\u{1F32A}️', color: '#F7D046' },
  'deepseek.com': { name: 'DeepSeek', icon: '\u{1F30A}', color: '#4F46E5' },
  'grok.x.ai': { name: 'Grok', icon: '\u{1F47E}', color: '#1DA1F2' },
}

export function getDomain(domain: string): DomainInfo {
  return DOMAINS[domain] ?? { name: domain, icon: '\u{1F310}', color: '#6B7280' }
}
