import * as admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';

export const initializeFirebase = () => {
  // Vérifier si Firebase est déjà initialisé
  if (admin.apps.length > 0) {
    return admin.app();
  }

  // Récupérer le chemin de la clé Firebase depuis les variables d'environnement
  const firebaseKeyPath = process.env.FIREBASE_KEY_PATH;

  if (!firebaseKeyPath) {
    console.debug(
      'ℹ️  FIREBASE_KEY_PATH non défini. Firebase Cloud Messaging ne sera pas disponible.',
    );
    return null;
  }

  // Charger la clé Firebase depuis le fichier
  let serviceAccount: any;

  try {
    // Essayer plusieurs chemins possibles
    const possiblePaths = [
      path.resolve(process.cwd(), firebaseKeyPath), // Relatif à la racine du projet
      path.resolve(__dirname, '../../..', firebaseKeyPath), // Relatif au dossier dist
      path.resolve(firebaseKeyPath), // Chemin absolu ou relatif au CWD
      path.join(process.cwd(), 'src', firebaseKeyPath), // Dans le dossier src
    ];

    let foundPath: string | null = null;
    
    for (const testPath of possiblePaths) {
      if (fs.existsSync(testPath)) {
        foundPath = testPath;
        break;
      }
    }

    if (foundPath) {
      const rawData = fs.readFileSync(foundPath, 'utf-8');
      serviceAccount = JSON.parse(rawData);
      console.log(`✅ Firebase service account key loaded from: ${foundPath}`);
    } else if (process.env.FIREBASE_KEY) {
      // Essayer de parser directement depuis les variables d'environnement (base64)
      serviceAccount = JSON.parse(
        Buffer.from(process.env.FIREBASE_KEY, 'base64').toString(),
      );
      console.log('✅ Firebase service account key loaded from FIREBASE_KEY environment variable');
    } else {
      const triedPaths = possiblePaths.join('\n  - ');
      throw new Error(
        `Firebase service account key not found. Tried paths:\n  - ${triedPaths}\n` +
        `Current working directory: ${process.cwd()}\n` +
        `FIREBASE_KEY_PATH: ${firebaseKeyPath}`
      );
    }

    const app = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    
    console.log('✅ Firebase Admin SDK initialized successfully');
    return app;
  } catch (error) {
    console.error('❌ Firebase initialization failed:', error.message);
    throw error;
  }
};

export const getFirebaseMessaging = () => {
  try {
    if (admin.apps.length === 0) {
      return null;
    }
    return admin.messaging();
  } catch (error) {
    return null;
  }
};
