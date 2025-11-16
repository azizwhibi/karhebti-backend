const mongoose = require('mongoose');
const { MaintenanceSchema } = require('./dist/maintenances/schemas/maintenance.schema');

async function updateExistingMaintenances() {
  try {
    await mongoose.connect('mongodb://localhost:27017/karhebti');

    // Register the model
    const Maintenance = mongoose.model('Maintenance', MaintenanceSchema);

    // Update all existing maintenances to have default values for new fields
    const result = await Maintenance.updateMany(
      {}, // All documents
      {
        $set: {
          title: 'Entretien', // Default title
          notes: '', // Empty notes
          tags: [], // Empty tags array
          status: 'done', // Assume existing ones are done
          dueAt: new Date(), // Current date as due date
          mileage: 0, // Default mileage
          ownerId: '690a56629d075ab83170b80f' // The user's ID
        }
      },
      { upsert: false }
    );

    console.log('Updated', result.modifiedCount, 'maintenances');

    // Verify one document
    const sample = await Maintenance.findOne().lean();
    if (sample) {
      console.log('Sample document has new fields:', {
        title: sample.title,
        notes: sample.notes,
        tags: sample.tags,
        status: sample.status,
        dueAt: sample.dueAt,
        mileage: sample.mileage,
        ownerId: sample.ownerId
      });
    }

  } catch (error) {
    console.error('Error:', error);
  } finally {
    await mongoose.disconnect();
  }
}

updateExistingMaintenances();