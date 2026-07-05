CREATE TABLE "search_species" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "search_species_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "species_requests" (
    "id" TEXT NOT NULL,
    "requestedName" TEXT NOT NULL,
    "normalizedName" TEXT NOT NULL,
    "animalId" TEXT NOT NULL,
    "shelterId" TEXT NOT NULL,
    "requestedById" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "rejectionReason" TEXT,
    "reviewedById" TEXT,
    "reviewedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "species_requests_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "search_species_slug_key" ON "search_species"("slug");
CREATE UNIQUE INDEX "species_requests_animalId_key" ON "species_requests"("animalId");
CREATE INDEX "species_requests_status_createdAt_idx" ON "species_requests"("status", "createdAt");

INSERT INTO "search_species" (
    "id",
    "name",
    "slug",
    "sortOrder",
    "updatedAt"
) VALUES
    (gen_random_uuid()::text, 'Perro', 'perro', 10, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Gato', 'gato', 20, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Conejo', 'conejo', 30, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Ave', 'ave', 40, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Loro', 'loro', 50, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Hamster', 'hamster', 60, CURRENT_TIMESTAMP);
