import { env } from '../lib/env';

const MODEL = 'gemini-2.0-flash';
const TIMEOUT_MS = 20_000;

/**
 * Sends an image + prompt to Gemini and returns parsed JSON, or null on any problem
 * (missing key, timeout, non-2xx, unparsable). Never throws. The image is NOT persisted —
 * it is forwarded to Gemini for this single request only.
 */
export async function geminiVisionJson(
  imageBase64: string,
  mimeType: string,
  prompt: string,
): Promise<Record<string, unknown> | null> {
  const key = env.GEMINI_API_KEY;
  if (!key) return null;

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${key}`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            role: 'user',
            parts: [
              { text: prompt },
              { inline_data: { mime_type: mimeType, data: imageBase64 } },
            ],
          },
        ],
        generationConfig: { temperature: 0.2, maxOutputTokens: 500, responseMimeType: 'application/json' },
      }),
      signal: controller.signal,
    });
    if (!res.ok) return null;
    const json = (await res.json()) as {
      candidates?: { content?: { parts?: { text?: string }[] } }[];
    };
    const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) return null;
    return parseJsonLoose(text);
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

/** Parse JSON that may be wrapped in ```json fences or have surrounding prose. */
function parseJsonLoose(text: string): Record<string, unknown> | null {
  const cleaned = text.replace(/```json/gi, '').replace(/```/g, '').trim();
  try {
    return JSON.parse(cleaned) as Record<string, unknown>;
  } catch {
    const start = cleaned.indexOf('{');
    const end = cleaned.lastIndexOf('}');
    if (start >= 0 && end > start) {
      try {
        return JSON.parse(cleaned.slice(start, end + 1)) as Record<string, unknown>;
      } catch {
        return null;
      }
    }
    return null;
  }
}

export const visionAvailable = (): boolean => Boolean(env.GEMINI_API_KEY);
