// Runs before any test module is imported. Provides placeholder env so `src/lib/env.ts`
// validation passes. These are NOT real secrets and never touch a real database — unit
// tests only exercise routes that don't require DB access.
process.env.NODE_ENV = 'test';
process.env.DATABASE_URL ??= 'postgresql://user:pw@localhost:5432/nutriai';
process.env.DIRECT_URL ??= 'postgresql://user:pw@localhost:5432/nutriai';
process.env.JWT_ACCESS_SECRET ??= 'test_access_secret_test_access_secret_0123456789';
process.env.JWT_REFRESH_SECRET ??= 'test_refresh_secret_test_refresh_secret_0123456789';
process.env.HEALTH_DATA_ENCRYPTION_KEY ??= 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=';
process.env.AI_PROVIDER ??= 'rules';
