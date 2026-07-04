import { Injectable, ServiceUnavailableException, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { App, applicationDefault, cert, getApp, getApps, initializeApp } from 'firebase-admin/app';
import { Auth, DecodedIdToken, getAuth } from 'firebase-admin/auth';

@Injectable()
export class FirebaseIdentityService {
  private readonly auth: Auth | null;

  constructor(private readonly config: ConfigService) {
    this.auth = this.initializeAuth();
  }

  isConfigured() {
    return this.auth !== null;
  }

  async verifyIdToken(idToken: string): Promise<DecodedIdToken> {
    if (!this.auth) {
      throw new ServiceUnavailableException('Firebase Authentication no esta configurado');
    }
    if (!idToken?.trim()) {
      throw new UnauthorizedException('Token de Firebase requerido');
    }

    try {
      return await this.auth.verifyIdToken(idToken);
    } catch {
      throw new UnauthorizedException('Sesion de Google invalida o expirada');
    }
  }

  async syncPasswordUser(user: { id: string; email: string; name: string; firebaseUid?: string | null }, password: string) {
    if (!this.auth || !password) return null;

    try {
      const existing = await this.auth.getUserByEmail(user.email);
      await this.auth.updateUser(existing.uid, {
        password,
        displayName: user.name,
        disabled: false,
      });
      return existing.uid;
    } catch (error: any) {
      if (error?.code !== 'auth/user-not-found') return null;
    }

    try {
      const created = await this.auth.createUser({
        uid: user.firebaseUid ?? user.id,
        email: user.email,
        password,
        displayName: user.name,
        emailVerified: false,
      });
      return created.uid;
    } catch {
      return null;
    }
  }

  private initializeAuth(): Auth | null {
    try {
      const app = getApps().length > 0 ? getApp() : initializeApp(this.firebaseOptions());
      return getAuth(app);
    } catch {
      return null;
    }
  }

  private firebaseOptions() {
    const serviceAccountJson = this.config.get<string>('FIREBASE_SERVICE_ACCOUNT_JSON');
    if (serviceAccountJson) {
      const serviceAccount = JSON.parse(serviceAccountJson);
      return { credential: cert(serviceAccount), projectId: serviceAccount.project_id };
    }

    const projectId = this.config.get<string>('FIREBASE_PROJECT_ID');
    const clientEmail = this.config.get<string>('FIREBASE_CLIENT_EMAIL');
    const privateKey = this.config.get<string>('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n');

    if (projectId && clientEmail && privateKey) {
      return {
        credential: cert({ projectId, clientEmail, privateKey }),
        projectId,
      };
    }

    if (projectId) {
      return { credential: applicationDefault(), projectId };
    }

    throw new Error('Firebase credentials missing');
  }
}
