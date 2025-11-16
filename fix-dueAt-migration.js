const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function fixDueAt() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');

    const Maintenance = mongoose.model('Maintenance', MaintenanceSchema);

    // More robust approach:
    // Find documents where `date` exists and `dueAt` differs from `date`,
    // and the document was updated recently (likely by the previous migration).
    // We'll consider documents updated within the last 14 days and with dueAt within the
    // same window as candidates to correct.
    const now = new Date();
    const windowMs = 14 * 24 * 60 * 60 * 1000; // 14 days
    const since = new Date(now.getTime() - windowMs);

    const candidates = await Maintenance.find({
      date: { $exists: true },
      $expr: { $ne: ['$dueAt', '$date'] },
      updatedAt: { $gte: since },
      dueAt: { $gte: new Date(now.getTime() - windowMs), $lte: new Date(now.getTime() + windowMs) }
    }).exec();

    console.log('Candidates found (recently updated, dueAt near now):', candidates.length);

    let updated = 0;
    for (const doc of candidates) {
      if (doc.date) {
        doc.dueAt = doc.date;
        await doc.save();
        updated++;
      }
    }

    console.log('Updated dueAt for', updated, 'documents');

  } catch (err) {
    console.error('Error:', err);
  } finally {
    await mongoose.disconnect();
  }
}

fixDueAt();
