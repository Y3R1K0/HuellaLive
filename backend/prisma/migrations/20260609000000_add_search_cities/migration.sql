CREATE TABLE "search_cities" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "region" TEXT,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "search_cities_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "search_cities_slug_key" ON "search_cities"("slug");

INSERT INTO "search_cities" (
    "id",
    "name",
    "slug",
    "region",
    "sortOrder",
    "updatedAt"
) VALUES
    (gen_random_uuid()::text, 'Arequipa', 'arequipa', 'Arequipa', 10, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Lima', 'lima', 'Lima', 20, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Cusco', 'cusco', 'Cusco', 30, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Trujillo', 'trujillo', 'La Libertad', 40, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Chiclayo', 'chiclayo', 'Lambayeque', 50, CURRENT_TIMESTAMP),
    (gen_random_uuid()::text, 'Piura', 'piura', 'Piura', 60, CURRENT_TIMESTAMP);
