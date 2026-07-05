ALTER TABLE "donations" ALTER COLUMN "currency" SET DEFAULT 'HUELLITAS';

ALTER TABLE "donations"
ADD CONSTRAINT "donations_shelterId_fkey"
FOREIGN KEY ("shelterId") REFERENCES "shelter_profiles"("id")
ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE TABLE "shelter_withdrawals" (
    "id" TEXT NOT NULL,
    "shelterId" TEXT NOT NULL,
    "amount" DOUBLE PRECISION NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "accountHolder" TEXT NOT NULL,
    "documentNumber" TEXT NOT NULL,
    "bankName" TEXT NOT NULL,
    "accountType" TEXT NOT NULL,
    "accountNumber" TEXT NOT NULL,
    "cci" TEXT NOT NULL,
    "currency" TEXT NOT NULL DEFAULT 'PEN',
    "receiptUrl" TEXT,
    "rejectionReason" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "processedAt" TIMESTAMP(3),
    CONSTRAINT "shelter_withdrawals_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "shelter_withdrawals_shelterId_status_idx"
ON "shelter_withdrawals"("shelterId", "status");

ALTER TABLE "shelter_withdrawals"
ADD CONSTRAINT "shelter_withdrawals_shelterId_fkey"
FOREIGN KEY ("shelterId") REFERENCES "shelter_profiles"("id")
ON DELETE RESTRICT ON UPDATE CASCADE;
