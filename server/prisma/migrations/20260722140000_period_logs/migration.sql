-- CreateTable
CREATE TABLE "period_logs" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "startDate" TIMESTAMP(3) NOT NULL,
    "endDate" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "period_logs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "period_logs_userId_startDate_idx" ON "period_logs"("userId", "startDate");

-- AddForeignKey
ALTER TABLE "period_logs" ADD CONSTRAINT "period_logs_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
