import { env } from '../lib/env';
import type { AiProvider } from './provider';
import { groqProvider } from './groq';
import { geminiProvider } from './gemini';

export type { AiProvider, AiChatContext } from './provider';

/**
 * Resolves the configured chat-AI provider, or `null` to use the deterministic rules
 * engine only. Ollama is out of scope for now and returns `null`.
 */
export function getAiProvider(): AiProvider | null {
  switch (env.AI_PROVIDER) {
    case 'groq':
      return groqProvider;
    case 'gemini':
      return geminiProvider;
    default:
      return null;
  }
}
