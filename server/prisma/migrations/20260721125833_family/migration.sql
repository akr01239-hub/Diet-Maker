-- CreateTable
CREATE TABLE "family_members" (
    "id" TEXT NOT NULL,
    "ownerId" TEXT NOT NULL,
    "firstName" TEXT NOT NULL,
    "relation" TEXT,
    "heightCm" DOUBLE PRECISION NOT NULL,
    "activityLevel" TEXT NOT NULL DEFAULT 'moderate',
    "goal" TEXT NOT NULL DEFAULT 'maintain',
    "dietType" TEXT NOT NULL DEFAULT 'nonveg',
    "reducedMobility" BOOLEAN NOT NULL DEFAULT false,
    "sensitiveEnc" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "family_members_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "family_members_ownerId_idx" ON "family_members"("ownerId");

-- AddForeignKey
ALTER TABLE "family_members" ADD CONSTRAINT "family_members_ownerId_fkey" FOREIGN KEY ("ownerId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
