-- AlterTable: estimated calories burned on a logged exercise (server-computed, MET-based)
ALTER TABLE "exercise_logs" ADD COLUMN "kcal" INTEGER;

-- CreateTable
CREATE TABLE "wellness_sessions" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "refId" TEXT NOT NULL,
    "refName" TEXT NOT NULL,
    "durationMin" INTEGER NOT NULL,
    "kcal" INTEGER NOT NULL DEFAULT 0,
    "completedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "wellness_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "wellness_sessions_userId_completedAt_idx" ON "wellness_sessions"("userId", "completedAt");

-- AddForeignKey
ALTER TABLE "wellness_sessions" ADD CONSTRAINT "wellness_sessions_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
