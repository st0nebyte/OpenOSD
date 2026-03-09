#!/usr/bin/env python3
"""
Generate OpenOSD app icon - modern pictogram style
Shows a display/TV with volume bars overlay
"""

from PIL import Image, ImageDraw
import os

def create_icon(size):
    """Create OpenOSD icon at specified size"""
    # Create image with transparency
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Calculate dimensions with padding
    padding = size * 0.15
    inner_size = size - (2 * padding)

    # Colors - modern blue-white theme matching the OSD
    bg_color = (45, 52, 64, 255)        # Dark blue-grey background
    accent_color = (136, 192, 208, 255) # Soft blue-white accent
    border_color = (200, 210, 220, 255) # Light border

    # Draw display/TV outline (rounded rectangle)
    display_rect = [padding, padding, size - padding, size - padding]
    corner_radius = size * 0.12

    # Draw display background
    draw.rounded_rectangle(display_rect, corner_radius, fill=bg_color)

    # Draw display border
    border_width = max(2, int(size * 0.04))
    draw.rounded_rectangle(display_rect, corner_radius, outline=border_color, width=border_width)

    # Draw volume bars in center (3 bars of increasing height)
    bar_width = max(4, int(size * 0.08))
    bar_center_y = size * 0.5

    # Three bars: short, medium, tall
    bars = [
        (0.25, 0.35),  # Height multiplier, x position multiplier
        (0.45, 0.5),
        (0.65, 0.65),
    ]

    for height_mult, x_mult in bars:
        bar_height = max(8, int(inner_size * height_mult))
        bar_x = int(padding + (inner_size * x_mult) - (bar_width / 2))
        bar_y = int(bar_center_y - (bar_height / 2))

        # Ensure valid coordinates
        if bar_x + bar_width > bar_x and bar_y + bar_height > bar_y:
            radius = max(1, int(bar_width * 0.25))
            draw.rounded_rectangle(
                [bar_x, bar_y, bar_x + bar_width, bar_y + bar_height],
                radius=radius,
                fill=accent_color
            )

    # Add small "speaker" indicators in corners (optional, only for larger sizes)
    if size >= 96:
        speaker_radius = size * 0.04
        speaker_offset = padding + speaker_radius + (size * 0.05)

        # Bottom left and right speakers
        for x in [speaker_offset, size - speaker_offset]:
            y = size - speaker_offset
            draw.ellipse(
                [x - speaker_radius, y - speaker_radius,
                 x + speaker_radius, y + speaker_radius],
                fill=accent_color
            )

    return img

def main():
    """Generate all icon sizes for Android"""
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192,
    }

    base_path = 'app/src/main/res'

    for density, size in sizes.items():
        # Create directory
        dir_path = f'{base_path}/mipmap-{density}'
        os.makedirs(dir_path, exist_ok=True)

        # Generate and save icon
        icon = create_icon(size)
        icon_path = f'{dir_path}/ic_launcher.png'
        icon.save(icon_path, 'PNG')
        print(f'✓ Created {density}: {size}x{size}px -> {icon_path}')

    # Also create a high-res version for preview
    preview = create_icon(512)
    preview.save('app_icon_preview.png', 'PNG')
    print(f'✓ Created preview: 512x512px -> app_icon_preview.png')

if __name__ == '__main__':
    main()
