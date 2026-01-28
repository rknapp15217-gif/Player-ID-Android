#!/usr/bin/env python3
"""
🚀 Enhanced Jersey Number Detection Training Pipeline
Automated training script for custom jersey number detection model
"""

import argparse
import os
import sys
import yaml
from pathlib import Path
import torch
from ultralytics import YOLO
import logging

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def setup_training_environment():
    """🔧 Setup training environment and dependencies"""
    try:
        import ultralytics
        import torch
        import torchvision
        logger.info("✅ All dependencies available")
        return True
    except ImportError as e:
        logger.error(f"❌ Missing dependency: {e}")
        logger.info("Installing required packages...")
        
        # Auto-install required packages
        import subprocess
        packages = [
            "ultralytics",
            "torch",
            "torchvision", 
            "opencv-python",
            "Pillow",
            "numpy",
            "matplotlib"
        ]
        
        for package in packages:
            subprocess.check_call([sys.executable, "-m", "pip", "install", package])
        
        logger.info("✅ Dependencies installed successfully")
        return True

def validate_dataset(data_path):
    """📊 Validate dataset structure and contents"""
    logger.info(f"🔍 Validating dataset at {data_path}")
    
    if not os.path.exists(data_path):
        logger.error(f"❌ Dataset file not found: {data_path}")
        return False
    
    with open(data_path, 'r') as f:
        config = yaml.safe_load(f)
    
    # Check required fields
    required_fields = ['train', 'val', 'nc', 'names']
    for field in required_fields:
        if field not in config:
            logger.error(f"❌ Missing required field in dataset.yaml: {field}")
            return False
    
    # Validate directories
    base_dir = Path(data_path).parent
    train_dir = base_dir / config['train']
    labels_dir = base_dir / 'labels'
    
    if not train_dir.exists():
        logger.error(f"❌ Training images directory not found: {train_dir}")
        return False
        
    if not labels_dir.exists():
        logger.error(f"❌ Labels directory not found: {labels_dir}")
        return False
    
    # Count samples
    image_files = list(train_dir.glob('*.jpg')) + list(train_dir.glob('*.png'))
    label_files = list(labels_dir.glob('*.txt'))
    
    logger.info(f"📊 Found {len(image_files)} images and {len(label_files)} labels")
    
    if len(image_files) == 0:
        logger.error("❌ No training images found")
        return False
    
    logger.info("✅ Dataset validation passed")
    return True

def train_jersey_detector(args):
    """🎯 Main training function"""
    logger.info("🚀 Starting jersey number detection training")
    
    # Validate dataset
    if not validate_dataset(args.data):
        return False
    
    try:
        # Initialize YOLO model
        if args.pretrained:
            logger.info(f"📥 Loading pretrained model: {args.pretrained}")
            model = YOLO(args.pretrained)
        else:
            logger.info("🆕 Creating new YOLOv8 model")
            model = YOLO('yolov8n.pt')  # Start with nano model for speed
        
        # Training parameters
        training_args = {
            'data': args.data,
            'epochs': args.epochs,
            'imgsz': args.img,
            'batch': args.batch,
            'device': args.device,
            'project': args.project,
            'name': args.name,
            'save': True,
            'save_period': 10,  # Save every 10 epochs
            'cache': True,      # Cache images for faster training
            'augment': True,    # Use data augmentation
            'mosaic': 1.0,     # Mosaic augmentation probability
            'mixup': 0.1,      # Mixup augmentation probability
            'copy_paste': 0.1, # Copy-paste augmentation probability
            'degrees': 15.0,   # Rotation degrees
            'translate': 0.1,  # Translation fraction
            'scale': 0.9,      # Scale fraction
            'shear': 2.0,      # Shear degrees
            'perspective': 0.0001, # Perspective transform
            'flipud': 0.0,     # Vertical flip probability
            'fliplr': 0.5,     # Horizontal flip probability
            'bgr': 0.0,        # BGR channels probability
            'hsv_h': 0.015,    # HSV-Hue augmentation
            'hsv_s': 0.7,      # HSV-Saturation augmentation
            'hsv_v': 0.4,      # HSV-Value augmentation
            'cls': 0.5,        # Classification loss weight
            'box': 7.5,        # Box loss weight
            'dfl': 1.5,        # DFL loss weight
            'pose': 12.0,      # Pose loss weight (unused)
            'kobj': 1.0,       # Keypoint obj loss weight (unused)
            'label_smoothing': 0.0,  # Label smoothing epsilon
            'nbs': 64,         # Nominal batch size
            'overlap_mask': True,    # Masks should overlap during training
            'mask_ratio': 4,   # Mask downsample ratio
            'dropout': 0.0,    # Dropout probability
            'val': True,       # Validate/test during training
            'plots': True,     # Generate training plots
            'verbose': True,   # Verbose output
        }
        
        logger.info("🎯 Starting training with optimized parameters...")
        results = model.train(**training_args)
        
        # Export trained model to different formats
        logger.info("📤 Exporting trained model...")
        
        # Export to TensorFlow Lite for mobile deployment
        model.export(format='tflite', imgsz=args.img)
        logger.info("✅ TensorFlow Lite model exported")
        
        # Export to ONNX for flexibility
        model.export(format='onnx', imgsz=args.img)
        logger.info("✅ ONNX model exported")
        
        # Validate trained model
        logger.info("🧪 Validating trained model...")
        val_results = model.val()
        
        # Print training summary
        logger.info("🎉 Training completed successfully!")
        logger.info(f"📊 mAP50: {val_results.box.map50:.3f}")
        logger.info(f"📊 mAP50-95: {val_results.box.map:.3f}")
        
        return True
        
    except Exception as e:
        logger.error(f"💥 Training failed: {str(e)}")
        return False

def main():
    parser = argparse.ArgumentParser(description='🚀 Jersey Number Detection Training')
    parser.add_argument('--data', type=str, required=True, help='Path to dataset.yaml')
    parser.add_argument('--img', type=int, default=640, help='Image size')
    parser.add_argument('--batch', type=int, default=16, help='Batch size')
    parser.add_argument('--epochs', type=int, default=100, help='Number of epochs')
    parser.add_argument('--device', type=str, default='cpu', help='Training device')
    parser.add_argument('--project', type=str, default='runs/train', help='Project directory')
    parser.add_argument('--name', type=str, default='jersey_detector', help='Experiment name')
    parser.add_argument('--pretrained', type=str, help='Path to pretrained model')
    
    args = parser.parse_args()
    
    # Setup environment
    if not setup_training_environment():
        logger.error("❌ Failed to setup training environment")
        sys.exit(1)
    
    # Run training
    success = train_jersey_detector(args)
    
    if success:
        logger.info("🎉 Jersey detector training completed successfully!")
        sys.exit(0)
    else:
        logger.error("❌ Training failed")
        sys.exit(1)

if __name__ == '__main__':
    main()