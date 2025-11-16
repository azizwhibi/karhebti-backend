const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function inspect() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');
    const Maintenance = mongoose.model('Maintenance', MaintenanceSchema);
    const now = new Date();
    const day = 24 * 60 * 60 * 1000;

    const docs = await Maintenance.find({ dueAt: { $gte: new Date(now.getTime() - day), $lte: new Date(now.getTime() + day) } }).lean().exec();
    console.log('Found', docs.length);
    docs.forEach((d, i) => console.log(i + 1, {
      id: d._id,
      date: d.date,
      dueAt: d.dueAt,
      updatedAt: d.updatedAt,
      createdAt: d.createdAt,
      status: d.status
    }));

    await mongoose.disconnect();
  } catch (err) {
    console.error('Error:', err);
  }
}

inspect();
