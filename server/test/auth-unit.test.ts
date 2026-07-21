import { describe, it, expect } from 'vitest';
import { signAccessToken, verifyAccessToken } from '../src/modules/auth/tokens';
import { hashPassword, verifyPassword } from '../src/modules/auth/password';
import { ageFromDob } from '../src/modules/nutrition/calc.service';

describe('access tokens', () => {
  it('signs and verifies claims', () => {
    const token = signAccessToken({ sub: 'user-123', role: 'user' });
    const claims = verifyAccessToken(token);
    expect(claims.sub).toBe('user-123');
    expect(claims.role).toBe('user');
  });

  it('rejects a garbage token', () => {
    expect(() => verifyAccessToken('nonsense.token.here')).toThrow();
  });
});

describe('password hashing (argon2id)', () => {
  it('verifies a correct password and rejects a wrong one', async () => {
    const hash = await hashPassword('correct horse battery staple');
    expect(await verifyPassword(hash, 'correct horse battery staple')).toBe(true);
    expect(await verifyPassword(hash, 'wrong password')).toBe(false);
  });

  it('does not throw on a malformed hash', async () => {
    expect(await verifyPassword('not-a-hash', 'x')).toBe(false);
  });
});

describe('ageFromDob', () => {
  it('computes whole years, respecting the birthday', () => {
    const now = new Date('2026-07-21T00:00:00Z');
    expect(ageFromDob('2000-01-01', now)).toBe(26);
    expect(ageFromDob('2000-12-31', now)).toBe(25); // birthday not yet reached
    expect(ageFromDob('2026-07-21', now)).toBe(0);
  });
});
