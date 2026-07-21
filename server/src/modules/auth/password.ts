import { hash, verify, Algorithm } from '@node-rs/argon2';

// Argon2id with sensible interactive-login parameters.
const OPTS = {
  algorithm: Algorithm.Argon2id,
  memoryCost: 19456, // 19 MiB
  timeCost: 2,
  parallelism: 1,
};

export function hashPassword(plain: string): Promise<string> {
  return hash(plain, OPTS);
}

export async function verifyPassword(hashStr: string, plain: string): Promise<boolean> {
  try {
    return await verify(hashStr, plain, OPTS);
  } catch {
    return false;
  }
}
