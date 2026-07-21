import { describe, it, expect } from 'vitest';
import { getAiProvider } from '../src/ai';
import { groqProvider } from '../src/ai/groq';
import { geminiProvider } from '../src/ai/gemini';
import { buildSystemPrompt, type AiChatContext } from '../src/ai/provider';

// setup.ts sets AI_PROVIDER=rules and leaves GROQ_API_KEY / GEMINI_API_KEY unset,
// so these tests exercise the default/no-key paths without any network access.

const ctx: AiChatContext = {
  targets: { dailyKcal: 2000, proteinG: 130, waterMl: 2600 },
  conditions: ['diabetes'],
  firstName: 'Sam',
};

describe('getAiProvider', () => {
  it('returns null when AI_PROVIDER=rules (the default)', () => {
    expect(getAiProvider()).toBeNull();
  });
});

describe('provider graceful fallback with no API key', () => {
  it('groq chatReply resolves to null when the key is absent (no throw)', async () => {
    await expect(groqProvider.chatReply('how much protein?', ctx)).resolves.toBeNull();
  });

  it('gemini chatReply resolves to null when the key is absent (no throw)', async () => {
    await expect(geminiProvider.chatReply('how much protein?', ctx)).resolves.toBeNull();
  });
});

describe('buildSystemPrompt', () => {
  it('embeds the provided targets and conditions, and forbids inventing numbers', () => {
    const p = buildSystemPrompt(ctx);
    expect(p).toContain('2000');
    expect(p).toContain('130');
    expect(p).toContain('diabetes');
    expect(p).toMatch(/NEVER invent/i);
  });

  it('avoids stating numbers when targets are unknown', () => {
    const p = buildSystemPrompt({ targets: null, conditions: [] });
    expect(p).not.toContain('kcal, ');
    expect(p.toLowerCase()).toContain('complete their profile');
  });
});
