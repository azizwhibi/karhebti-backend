import { Injectable, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as admin from 'firebase-admin';

@Injectable()
export class FirebaseService implements OnModuleInit {
  private firebaseApp: admin.app.App;

  constructor(private configService: ConfigService) {}

  onModuleInit() {
    this.initializeFirebase();
  }

  private initializeFirebase() {
    try {
      // Check if Firebase is already initialized
      if (admin.apps.length > 0) {
        this.firebaseApp = admin.app();
        console.log('✅ Firebase Admin SDK already initialized');
        return;
      }

      // Build service account from environment variables
      const serviceAccount = {
        type: this.configService.get<string>('FIREBASE_TYPE'),
        project_id: this.configService.get<string>('FIREBASE_PROJECT_ID'),
        private_key_id: this.configService.get<string>('FIREBASE_PRIVATE_KEY_ID'),
        private_key: this.configService.get<string>('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n'),
        client_email: this.configService.get<string>('FIREBASE_CLIENT_EMAIL'),
        client_id: this.configService.get<string>('FIREBASE_CLIENT_ID'),
        auth_uri: this.configService.get<string>('FIREBASE_AUTH_URI'),
        token_uri: this.configService.get<string>('FIREBASE_TOKEN_URI'),
        auth_provider_x509_cert_url: this.configService.get<string>('FIREBASE_AUTH_PROVIDER_CERT_URL'),
        client_x509_cert_url: this.configService.get<string>('FIREBASE_CLIENT_CERT_URL'),
        universe_domain: this.configService.get<string>('FIREBASE_UNIVERSE_DOMAIN'),
      };

      // Validate required fields
      if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
        throw new Error('Missing required Firebase configuration in environment variables');
      }

      this.firebaseApp = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount as admin.ServiceAccount),
        projectId: serviceAccount.project_id,
      });

      console.log('✅ Firebase Admin SDK initialized successfully');
    } catch (error) {
      console.error('❌ Firebase initialization failed:', error.message);
      throw error;
    }
  }

  getAuth() {
    return admin.auth();
  }

  getFirestore() {
    return admin.firestore();
  }

  getStorage() {
    return admin.storage();
  }

  getDatabase() {
    return admin.database();
  }

  /**
   * Vérifier un token Firebase
   */
  async verifyToken(token: string) {
    try {
      const decodedToken = await this.getAuth().verifyIdToken(token);
      return decodedToken;
    } catch (error) {
      throw new Error(`Token verification failed: ${error.message}`);
    }
  }

  /**
   * Créer un utilisateur Firebase
   */
  async createUser(email: string, password: string, displayName?: string) {
    try {
      const userRecord = await this.getAuth().createUser({
        email,
        password,
        displayName,
      });
      return userRecord;
    } catch (error) {
      throw new Error(`Failed to create Firebase user: ${error.message}`);
    }
  }

  /**
   * Supprimer un utilisateur Firebase
   */
  async deleteUser(uid: string) {
    try {
      await this.getAuth().deleteUser(uid);
      return { success: true, message: 'User deleted successfully' };
    } catch (error) {
      throw new Error(`Failed to delete Firebase user: ${error.message}`);
    }
  }

  /**
   * Envoyer un email de réinitialisation de mot de passe
   */
  async sendPasswordResetEmail(email: string) {
    try {
      const resetLink = await this.getAuth().generatePasswordResetLink(email);
      return resetLink;
    } catch (error) {
      throw new Error(`Failed to generate password reset link: ${error.message}`);
    }
  }

  /**
   * Obtenir les informations d'un utilisateur
   */
  async getUserByEmail(email: string) {
    try {
      const userRecord = await this.getAuth().getUserByEmail(email);
      return userRecord;
    } catch (error) {
      throw new Error(`Failed to get user by email: ${error.message}`);
    }
  }
}
