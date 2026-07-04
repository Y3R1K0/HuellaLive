CREATE TABLE "shelter_stories" (
    "id" TEXT NOT NULL,
    "videoUrl" TEXT NOT NULL,
    "thumbnailUrl" TEXT,
    "description" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "shelterId" TEXT NOT NULL,
    "uploadedById" TEXT NOT NULL,

    CONSTRAINT "shelter_stories_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "shelter_stories_shelterId_createdAt_idx" ON "shelter_stories"("shelterId", "createdAt");

ALTER TABLE "shelter_stories" ADD CONSTRAINT "shelter_stories_shelterId_fkey" FOREIGN KEY ("shelterId") REFERENCES "shelter_profiles"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "shelter_stories" ADD CONSTRAINT "shelter_stories_uploadedById_fkey" FOREIGN KEY ("uploadedById") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
