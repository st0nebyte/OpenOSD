#!/usr/bin/env python3
"""
Generate docs/index.html from docs/index.template.html by extracting
values from Kotlin source files (OSDView.kt, AVRState.kt).

This ensures the HTML demo always matches the APK implementation.
"""

import re
import sys
from pathlib import Path


def extract_trigger_config(avr_state_path):
    """Extract OSDTrigger enum values from AVRState.kt"""
    content = avr_state_path.read_text()

    # Find the OSDTrigger enum definition
    trigger_pattern = r'(\w+)\s+\(timeoutMs\s*=\s*(\d+(?:_\d+)*),\s*showVolume\s*=\s*(\w+),\s*showInfo\s*=\s*(\w+)(?:,\s*persistWhileMuted\s*=\s*(\w+))?\)'

    triggers = {}
    for match in re.finditer(trigger_pattern, content):
        name = match.group(1)
        timeout = match.group(2).replace('_', '')  # Remove underscores from numbers
        show_volume = match.group(3) == 'true'
        show_info = match.group(4) == 'true'
        persist = match.group(5) == 'true' if match.group(5) else False

        triggers[name] = {
            'timeoutMs': int(timeout),
            'showVolume': show_volume,
            'showInfo': show_info,
            'persistWhileMuted': persist
        }

    return triggers


def extract_colors(osd_view_path):
    """Extract color constants from OSDView.kt"""
    content = osd_view_path.read_text()

    # Find color definitions like: private val BG = Color.parseColor("#B31E1E23")
    color_pattern = r'private\s+val\s+(\w+)\s*=\s*Color\.parseColor\("(#[0-9A-Fa-f]+)"\)'

    colors = {}
    for match in re.finditer(color_pattern, content):
        name = match.group(1)
        value = match.group(2)
        colors[name] = hex_to_rgba(value)

    return colors


def hex_to_rgba(hex_color):
    """Convert Android hex color (#AARRGGBB or #RRGGBB) to rgba() string"""
    hex_color = hex_color.lstrip('#')

    if len(hex_color) == 8:
        # #AARRGGBB format
        a = int(hex_color[0:2], 16) / 255
        r = int(hex_color[2:4], 16)
        g = int(hex_color[4:6], 16)
        b = int(hex_color[6:8], 16)
    else:
        # #RRGGBB format
        r = int(hex_color[0:2], 16)
        g = int(hex_color[2:4], 16)
        b = int(hex_color[4:6], 16)
        a = 1.0

    return f'rgba({r}, {g}, {b}, {a:.2f})'


def generate_trigger_js(triggers):
    """Generate JavaScript TRIGGERS constant"""
    lines = ['const TRIGGERS = {']
    for name, config in triggers.items():
        show_vol = 'true' if config['showVolume'] else 'false'
        show_inf = 'true' if config['showInfo'] else 'false'
        persist = 'true' if config['persistWhileMuted'] else 'false'
        lines.append(f"    {name:12s}: {{ timeoutMs: {config['timeoutMs']:4d}, showVolume: {show_vol:5s}, showInfo: {show_inf:5s}, persistWhileMuted: {persist:5s} }},")
    lines.append('};')
    return '\n        '.join(lines)


def generate_colors_js(colors):
    """Generate JavaScript COLORS constant"""
    lines = ['const COLORS = {']
    for name, value in colors.items():
        # Pad name for alignment
        lines.append(f"    {name:13s}: '{value}',")
    lines.append('};')
    return '\n        '.join(lines)


def main():
    # Paths
    project_root = Path(__file__).parent.parent
    avr_state_path = project_root / 'app/src/main/java/dev/st0nebyte/openosd/AVRState.kt'
    osd_view_path = project_root / 'app/src/main/java/dev/st0nebyte/openosd/OSDView.kt'
    template_path = project_root / 'docs/index.template.html'
    output_path = project_root / 'docs/index.html'

    # Check files exist
    if not avr_state_path.exists():
        print(f'Error: {avr_state_path} not found', file=sys.stderr)
        sys.exit(1)
    if not osd_view_path.exists():
        print(f'Error: {osd_view_path} not found', file=sys.stderr)
        sys.exit(1)
    if not template_path.exists():
        print(f'Error: {template_path} not found', file=sys.stderr)
        sys.exit(1)

    # Extract values from Kotlin files
    print('Extracting trigger config from AVRState.kt...')
    triggers = extract_trigger_config(avr_state_path)
    print(f'  Found {len(triggers)} triggers: {", ".join(triggers.keys())}')

    print('Extracting colors from OSDView.kt...')
    colors = extract_colors(osd_view_path)
    print(f'  Found {len(colors)} colors')

    # Generate JavaScript code
    triggers_js = generate_trigger_js(triggers)
    colors_js = generate_colors_js(colors)

    # Read template
    print(f'Reading template from {template_path}...')
    template = template_path.read_text()

    # Replace placeholders
    output = template.replace('{{TRIGGERS_JS}}', triggers_js)
    output = output.replace('{{COLORS_JS}}', colors_js)

    # Write output
    print(f'Writing generated HTML to {output_path}...')
    output_path.write_text(output)
    print('✓ docs/index.html generated successfully!')
    print()
    print('Generated values:')
    print(f'  Triggers: {", ".join(triggers.keys())}')
    print(f'  Colors: {", ".join(list(colors.keys())[:5])}, ...')


if __name__ == '__main__':
    main()
