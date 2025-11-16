const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function countDiff() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');
    const M = mongoose.model('Maintenance', MaintenanceSchema);
    const total = await M.countDocuments();
    const diff = await M.countDocuments({ $expr: { $ne: ['$dueAt', '$date'] } });
    console.log('Total maintenances:', total);
    console.log('Entries where dueAt != date:', diff);
    await mongoose.disconnect();
  } catch (e) { console.error(e); }
}
countDiff();
