import { Injectable, BadRequestException } from '@nestjs/common';
import sharp from 'sharp';
import { SupabaseService } from './supabase.service';

export interface ImageMetadata {
  width: number;
  height: number;
  format: string;
  size: number;
}

export interface ProcessedImage {
  filename: string;
  path: string;
  url: string;
  metadata: ImageMetadata;
}

@Injectable()
export class UploadService {
  private readonly maxWidth = 1600;
  private readonly quality = 80;
  private readonly allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly maxFileSize = 5 * 1024 * 1024; // 5MB

  constructor(private readonly supabaseService: SupabaseService) {}

  validateFile(file: Express.Multer.File): void {
    if (!file) {
      throw new BadRequestException('No file provided');
    }

    if (!this.allowedMimeTypes.includes(file.mimetype)) {
      throw new BadRequestException(
        `Invalid file type. Allowed types: ${this.allowedMimeTypes.join(', ')}`,
      );
    }

    if (file.size > this.maxFileSize) {
      throw new BadRequestException(
        `File too large. Maximum size: ${this.maxFileSize / 1024 / 1024}MB`,
      );
    }
  }

  async processCarImage(
    file: Express.Multer.File,
    carId: string,
  ): Promise<ProcessedImage> {
    this.validateFile(file);

    const timestamp = Date.now();
    const filename = `car-${carId}-${timestamp}.webp`;

    try {
      // Process image with Sharp
      const processedBuffer = await sharp(file.buffer)
        .resize(this.maxWidth, null, {
          fit: 'inside',
          withoutEnlargement: true,
        })
        .webp({ quality: this.quality })
        .toBuffer();

      // Get metadata
      const metadata = await sharp(processedBuffer).metadata();

      // Upload to Supabase Storage
      const publicUrl = await this.supabaseService.uploadFile(
        processedBuffer,
        filename,
        'image/webp',
      );

      return {
        filename,
        path: filename, // Store just the filename for future reference
        url: publicUrl, // Full Supabase public URL
        metadata: {
          width: metadata.width || 0,
          height: metadata.height || 0,
          format: 'webp',
          size: processedBuffer.length,
        },
      };
    } catch (error) {
      console.error('Image processing error:', error);
      throw new BadRequestException('Failed to process image');
    }
  }

  async deleteCarImage(imageUrl: string): Promise<void> {
    if (!imageUrl) return;

    try {
      await this.supabaseService.deleteFile(imageUrl);
    } catch (error) {
      console.warn('Failed to delete image:', error.message);
      // Don't throw error if deletion fails
    }
  }
}
