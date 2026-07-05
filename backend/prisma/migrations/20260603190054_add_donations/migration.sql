-- AddForeignKey
ALTER TABLE "donations" ADD CONSTRAINT "donations_shelterId_fkey" FOREIGN KEY ("shelterId") REFERENCES "shelter_profiles"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
