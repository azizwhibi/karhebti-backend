// Quick Backend Test Script to Verify Garage Owner Setup
// Run this in your backend project directory

const mongoose = require('mongoose');

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/karhebti', {
  useNewUrlParser: true,
  useUnifiedTopology: true,
});

async function checkGarageOwnerSetup() {
  console.log('🔍 Checking Garage Owner Setup...\n');

  try {
    // Check if garage owner exists
    const User = mongoose.model('User');
    const garageOwner = await User.findById('6932f6f96551fb27afecc516');

    if (!garageOwner) {
      console.error('❌ Garage owner not found with ID: 6932f6f96551fb27afecc516');
      process.exit(1);
    }

    console.log('✅ Garage Owner Found:');
    console.log(`   Email: ${garageOwner.email}`);
    console.log(`   Role: ${garageOwner.role}`);
    console.log(`   Email Verified: ${garageOwner.emailVerified}`);
    console.log(`   FCM Token: ${garageOwner.fcmToken ? '✅ EXISTS' : '❌ MISSING'}`);

    if (garageOwner.fcmToken) {
      console.log(`   Token Preview: ${garageOwner.fcmToken.substring(0, 20)}...`);
    } else {
      console.log('\n⚠️  WARNING: FCM Token is missing!');
      console.log('   The garage owner needs to:');
      console.log('   1. Open the garage Android app');
      console.log('   2. Log in with: prop.garage@example.com');
      console.log('   3. App will automatically register FCM token');
      console.log('   4. Token will be sent to backend on login');
    }

    console.log('\n---\n');

    // Check all propGarage users
    const allGarageOwners = await User.find({ role: 'propGarage', emailVerified: true });
    console.log(`📊 Total Verified Garage Owners: ${allGarageOwners.length}`);

    let ownersWithToken = 0;
    let ownersWithoutToken = 0;

    allGarageOwners.forEach((owner) => {
      if (owner.fcmToken) {
        ownersWithToken++;
        console.log(`   ✅ ${owner.email} - Has FCM token`);
      } else {
        ownersWithoutToken++;
        console.log(`   ❌ ${owner.email} - Missing FCM token`);
      }
    });

    console.log('\n---\n');

    // Check recent breakdowns
    const Breakdown = mongoose.model('Breakdown');
    const recentBreakdowns = await Breakdown.find()
      .sort({ createdAt: -1 })
      .limit(5);

    console.log(`📋 Recent SOS Requests (Last 5):`);
    if (recentBreakdowns.length === 0) {
      console.log('   No breakdowns found');
    } else {
      recentBreakdowns.forEach((breakdown) => {
        console.log(`   🚨 ${breakdown.type} - Status: ${breakdown.status}`);
        console.log(`      Location: ${breakdown.latitude}, ${breakdown.longitude}`);
        console.log(`      Created: ${breakdown.createdAt}`);
      });
    }

    console.log('\n---\n');

    // Check notifications
    const Notification = mongoose.model('Notification');
    const recentNotifications = await Notification.find({
      recipientId: '6932f6f96551fb27afecc516',
      type: 'NEW_BREAKDOWN'
    })
      .sort({ createdAt: -1 })
      .limit(5);

    console.log(`🔔 Recent Notifications for Garage Owner:`);
    if (recentNotifications.length === 0) {
      console.log('   ❌ No notifications found');
      console.log('   This confirms that notifications are NOT being sent!');
    } else {
      recentNotifications.forEach((notif) => {
        console.log(`   📬 ${notif.title}`);
        console.log(`      ${notif.message}`);
        console.log(`      Read: ${notif.read}`);
        console.log(`      Created: ${notif.createdAt}`);
      });
    }

    console.log('\n---\n');
    console.log('✅ Diagnostic Complete!\n');

    if (!garageOwner.fcmToken) {
      console.log('🚨 ACTION REQUIRED:');
      console.log('1. Garage owner needs to log in to Android app to register FCM token');
      console.log('2. After login, run this script again to verify token is saved');
    }

    if (recentNotifications.length === 0 && recentBreakdowns.length > 0) {
      console.log('🚨 CRITICAL ISSUE DETECTED:');
      console.log('- SOS requests exist in database');
      console.log('- But NO notifications were sent');
      console.log('- Backend notification logic is missing or broken');
      console.log('\n👉 See BACKEND_SOS_NOTIFICATION_IMPLEMENTATION.md for fix');
    }

  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    mongoose.connection.close();
  }
}

checkGarageOwnerSetup();

