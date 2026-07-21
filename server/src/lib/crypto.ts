import {
  createCipheriv,
  createDecipheriv,
  createHash,
  randomBytes,
} from 'node:crypto';
import { env } from './env';

/**
 * AES-256-GCM encryption for sensitive health data at the application layer.
 * Key comes from HEALTH_DATA_ENCRYPTION_KEY (base64, 32 bytes).
 * Output format: base64(iv).base64(authTag).base64(ciphertext)
 */
const ALGO = 'aes-256-gcm';
const IV_BYTES = 12;

function getKey(): Buffer {
  const key = Buffer.from(env.HEALTH_DATA_ENCRYPTION_KEY, 'base64');
  if (key.length !== 32) {
    throw new Error('HEALTH_DATA_ENCRYPTION_KEY must decode to 32 bytes (base64).');
  }
  return key;
}

export function encryptJson(value: unknown): string {
  const iv = randomBytes(IV_BYTES);
  const cipher = createCipheriv(ALGO, getKey(), iv);
  const plaintext = Buffer.from(JSON.stringify(value), 'utf8');
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const tag = cipher.getAuthTag();
  return [
    iv.toString('base64'),
    tag.toString('base64'),
    ciphertext.toString('base64'),
  ].join('.');
}

export function decryptJson<T = unknown>(payload: string): T {
  const parts = payload.split('.');
  if (parts.length !== 3) throw new Error('Malformed ciphertext');
  const [ivB64, tagB64, dataB64] = parts as [string, string, string];
  const decipher = createDecipheriv(ALGO, getKey(), Buffer.from(ivB64, 'base64'));
  decipher.setAuthTag(Buffer.from(tagB64, 'base64'));
  const plaintext = Buffer.concat([
    decipher.update(Buffer.from(dataB64, 'base64')),
    decipher.final(),
  ]);
  return JSON.parse(plaintext.toString('utf8')) as T;
}

/** SHA-256 hex — used to store refresh tokens without keeping the raw value. */
export function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

/** Cryptographically-random opaque token (URL-safe). */
export function randomToken(bytes = 48): string {
  return randomBytes(bytes).toString('base64url');
}
