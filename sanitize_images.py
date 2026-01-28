import os
from PIL import Image

# --- HOW TO USE ---
# 1. Make sure you have the Pillow library installed. From your command prompt or terminal, run:
#    pip install Pillow
#
# 2. Set the two folder paths below.
#
# 3. Run this script from your terminal:
#    python sanitize_images.py
# ------------------

# --- Configuration ---
# Set this to the folder containing your 80+ new images that need cleaning.
# Example: '''C:/Users/Ryan/Downloads/hard_examples_to_clean'''
input_folder = "C:/ProgramData/ASUS/ASUS Live Update/Temp/images_to_clean"

# Set this to a new, empty folder where the cleaned images will be saved.
# Example: '''C:/Users/Ryan/Downloads/cleaned_images'''
output_folder = "C:/ProgramData/ASUS/ASUS Live Update/Temp/cleaned_images"
# --- End Configuration ---


def sanitize_images():
    """
    Opens each image from the input folder and re-saves it as a clean JPEG,
    stripping all non-standard metadata that can cause import errors.
    """
    if not os.path.exists(input_folder) or "PATH_TO_YOUR_INPUT_IMAGES" in input_folder:
        print("Error: Please set the 'input_folder' variable in this script.")
        return

    if not os.path.exists(output_folder):
        print(f"Creating output folder: {output_folder}")
        os.makedirs(output_folder)

    print(f"Starting image sanitization...")
    print(f"Input folder: {input_folder}")
    print(f"Output folder: {output_folder}")

    sanitized_count = 0
    failed_count = 0
    for filename in os.listdir(input_folder):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif', '.mpo')):
            try:
                input_path = os.path.join(input_folder, filename)
                
                # Create a new filename with a .jpg extension
                sanitized_filename = os.path.splitext(filename)[0] + '.jpg'
                output_path = os.path.join(output_folder, sanitized_filename)

                with Image.open(input_path) as img:
                    # Convert to RGB to handle formats like RGBA (PNG) or P (Palette)
                    # and save as a standard JPEG. This strips problematic metadata.
                    img.convert('RGB').save(output_path, "JPEG", quality=95)
                
                sanitized_count += 1
                print(f"  - Sanitized: {filename} -> {sanitized_filename}")

            except Exception as e:
                failed_count += 1
                print(f"  - FAILED to process {filename}. Error: {e}")

    print(f"\nSanitization complete.")
    print(f"Successfully processed: {sanitized_count}")
    print(f"Failed: {failed_count}")


if __name__ == "__main__":
    sanitize_images()
