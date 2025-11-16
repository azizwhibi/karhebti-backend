const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function fixStatus() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');
    const Maintenance = mongoose.model('Maintenance', MaintenanceSchema);

    const docs = await Maintenance.find({ date: { $exists: true } }).exec();
    let changed = 0;
    const now = new Date();
    for (const d of docs) {
      const desired = (d.date && new Date(d.date) > now) ? 'planned' : 'done';
      if (d.status !== desired) {
        d.status = desired;
        await d.save();
        changed++;
      }
    }

    console.log('Updated status for', changed, 'documents');
    await mongoose.disconnect();
  } catch (e) {
    console.error('Error:', e);
  }
}

fixStatus();
