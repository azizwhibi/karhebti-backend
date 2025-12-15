import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

@Injectable()
export class SupabaseService {
  private readonly logger = new Logger(SupabaseService.name);
  private supabase: SupabaseClient;
  private bucketName: string;

  constructor(private configService: ConfigService) {
    const supabaseUrl = this.configService.get<string>('SUPABASE_URL');
    const supabaseServiceRoleKey = this.configService.get<string>('SUPABASE_SERVICE_ROLE_KEY');
    this.bucketName = this.configService.get<string>('SUPABASE_BUCKET') || 'cars images';

    if (!supabaseUrl || !supabaseServiceRoleKey) {
      throw new Error('Supabase credentials are not configured properly');
    }

    // Use service role key for admin operations
    this.supabase = createClient(supabaseUrl, supabaseServiceRoleKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false,
      },
    });

    this.logger.log('Supabase client initialized successfully');
  }

  /**
   * Upload a file to Supabase Storage
   * @param buffer File buffer
   * @param path Path within the bucket (e.g., 'car-123-1234567890.webp')
   * @param contentType MIME type of the file
   * @returns Public URL of the uploaded file
   */
  async uploadFile(
    buffer: Buffer,
    path: string,
    contentType: string,
  ): Promise<string> {
    try {
      const { data, error } = await this.supabase.storage
        .from(this.bucketName)
        .upload(path, buffer, {
          contentType,
          upsert: true, // Replace if exists
          cacheControl: '3600', // Cache for 1 hour
        });

      if (error) {
        this.logger.error(`Failed to upload file to Supabase: ${error.message}`, error);
        throw new BadRequestException(`File upload failed: ${error.message}`);
      }

      // Get public URL
      const { data: urlData } = this.supabase.storage
        .from(this.bucketName)
        .getPublicUrl(path);

      if (!urlData?.publicUrl) {
        throw new BadRequestException('Failed to generate public URL');
      }

      this.logger.log(`File uploaded successfully: ${path}`);
      return urlData.publicUrl;
    } catch (error) {
      this.logger.error('Error uploading file to Supabase', error);
      if (error instanceof BadRequestException) {
        throw error;
      }
      throw new BadRequestException('Failed to upload file to storage');
    }
  }

  /**
   * Delete a file from Supabase Storage
   * @param path Path within the bucket or full URL
   */
  async deleteFile(path: string): Promise<void> {
    if (!path) return;

    try {
      // Extract path from URL if a full URL is provided
      let filePath = path;
      if (path.includes('supabase.co')) {
        const url = new URL(path);
        const pathParts = url.pathname.split('/');
        // Format: /storage/v1/object/public/bucket-name/file-path
        const bucketIndex = pathParts.indexOf(this.bucketName);
        if (bucketIndex !== -1) {
          filePath = pathParts.slice(bucketIndex + 1).join('/');
        }
      }

      const { error } = await this.supabase.storage
        .from(this.bucketName)
        .remove([filePath]);

      if (error) {
        this.logger.warn(`Failed to delete file from Supabase: ${error.message}`);
        // Don't throw error for missing files
        return;
      }

      this.logger.log(`File deleted successfully: ${filePath}`);
    } catch (error) {
      this.logger.warn('Error deleting file from Supabase', error);
      // Don't throw error to avoid breaking the flow
    }
  }

  /**
   * Get public URL for a file
   * @param path Path within the bucket
   * @returns Public URL
   */
  getPublicUrl(path: string): string {
    const { data } = this.supabase.storage
      .from(this.bucketName)
      .getPublicUrl(path);

    return data.publicUrl;
  }

  /**
   * List files in a specific folder
   * @param folder Folder path (optional)
   * @returns List of files
   */
  async listFiles(folder?: string): Promise<any[]> {
    try {
      const { data, error } = await this.supabase.storage
        .from(this.bucketName)
        .list(folder, {
          limit: 100,
          offset: 0,
          sortBy: { column: 'created_at', order: 'desc' },
        });

      if (error) {
        this.logger.error(`Failed to list files: ${error.message}`);
        return [];
      }

      return data || [];
    } catch (error) {
      this.logger.error('Error listing files from Supabase', error);
      return [];
    }
  }

  /**
   * Check if bucket exists and is accessible
   */
  async checkBucketAccess(): Promise<boolean> {
    try {
      const { data, error } = await this.supabase.storage
        .from(this.bucketName)
        .list('', { limit: 1 });

      if (error) {
        this.logger.error(`Bucket access check failed: ${error.message}`);
        return false;
      }

      return true;
    } catch (error) {
      this.logger.error('Error checking bucket access', error);
      return false;
    }
  }
}
