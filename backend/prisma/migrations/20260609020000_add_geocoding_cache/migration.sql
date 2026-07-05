CREATE TABLE "geocoding_cache" (
    "id" TEXT NOT NULL,
    "cacheKey" TEXT NOT NULL,
    "kind" TEXT NOT NULL,
    "payload" JSONB NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "geocoding_cache_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "geocoding_cache_cacheKey_key" ON "geocoding_cache"("cacheKey");
CREATE INDEX "geocoding_cache_expiresAt_idx" ON "geocoding_cache"("expiresAt");
