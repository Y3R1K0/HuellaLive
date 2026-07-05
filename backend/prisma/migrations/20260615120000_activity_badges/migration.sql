ALTER TABLE "users" ADD COLUMN "selectedBadgeId" TEXT;

ALTER TABLE "badges" ADD COLUMN "key" TEXT;
ALTER TABLE "badges" ADD COLUMN "activity" TEXT;
ALTER TABLE "badges" ADD COLUMN "threshold" INTEGER;

UPDATE "badges"
SET
  "key" = CONCAT('LEGACY_', "id"),
  "activity" = 'LEGACY',
  "threshold" = 0
WHERE "key" IS NULL;

ALTER TABLE "badges" ALTER COLUMN "key" SET NOT NULL;
ALTER TABLE "badges" ALTER COLUMN "activity" SET NOT NULL;
ALTER TABLE "badges" ALTER COLUMN "threshold" SET NOT NULL;

CREATE UNIQUE INDEX "badges_key_key" ON "badges"("key");
