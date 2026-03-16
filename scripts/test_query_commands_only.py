#!/usr/bin/env python3
"""
Test ONLY query commands (ending with ?) from X1200W_EU_Commands.csv
DOES NOT send any control commands that change settings!
"""

import csv
import socket
import time
import sys
from pathlib import Path


def test_query_command(host, command, port=23, timeout=1.0):
    """Test a single query command"""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.settimeout(timeout)
            sock.connect((host, port))

            # Small delay after connect
            time.sleep(0.1)

            # Send command
            sock.sendall(f"{command}\r".encode('ascii'))

            # Wait for response
            time.sleep(0.2)

            # Read response
            response = b''
            sock.settimeout(0.1)
            try:
                while True:
                    data = sock.recv(1024)
                    if not data:
                        break
                    response += data
            except socket.timeout:
                pass

            return response.decode('ascii', errors='ignore').strip()

    except Exception as e:
        return f"ERROR: {e}"


def parse_csv_queries_only(csv_path):
    """Parse CSV and return ONLY query commands (ending with ?)"""
    commands = []

    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)

        for row in reader:
            # Skip empty rows
            if not row or all(v is None or v == '' for v in row.values()):
                continue

            # Skip if not supported
            supported = row.get('Supported_X1200W_EU', '') or ''
            if supported.strip() != '✔':
                continue

            # Extract command from Example column
            example = (row.get('Example', '') or '').strip()
            if not example:
                continue

            # Remove <CR> to get the actual command
            command = example.replace('<CR>', '').strip()

            # **IMPORTANT**: ONLY query commands (ending with ?)
            if not command.endswith('?'):
                continue

            commands.append({
                'command': command,
                'function': (row.get('Function', '') or '').strip(),
                'prefix': (row.get('Command', '') or '').strip(),
            })

    return commands


def main():
    # Configuration
    avr_host = '192.168.178.130'
    csv_path = Path(__file__).parent.parent / 'X1200W_EU_Commands.csv'

    print("=" * 80)
    print("TESTING ONLY READ-ONLY QUERY COMMANDS (ending with ?)")
    print("NO CONTROL COMMANDS WILL BE SENT!")
    print("=" * 80)
    print()
    print(f"Testing Denon AVR at {avr_host}")
    print(f"Loading commands from {csv_path}")
    print()

    # Parse CSV - queries only
    commands = parse_csv_queries_only(csv_path)
    print(f"Found {len(commands)} query commands to test")
    print()

    # Test all query commands
    results = []
    success_count = 0

    for i, cmd_info in enumerate(commands, 1):
        print(f"[{i}/{len(commands)}] {cmd_info['command']:30s}", end=' ... ', flush=True)

        response = test_query_command(avr_host, cmd_info['command'])

        if response and not response.startswith('ERROR'):
            print(f"✓ {response[:50]}")
            success_count += 1
            results.append((cmd_info['command'], response, True))
        else:
            print(f"✗ {response[:50]}")
            results.append((cmd_info['command'], response, False))

        # Delay between commands
        time.sleep(0.2)

    # Summary
    print()
    print("=" * 80)
    print(f"SUMMARY: {success_count}/{len(commands)} query commands succeeded")
    print(f"Success rate: {100 * success_count / len(commands):.1f}%")
    print("=" * 80)

    # Save results
    results_file = Path('/tmp/query_test_results.txt')
    with open(results_file, 'w') as f:
        f.write("QUERY COMMAND TEST RESULTS\n")
        f.write("=" * 80 + "\n\n")
        for cmd, resp, success in results:
            status = "✓" if success else "✗"
            f.write(f"{status} {cmd:30s} -> {resp}\n")

    print(f"\nDetailed results saved to: {results_file}")

    return 0


if __name__ == '__main__':
    sys.exit(main())
