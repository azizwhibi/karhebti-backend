const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function print() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');
    const M = mongoose.model('Maintenance', MaintenanceSchema);
    const docs = await M.find({}).limit(10).lean().exec();
    docs.forEach((d,i)=>{
      console.log('---');
      console.log('id:', d._id.toString());
      console.log('title:', d.title);
      console.log('date:', d.date);
      console.log('dueAt:', d.dueAt);
      console.log('createdAt:', d.createdAt);
      console.log('updatedAt:', d.updatedAt);
      const displayDate = d.date || d.dueAt || d.createdAt;
      console.log('displayDate(calc):', displayDate);
    });
    await mongoose.disconnect();
  } catch(e) { console.error(e); }
}
print();
